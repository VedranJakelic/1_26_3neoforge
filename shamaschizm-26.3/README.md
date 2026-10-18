# ShamaSchizm — Minecraft 26.3 port candidate

Target: Minecraft **26.3**, NeoForge **26.3.0.55-beta**, Java **25**.
Based on GitHub commit `1caeb78a762d59669dd0cc4390e6ab47822432e0`.
The original 26.2 repository branch is unchanged.

## Install

Use a separate 26.3 NeoForge profile. Put `shamaschizm-1.1.0+mc26.3.jar` in its `mods` directory and remove the older Shamaschizm JAR from that profile. Use the same version on a dedicated server and its clients. Test a copy of existing worlds.

## Build source

Set JAVA_HOME to a Java 25 JDK, then run `gradlew.bat clean build` on Windows or `./gradlew clean build` on Linux/macOS. The mod JAR goes in `build/libs`.

## Validation

See [VERIFICATION_REPORT.md](VERIFICATION_REPORT.md). Compilation, dedicated-server startup, 70 integration checks and 19 client mixin target transformations passed. A graphical client playthrough and normal Gradle/launcher build remain to be verified. This is a testing candidate, not a claim that every feature was played through.

Use [the 184-test checklist](docs/26.3-test-checklist.md) and [resource inventory](docs/26.3-content-inventory.md) for acceptance testing. Report failures with their test ID and `logs/latest.log`.
