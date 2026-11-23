package com.example.commandaliases;

/**
 * Turns a key code into something readable ("H", "F5", "Left Shift").
 *
 * Minecraft 26.3 switched from GLFW to SDL, so key codes are SDL scancodes
 * (the physical key position, same numbers as the USB keyboard standard).
 * Plain numbers are used so no extra libraries are needed.
 */
final class KeyNames {
    static final int ESCAPE = 41;

    private KeyNames() {
    }

    /** Name for a physical key (SDL scancode). */
    static String name(int key) {
        if (key < 0) {
            return "Not set";
        }
        if (key >= 4 && key <= 29) {           // A-Z
            return String.valueOf((char) ('A' + (key - 4)));
        }
        if (key >= 30 && key <= 38) {          // 1-9
            return String.valueOf((char) ('1' + (key - 30)));
        }
        if (key >= 58 && key <= 69) {          // F1-F12
            return "F" + (key - 58 + 1);
        }
        if (key >= 104 && key <= 115) {        // F13-F24
            return "F" + (key - 104 + 13);
        }
        if (key >= 89 && key <= 97) {          // keypad 1-9
            return "Keypad " + (key - 89 + 1);
        }
        switch (key) {
            case 39: return "0";
            case 40: return "Enter";
            case 41: return "Escape";
            case 42: return "Backspace";
            case 43: return "Tab";
            case 44: return "Space";
            case 45: return "-";
            case 46: return "=";
            case 47: return "[";
            case 48: return "]";
            case 49: return "\\";
            case 50: return "#";
            case 51: return ";";
            case 52: return "'";
            case 53: return "`";
            case 54: return ",";
            case 55: return ".";
            case 56: return "/";
            case 57: return "Caps Lock";
            case 70: return "Print Screen";
            case 71: return "Scroll Lock";
            case 72: return "Pause";
            case 73: return "Insert";
            case 74: return "Home";
            case 75: return "Page Up";
            case 76: return "Delete";
            case 77: return "End";
            case 78: return "Page Down";
            case 79: return "Right Arrow";
            case 80: return "Left Arrow";
            case 81: return "Down Arrow";
            case 82: return "Up Arrow";
            case 83: return "Num Lock";
            case 84: return "Keypad /";
            case 85: return "Keypad *";
            case 86: return "Keypad -";
            case 87: return "Keypad +";
            case 88: return "Keypad Enter";
            case 98: return "Keypad 0";
            case 99: return "Keypad .";
            case 100: return "\\";
            case 224: return "Left Ctrl";
            case 225: return "Left Shift";
            case 226: return "Left Alt";
            case 228: return "Right Ctrl";
            case 229: return "Right Shift";
            case 230: return "Right Alt";
            default: return "Key " + key;
        }
    }

    /**
     * Name to show for a key that was just pressed. The "keycode" is what is printed on the key
     * for the player's keyboard layout (so AZERTY etc. show correctly); other keys use the table.
     */
    static String fromEvent(int scancode, int keycode) {
        if (keycode >= 33 && keycode <= 126) {
            return String.valueOf(Character.toUpperCase((char) keycode));
        }
        return name(scancode);
    }

    /** Ctrl / Shift / Alt / Windows keys (left and right). */
    static boolean isModifier(int key) {
        return key >= 224 && key <= 231;
    }

    /** "H", "Ctrl+H", "Ctrl+Shift+F5" ... mods: 1 = Shift, 2 = Ctrl, 4 = Alt. */
    static String combo(int mods, int key, String keyName) {
        if (key < 0) {
            return "Not set";
        }
        StringBuilder sb = new StringBuilder();
        if ((mods & 2) != 0) {
            sb.append("Ctrl+");
        }
        if ((mods & 1) != 0) {
            sb.append("Shift+");
        }
        if ((mods & 4) != 0) {
            sb.append("Alt+");
        }
        String n = (keyName != null && !keyName.isEmpty()) ? keyName : name(key);
        return sb.append(n).toString();
    }
}
