# Vanguard 0.4

Client-side PvP utility mod for anarchy servers. Fabric, Minecraft 1.21.11, Mojang mappings.
Open the menu with Right Shift. Click a card to toggle, click "KeyBind" on a card to bind a key
(Delete clears it), right-click a card for its settings.

## Getting the .jar
Upload this folder to a GitHub repo (the `.github/workflows/build.yml` file builds it), then
download the `vanguard-jar` artifact from the finished run in the Actions tab. Needs Fabric API
for 1.21.11 in your mods folder. Local build: JDK 21 + Gradle 9.2+, `gradle build`.

## Modules
Combat: AutoHitCrystal, CrystalOptimizer, AnchorMacro, AutoCart, CartRefill, AutoTotem, AutoRefill,
        AutoXP, TriggerBot, AutoShieldBreak, AutoStunSlam, AutoMace, InstantLunge, AutoTap, JumpReset
Movement: Sprint, AutoWalk, Flight, AutoMLG, Freecam
World: BaseFinder, SchematicBuilder, Xray, TunnelESP, AmethystNotifier
Visuals: Fullbright, Galaxy, SwordInspect, PlayerESP, Tracers, JumpCircles, RangeRing, FpsDisplay, PingDisplay
Misc: AutoMine, PerformanceMode

TriggerBot, AutoHitCrystal, AutoShieldBreak, AutoStunSlam, AutoMace and InstantLunge only act on
what is under your crosshair at normal game reach. Nothing aims for you.

## Commands
    /vg list | toggle <module> | set <module> <setting> <value> | save
    /vg friend add|remove <name>
    /vg bases [clear]
    /vg schem list | load "<file>" | origin | clear | info

## Notes
- BaseFinder highlights: chests orange, shulker boxes pink, barrels brown, hoppers grey.
- Xray highlights ores through walls (it does not make stone see-through).
- SchematicBuilder places block types only, not block state.
- SwordInspect is a HUD overlay, press I.
- Settings and friends save to config/vanguard.json; enabled state does not.
- Galaxy is drawn over the vanilla sky (terrain hides it), so it also covers the sun and moon.
- Not built: auto pearl catch, auto drain, safe anchor, auto attribute swap (need definitions).
