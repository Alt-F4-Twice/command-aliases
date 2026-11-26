package com.example.commandaliases;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.List;

public class CommandAliasesClient implements ClientModInitializer {
    public static final String MOD_ID = "commandaliases";

    private static KeyMapping openKey;
    private static boolean openRequested = false;

    @Override
    public void onInitializeClient() {
        ProfileManager.init();

        // Shows up in Options > Controls > Key Binds under "Command Aliases".
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.commandaliases.open", InputConstants.KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                openRequested = true;
            }
            // Wait until no screen is open (e.g. the chat box has closed after typing /aliases).
            if (openRequested && Ui.current() == null) {
                openRequested = false;
                Ui.open(MenuState.create());
            }
            Automations.tick(mc);
            HotkeyManager.tick(mc);
            Runner.tick(mc);
        });

        // /s style quick commands (and /aliases). The typed command is cancelled and the real
        // one is sent for you instead.
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (Runner.bypass) {
                return true;
            }
            String c = command.strip();
            if (c.equalsIgnoreCase("aliases")) {
                openRequested = true;
                return false;
            }
            List<Action> actions = AliasManager.expand("/" + c);
            if (actions == null) {
                return true;
            }
            Runner.queue(actions);
            return false;
        });

        // !s and ?s style quick commands: typed in chat, so we cancel the chat message
        // and run the real command instead. Chat that isn't one of your quick commands is untouched.
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (Runner.bypass) {
                return true;
            }
            String t = message.strip();
            if (t.isEmpty() || (t.charAt(0) != '!' && t.charAt(0) != '?')) {
                return true;
            }
            List<Action> actions = AliasManager.expand(t);
            if (actions == null) {
                return true;
            }
            Runner.queue(actions);
            return false;
        });
    }
}
