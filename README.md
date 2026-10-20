# Vanguard 0.5.1

Client-side PvP utility mod for anarchy servers. Fabric, Minecraft 1.21.11, Mojang mappings.
Open the menu with Right Shift. Click a card to toggle, click "KeyBind" on a card to bind a key
(Delete clears it), right-click a card for its settings.

## Getting the .jar
Upload this folder to a GitHub repo (the `.github/workflows/build.yml` file builds it), then
download the `vanguard-jar` artifact from the finished run in the Actions tab. Needs Fabric API
for 1.21.11 in your mods folder. Local build: JDK 21 + Gradle 9.2+, `gradle build`.

## Modules
Combat: AutoHitCrystal, InstantCrystal, CrystalOptimizer, AnchorMacro, SafeAnchor, AutoCart, CartRefill, AutoTotem,
        AutoRefill, AutoXP, AutoDrain, TriggerBot, AutoShieldBreak, AutoStunSlam, AutoMace,
        InstantLunge, AutoTap, JumpReset, Reach, WallHit, AutoTrap
Movement: Sprint, AutoWalk, Flight, Boatfly, AirWalk, AutoMLG, AutoPearlCatch, Freecam, Orbit
World: BaseFinder, SchematicBuilder, Xray, TunnelESP, AmethystNotifier
Visuals: Fullbright, Galaxy, Freelook, SwordInspect, ItemLean, SwingAnimations, PlayerESP, Tracers, JumpCircles, RangeRing, FpsDisplay, PingDisplay
Misc: AutoMine, PerformanceMode, FastUse

TriggerBot, AutoHitCrystal, AutoShieldBreak, AutoStunSlam, AutoMace and InstantLunge only act on
what is under your crosshair, at your current reach (normal unless Reach is on). Nothing aims for you.
Reach only changes what the client allows; many servers reject hits past roughly 3-4 blocks.

## Commands
    /vg list | toggle <module> | set <module> <setting> <value> | save
    /vg friend add|remove <name>
    /vg bases [clear]
    /vg schem list | load "<file>" | origin | clear | info

## Notes
- BaseFinder highlights: chests orange, shulker boxes pink, barrels brown, hoppers grey.
- Xray hides every block except ores, storage and base blocks so you see them through the ground (use with Fullbright). It also draws backup ore boxes (`boxes`).
- SchematicBuilder places block types only, not block state.
- SwordInspect is a HUD overlay, press I.
- Settings and friends save to config/vanguard.json; enabled state does not.
- Galaxy is drawn over the vanilla sky (terrain hides it), so it also covers the sun and moon.
- AutoPearlCatch and AutoCart are one-shot: switch them on, they run once, then switch themselves off.
- SafeAnchor walls you off from the anchor with glowstone in the blocks beside you (you cannot place a block in your own spot).
- Not built: auto attribute swap (needs a definition).

- ItemLean and SwingAnimations hook the first-person hand renderer by method name. If the game's names differ,
  the hook is skipped (no crash) and the effect simply does not show.
