# Coal Mod

A Hytale mod that adds coal ore to the game that can be used as fuel.

## Features

- Adds coal ore blocks to the game (Stone, Basalt, Sandstone, Shale, Volcanic variants)
- Coal ore can be used as fuel for furnaces and other fuel-consuming blocks
- Coal ore generates in the world across all zones (0-19 zones) via a custom worldgen provider
- Coal ore spawns at all height levels (Y 0-255) with distribution similar to iron ore

## Installation

Place the built JAR in the `mods/` folder at your server root. The mod uses a custom `IWorldGenProvider` that:

1. Copies the base worldgen configuration to a temporary directory
2. Overlays coal ore node files from the mod JAR (Entry.node.json merges, CoalSpread/Column/Vein nodes)
3. Delegates to the standard Hytale worldgen loader

The mod registers itself as the "Hytale" worldgen provider, replacing the built-in one.

## World Generation Details

- **Entry Distribution:** Coal is added to the ore distribution wheel in each zone's `Entry.node.json`
- **Repeat:** Each coal variant (CoalSpread, CoalSpread1-5) has `Repeat: [110, 120]`
- **Height Distribution:** CoalColumn uses `Length: [255, 10]` with `PitchSet: [0, 180]` on CoalSpread children to enable spawning from Y 10 to Y 255
- **Vein Types:** 12 vein files per zone (CoalVeinSmall/Large × 6 variants) matching copper's structure

## Library Abstraction

`WorldGenOverlayHelper` and `ModOresWorldGenProvider` are structured so they can be extracted into a library mod later. The overlay resource paths and merge-entry prefixes (e.g. `Ores.Coal.`) can be supplied by a builder or config instead of hardcoding.

