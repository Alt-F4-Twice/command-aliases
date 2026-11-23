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
        AliasManager.init();

        // Shows up in Options > Controls > Key Binds under "Command Aliases".
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.commandaliases.open", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                openRequested = true;
            }
            // Wait until no screen is open (e.g. the chat box has closed after typing /aliases).
            if (openRequested && Ui.current() == null) {
                openRequested = false;
                Ui.open(new AliasListScreen(null));
            }
            AliasManager.tick(mc);
        });

        // Typing /aliases opens the menu and is not sent to the server.
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (AliasManager.bypass) {
                return true;
            }
            if (command.strip().equalsIgnoreCase("aliases")) {
                openRequested = true;
                return false;
            }
            return true;
        });

        // Swap an alias for its real command before it is sent.
        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            if (AliasManager.bypass) {
                return command;
            }
            List<String> expanded = AliasManager.expand(command);
            if (expanded == null) {
                return command;
            }
            if (expanded.size() > 1) {
                AliasManager.queue(List.copyOf(expanded.subList(1, expanded.size())));
            }
            return expanded.get(0);
        });
    }
}
