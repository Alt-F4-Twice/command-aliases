package com.example.commandaliases;

import java.util.ArrayList;
import java.util.List;

/** A profile: its own quick commands, hotkeys and automations. Only one profile is on at a time. */
public final class Profile {
    public String id = Long.toHexString(System.nanoTime());
    public String name = "Profile";
    /** One of the picture ids in Icons. */
    public String icon = "grass_block";
    public List<AliasEntry> aliases = new ArrayList<>();
    public List<HotkeyEntry> hotkeys = new ArrayList<>();
    public List<AutomationEntry> automations = new ArrayList<>();

    public Profile() {
    }
}
