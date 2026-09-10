# Granules

A mod that adds a lot of little things.

Authored by ProtoAno for Minecraft 26.2 and Fabric.

[Modrinth](https://modrinth.com/mod/granules)

## Building

Install JDK 25 or newer and set JAVA_HOME to it. Run `./gradlew build` on Linux/macOS or `.\gradlew.bat build` on Windows. The build downloads dependencies and runs the game tests. Output JARs appear in `build/libs/`.

Fabric Loader and Fabric API are required at runtime. Mod Menu and Cloth Config provide the optional settings interface. Jade and REI integrations are optional.

The source and runtime resources live in `src/`. Minimal Pet Bed generation inputs live in `build-inputs/`. Gradle caches, generated output, test worlds, and local profile files are excluded from version control.

## License

Mod code is licensed under LGPL-3.0-only; see LICENSE. Third-party Minecraft material textures and credited music remain subject to their respective rights holders; the code license does not grant rights to those assets.
