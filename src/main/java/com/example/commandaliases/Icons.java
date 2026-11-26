package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Pictures for profiles. Most of them are Minecraft's own textures (read from the game itself,
 * nothing is copied into the mod). The command block is drawn from a 16x16 pixel map because
 * its game texture is animated.
 */
final class Icons {
    static final String[] IDS = {
            "grass_block", "bedrock", "crafting_table", "command_block", "diamond",
            "redstone", "emerald", "gold_ingot", "tnt", "diamond_ore"
    };

    /** id -> texture path inside the game's resources. */
    private static final Map<String, String> TEXTURES = new HashMap<>();
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    static {
        TEXTURES.put("grass_block", "textures/block/grass_block_side.png");
        TEXTURES.put("bedrock", "textures/block/bedrock.png");
        TEXTURES.put("crafting_table", "textures/block/crafting_table_front.png");
        TEXTURES.put("diamond", "textures/item/diamond.png");
        TEXTURES.put("redstone", "textures/item/redstone.png");
        TEXTURES.put("emerald", "textures/item/emerald.png");
        TEXTURES.put("gold_ingot", "textures/item/gold_ingot.png");
        TEXTURES.put("tnt", "textures/block/tnt_side.png");
        TEXTURES.put("diamond_ore", "textures/block/diamond_ore.png");
    }

    // Command block, 16x16. K = dark edge, D = dark brown, W = white slash, L = light tan, O/o = orange.
    private static final String[] COMMAND_BLOCK = {
            "BBBBBBBBBBBBBBBB",
            "BOOOOoOOOOOoOOOB",
            "BOLLOOOOOOOOLLOB",
            "BOLOOOOOOOOOOLOB",
            "BOOOKKKKKKKKOOOB",
            "BOOOKDDDDDWKOOOB",
            "BOOOKDDDDWDKOOOB",
            "BOOOKDDDWDDKOOOB",
            "BOOOKDDWDDDKOOOB",
            "BOOOKDWDDDDKOOOB",
            "BOOOKWDDDDDKOOOB",
            "BOOOKKKKKKKKOOOB",
            "BOOoOOOOOOOOoOOB",
            "BOLOOOOOOOOOOLOB",
            "BOLLOOOOOOOOLLOB",
            "BBBBBBBBBBBBBBBB"};
    private static final Map<Character, Integer> COMMAND_PALETTE = new HashMap<>();

    static {
        COMMAND_PALETTE.put('B', 0xFF3A2410);
        COMMAND_PALETTE.put('O', 0xFFC8782A);
        COMMAND_PALETTE.put('o', 0xFFA85E1C);
        COMMAND_PALETTE.put('L', 0xFFE8B070);
        COMMAND_PALETTE.put('K', 0xFF2A1808);
        COMMAND_PALETTE.put('D', 0xFF5A3418);
        COMMAND_PALETTE.put('W', 0xFFFFF0D0);
    }

    private Icons() {
    }

    /** Draws a picture {@code size} pixels wide and tall (16, 32, ... look sharpest). */
    static void draw(GuiGraphicsExtractor g, String id, int x, int y, int size) {
        if ("command_block".equals(id)) {
            drawCommandBlock(g, x, y, size);
            return;
        }
        String path = TEXTURES.get(id);
        if (path == null) {
            path = TEXTURES.get(IDS[0]); // unknown picture: show the grass block
        }
        Identifier texture = CACHE.computeIfAbsent(path, Identifier::withDefaultNamespace);
        // size x size on screen, taken from the whole 16x16 texture
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, 16, 16, 16, 16);
    }

    private static void drawCommandBlock(GuiGraphicsExtractor g, int x, int y, int size) {
        for (int r = 0; r < 16; r++) {
            String row = COMMAND_BLOCK[r];
            for (int c = 0; c < 16; c++) {
                Integer color = COMMAND_PALETTE.get(row.charAt(c));
                if (color == null) {
                    continue;
                }
                g.fill(x + c * size / 16, y + r * size / 16,
                        x + (c + 1) * size / 16, y + (r + 1) * size / 16, color);
            }
        }
    }
}
