# Vanguard 0.2.1

Client-side PvP utility mod for anarchy servers. Fabric, Minecraft 1.21.11, Mojang mappings.
Open the menu with Right Shift.

## Getting the .jar
This folder is source code. A .jar has to be compiled, which needs the Minecraft and Fabric
libraries downloaded, so it can't be done from inside this chat. Two ways to get it:

**A. No installs (GitHub Actions, about 3 minutes)**
1. Make a free GitHub account, create a new repository, and upload everything in this folder
   (including the hidden `.github` folder).
2. Open the repository's Actions tab. The "Build jar" workflow runs on every push.
   You can also press "Run workflow".
3. When it finishes, open the run and download the `vanguard-jar` artifact. Unzip it: the .jar inside
   is the mod.

**B. On your PC**
Install JDK 21 and Gradle 9.2+, open a terminal in this folder, run `gradle build`.
The jar is in `build/libs/` (the one without "sources" in the name).

Put the jar and Fabric API for 1.21.11 in your `mods` folder.

If a version in `gradle.properties` doesn't resolve, copy the current ones from
https://fabricmc.net/develop. If the build fails on a method name, it is almost certainly a
Minecraft internal that changed spelling; paste the error to get it fixed.

## Menu
Left sidebar picks the category, the header has search, cards toggle on click and open
settings on right-click. Keybinds are set in Options > Controls > Key Binds > Vanguard.

## Modules
Combat: AutoCrystal, CrystalOptimizer, AnchorMacro, AutoTotem, AutoRefill, AutoXP, KillAura
Movement: Sprint, AutoWalk
World: BaseFinder, SchematicBuilder
Visuals: Fullbright, SwordInspect (press the Inspect key, default I), HudOptions

## Commands
    /vg list | toggle <module> | set <module> <setting> <value> | save
    /vg profile list | save <name> | load <name> | delete <name>
    /vg friend add|remove <name>
    /vg bases [clear]
    /vg schem list | load "<file>" | origin | clear | info

## Schematics
Put `.litematic` or vanilla `.nbt` files in `<game folder>/vanguard/schematics/`.
`/vg schem load "castle.litematic"`, stand at the schematic's lowest corner, run
`/vg schem origin`, enable SchematicBuilder. It builds bottom-up from blocks in your inventory.
It places the block type only, not block state (stair direction, slab half, log axis), so it
suits full blocks.

## Known limits
- Never compiled or run by the author. Expect a small fix or two on first build.
- SwordInspect is a HUD overlay, not a change to the first-person hand model.
- AutoCrystal/AnchorMacro damage estimates ignore enchantments and potion effects.
- BaseFinder flags chunks with many chests/barrels/shulkers/hoppers, so dungeons and villages
  can trigger it. It only sees chunks your client has loaded.
- Settings, friends and theme are saved; enabled state is not. Named profiles are supported with `/vg profile save <name>`, `/vg profile load <name>`, `/vg profile list`, and `/vg profile delete <name>`. HUD elements can be individually hidden with `/vg set HudOptions watermark|modules|itemcounts|basefinder|swordinspect 0` and restored with value `1`.
