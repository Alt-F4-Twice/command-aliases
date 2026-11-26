package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.HashMap;
import java.util.Map;

/** Little 8x8 pixel pictures for profiles, drawn with plain colored squares (no image files needed). */
final class Icons {
    static final String[] IDS = {
            "grass_block", "bedrock", "crafting_table", "command_block", "diamond",
            "redstone", "emerald", "gold_ingot", "tnt", "chest"
    };

    private record Icon(String[] rows, Map<Character, Integer> palette) {
    }

    private static final Map<String, Icon> ICONS = new HashMap<>();

    private static void add(String id, String[] rows, String... palette) {
        Map<Character, Integer> map = new HashMap<>();
        for (String p : palette) {
            map.put(p.charAt(0), 0xFF000000 | Integer.parseInt(p.substring(1), 16));
        }
        ICONS.put(id, new Icon(rows, map));
    }

    static {
        add("grass_block", new String[]{
                "GGGGGGGG",
                "GgGGGgGG",
                "GGgGGGgG",
                "DGDDGDDG",
                "DDdDDDdD",
                "DDDDdDDD",
                "dDDDDDdD",
                "DDdDDDDD"},
                "G5DA130", "g4A8A25", "D8B5A2B", "d6E4421");

        add("bedrock", new String[]{
                "abcbadba",
                "cdabcabd",
                "bacdbaac",
                "dbacabcb",
                "acbdbcda",
                "bdcabdab",
                "cabdcbad",
                "bacbadcb"},
                "a1E1E1E", "b3A3A3A", "c5A5A5A", "d777777");

        add("crafting_table", new String[]{
                "DDDDDDDD",
                "DPPDDPPD",
                "DPTDDPRD",
                "DDDDDDDD",
                "DppppppD",
                "DpDppDpD",
                "DpDppDpD",
                "DDDDDDDD"},
                "D5B3E1E", "PB98B4F", "p9C7238", "T9A9A9A", "RB04030");

        add("command_block", new String[]{
                "BBBBBBBB",
                "BOOOOOOB",
                "BOOOOWOB",
                "BOOOWOOB",
                "BOOWOOoB",
                "BOWOOOoB",
                "BOOOOooB",
                "BBBBBBBB"},
                "B4A2A10", "OC8782A", "oA85E1C", "WFFF0D0");

        add("diamond", new String[]{
                "..kkkk..",
                ".kwCCCk.",
                "kwCCCCCk",
                "kCCCCCck",
                ".kCCCck.",
                "..kCck..",
                "...kck..",
                "....k..."},
                "k135E58", "C4AEDD9", "c2CB8A8", "wC8FFF6");

        add("redstone", new String[]{
                "........",
                "..r.R...",
                ".rRRhR..",
                ".RhRRRr.",
                "rRRRhRRr",
                ".rRRRRr.",
                "..rRRr..",
                "...rr..."},
                "rA01010", "RE02020", "hFF6060");

        add("emerald", new String[]{
                "..kkkk..",
                ".kwEEEk.",
                "kwEEEEEk",
                "kEEEEEek",
                ".kEEEek.",
                "..kEek..",
                "...kek..",
                "....k..."},
                "k0B4A1A", "E3FD35C", "e1E9E3A", "wC8FFD0");

        add("gold_ingot", new String[]{
                "........",
                "..kkkkk.",
                ".kwYYYYk",
                "kYYYYYyk",
                "kYYYYyyk",
                ".kyyyyk.",
                "..kkkk..",
                "........"},
                "k6B5410", "YF5D142", "yC9A227", "wFFF2A8");

        add("tnt", new String[]{
                "RrRRrRRr",
                "RRrRRRrR",
                "WWWWWWWW",
                "WkWkWkWW",
                "WWWWWWWW",
                "RRrRRRrR",
                "RrRRrRRr",
                "rRRRRrRR"},
                "RD03030", "rA02020", "WF0F0F0", "k222222");

        add("chest", new String[]{
                "DDDDDDDD",
                "DBBBBBBD",
                "DBBBBBBD",
                "DDDYYDDD",
                "DbbYYbbD",
                "DbbbbbbD",
                "DbbbbbbD",
                "DDDDDDDD"},
                "D3A2410", "B8A5A2A", "b6A4220", "YE0C040");
    }

    private Icons() {
    }

    /** Draws a picture; each of its 8x8 pixels becomes a square of {@code scale} screen pixels. */
    static void draw(GuiGraphicsExtractor g, String id, int x, int y, int scale) {
        Icon icon = ICONS.get(id);
        if (icon == null) {
            icon = ICONS.get(IDS[0]);
        }
        for (int r = 0; r < icon.rows().length; r++) {
            String row = icon.rows()[r];
            for (int c = 0; c < row.length(); c++) {
                Integer color = icon.palette().get(row.charAt(c));
                if (color == null) {
                    continue; // "." = see-through
                }
                g.fill(x + c * scale, y + r * scale, x + (c + 1) * scale, y + (r + 1) * scale, color);
            }
        }
    }
}
