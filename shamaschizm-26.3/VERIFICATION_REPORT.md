# Minecraft 26.3 port verification — 2026-10-08

Baseline: `VedranJakelic/shamaschizm-neoforge`, commit `1caeb78a762d59669dd0cc4390e6ab47822432e0`.
Target: Minecraft 26.3; NeoForge 26.3.0.55-beta; Temurin Java 25.0.4.1+1.

## Passed

- Compiled all Java sources against the exact official NeoForge-patched 26.3 client API and its dependencies. Only deprecation notes remain.
- Loaded the mod on the exact official NeoForge-patched dedicated server, loaded its registries/data packs, created a world and generated a Schizm dungeon.
- All **70** existing integration checks passed: souls (including zero and legacy migration), trades, equipment/anvil upgrades, Schizm scale, safe portal arrival, return travel, reentry and saved portal links. Raw results accompany this report.
- Loaded/transformed all **19 distinct mixin target classes** under FML's client distribution, including the new Ascent trim target. This catches injection/signature failures without requiring a graphics window.
- After converting loot formats, the prior unreachable-loot warnings disappeared.

## Changes

Updated version metadata and migrated renamed/changed entity, sound, structure, renderer, animation, culling, spawning, block and GPU APIs. Updated client injection targets and shield blocking hook.

Migrated recipe advancements, stag spawn counts, glow trim material and loot conditions/modifiers to 26.3 schemas. Kept animated Ascent sprites in a registered custom atlas. Added explicit legacy Forge soul import because NeoForge no longer imports ForgeData. Added explicit shader input/output locations matching 26.3 conventions.

The portal integration assertion was corrected to check a safe arrival **near** the scaled portal anchor: the existing baseline code stores a safe tile beside the template portal, not the exact anchor. The test also restores normal difficulty after its peaceful shaman check before generating the dungeon. Production portal behavior was not changed for this assertion.

## Limits and reproduction

No graphical client was available. Rendering, shader execution, sounds, combat timing, multiplayer interactions, copied-world upgrades and performance still require the manual checklist. The headless mixin probe does not validate pixels or client resource reloads.

The candidate JAR was packaged directly from javac output plus mod resources and expanded NeoForge metadata; verification-only classes and Blockbench source models are excluded. Minecraft/NeoForge binaries and dependencies are not bundled. The normal Gradle build was not completed in this environment, so run `gradlew.bat clean build` / `./gradlew clean build` locally before publishing.

The repository's verification run is `runVerificationServer`; use its disposable world and Java 25. It writes `verification-results.txt` in the run directory. Do not count process exit alone as success: check the report for FAIL lines and the final server log for the success message. The existing `clientSmoke` task has no automated graphical assertions.

This report supersedes the old 26.2 verification report only for the target listed above. The 184 manual checks and 353 resource entries remain unchecked until tested in game.
