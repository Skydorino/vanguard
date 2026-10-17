# Vanguard 0.2.1 source changes

## Added
- `HudOptions` settings module to toggle watermark, enabled-module list, item counts, BaseFinder HUD, and SwordInspect overlay independently.
- Named JSON profiles via `/vg profile list`, `/vg profile save <name>`, `/vg profile load <name>`, and `/vg profile delete <name>`.
- Safer profile-name validation and directory creation.
- More defensive configuration parsing for malformed settings and friend entries.

## Notes
- Existing combat modules were retained; this patch focuses on configurability, HUD/visual controls, and configuration reliability.
- This source has not been compiled in this environment because Gradle is not installed and external network access is unavailable to download the Gradle distribution and Minecraft/Fabric dependencies.
- Build and test with JDK 21 and Gradle 9.2+ as documented in README.md before using in-game.
