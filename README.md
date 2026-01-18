# Coal Mod

A Hytale mod that adds coal ore to the game that can be used as fuel.

## Features

- Adds coal ore blocks to the game
- Coal ore can be used as fuel for furnaces and other fuel-consuming blocks
- Coal ore generates in the world (all zones) via a custom worldgen provider

## Installation (ore generation)

**Recommended — normal mod:** Place the built JAR in the `mods/` folder at your server root. The mod replaces the built-in "Hytale" worldgen provider with one that copies the base worldgen, overlays coal ore node files from the mod JAR, and runs the standard loader. No early plugins or Mixin required. If coal does not generate, try ensuring the mod loads after the built-in worldgen plugin (load order can vary by environment).

**Alternative — early plugin:** For the bytecode-transform approach, place the JAR in `earlyplugins/` instead. See [docs/EARLY_PLUGIN_SETUP.md](docs/EARLY_PLUGIN_SETUP.md). Use either `mods/` or `earlyplugins/`, not both, for ore generation.

## Library abstraction

`WorldGenOverlayHelper` and `ModOresWorldGenProvider` are structured so they can be moved into a library mod later. The overlay resource paths and merge-entry prefixes (e.g. `Ores.Coal.`) can be supplied by a builder or config instead of hardcoding.

