# Command Aliases (Fabric, Minecraft 26.3)

Client-side mod. Make your own short commands from an in-game menu.

- Open the menu with the **Command Aliases** keybind (default `K`, rebindable in Options > Controls > Key Binds) or by typing `/aliases`.
- **Add new command**: first box = what you type (`/s`), second box = what it runs (`/gamemode survival`).
- Saved to `config/commandaliases/aliases.json` (editable by hand). They stay until you remove them.
- Extra words go on the end: `/s Steve` -> `/gamemode survival Steve`. Or place them with `{args}`.
- Chain commands with `;` : `/heal ; /feed`.
- ON/OFF toggle per alias, Edit, Remove (click twice to confirm).
- Aliases don't call other aliases (no loops).
