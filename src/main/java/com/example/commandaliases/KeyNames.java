package com.example.commandaliases;

import org.lwjgl.glfw.GLFW;

/** Turns a GLFW key code into something readable ("H", "F5", "Left Shift"). */
final class KeyNames {
    private KeyNames() {
    }

    static String name(int key) {
        if (key < 0) {
            return "Not set";
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) {
            return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        }
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            return "Keypad " + (key - GLFW.GLFW_KEY_KP_0);
        }
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_RIGHT: return "Right Arrow";
            case GLFW.GLFW_KEY_LEFT: return "Left Arrow";
            case GLFW.GLFW_KEY_DOWN: return "Down Arrow";
            case GLFW.GLFW_KEY_UP: return "Up Arrow";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Right Alt";
            default: break;
        }
        String printable = GLFW.glfwGetKeyName(key, 0);
        if (printable != null && !printable.isEmpty()) {
            return printable.toUpperCase();
        }
        return "Key " + key;
    }
}
