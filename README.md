# Command Aliases (Fabric, Minecraft 26.3)

Client-side mod. Make your own short commands, hotkeys and automations from an in-game menu.

Open the menu with the **Command Aliases** keybind (default `K`, rebindable in Options > Controls) or type `/aliases`.

## Profiles
Pick a profile first. Only one is on at a time and each has its own commands, hotkeys and automations.
Each profile has a small picture taken from Minecraft's own textures (grass block, bedrock, crafting table, command block, diamond, redstone, emerald, gold, TNT, diamond ore).
The menu remembers your tab and page until you leave the world or server.

## Commands
- `/s` -> `/gamemode survival`. Not case sensitive.
- Prefixes: `/` (typed as a command), `!` and `?` (typed in chat), e.g. `!s` or `?s`.
- Extra words go on the end (`/s Steve`) or where you put `{args}`.
- Chain builder: add more commands, each with a wait of 0-5 seconds. (Under the hood it is just text: `/gamemode creative ; wait 2 ; /tp @s ~ 100 ~`.)

## Hotkeys
Click the key button, press a key (hold Ctrl / Shift / Alt for combos like `Ctrl+H`), then choose what it runs.

## Automations
Run commands when you **join** a world or server, when you **die**, or when you **respawn**.

## Extras
ON/OFF toggle, Edit, Remove (click twice), drag rows to reorder, **Reload** to re-read the file after editing it by hand.

Saved in `config/commandaliases/profiles.json`. Older `aliases.json` / `hotkeys.json` files are brought into a "Default" profile the first time.

## Licence
All rights reserved (c) 2026 Max. See `LICENSE`.
