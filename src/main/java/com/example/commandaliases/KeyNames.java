package com.example.commandaliases;

/**
 * Turns a key code into something readable ("H", "F5", "Left Shift").
 * Uses plain numbers (the standard GLFW key codes) so it needs no extra libraries.
 */
final class KeyNames {
    static final int ESCAPE = 256;

    private KeyNames() {
    }

    static String name(int key) {
        if (key < 0) {
            return "Not set";
        }
        if (key >= 65 && key <= 90) {          // A-Z
            return String.valueOf((char) key);
        }
        if (key >= 48 && key <= 57) {          // 0-9
            return String.valueOf((char) key);
        }
        if (key >= 290 && key <= 314) {        // F1-F25
            return "F" + (key - 290 + 1);
        }
        if (key >= 320 && key <= 329) {        // keypad 0-9
            return "Keypad " + (key - 320);
        }
        switch (key) {
            case 32: return "Space";
            case 39: return "'";
            case 44: return ",";
            case 45: return "-";
            case 46: return ".";
            case 47: return "/";
            case 59: return ";";
            case 61: return "=";
            case 91: return "[";
            case 92: return "\\";
            case 93: return "]";
            case 96: return "`";
            case 257: return "Enter";
            case 258: return "Tab";
            case 259: return "Backspace";
            case 260: return "Insert";
            case 261: return "Delete";
            case 262: return "Right Arrow";
            case 263: return "Left Arrow";
            case 264: return "Down Arrow";
            case 265: return "Up Arrow";
            case 266: return "Page Up";
            case 267: return "Page Down";
            case 268: return "Home";
            case 269: return "End";
            case 280: return "Caps Lock";
            case 340: return "Left Shift";
            case 341: return "Left Ctrl";
            case 342: return "Left Alt";
            case 344: return "Right Shift";
            case 345: return "Right Ctrl";
            case 346: return "Right Alt";
            default: return "Key " + key;
        }
    }
}
