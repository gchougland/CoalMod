# CoalMod

A data-driven mod for Hytale that adds coal ore to world generation using only JSON files - no Java code required!

## Overview

CoalMod demonstrates the **data-driven approach** to adding ores using [WorldGenOverlayLib](https://github.com/gchougland/WorldGenOverlayLib). This mod contains:

- **Zero Java code** - completely JSON-based
- Overlay configuration file
- Ore definition files (`Coal.json` and `CoalPlacement.json` for each zone)

## How It Works

CoalMod uses WorldGenOverlayLib's auto-discovery feature:

1. The overlay configuration is defined in `Server/WorldGenOverlays/overlays.json`
2. WorldGenOverlayLib's `AutoOverlayPlugin` automatically discovers and registers the overlay
3. All coal ore files are placed in the `Ores/ZoneX/` structure
4. Placement references are automatically merged into `Caves.json` files for all relevant zones

## File Structure

```
CoalMod/
└── Server/
    ├── WorldGenOverlays/
    │   └── overlays.json          (overlay configuration)
    └── World/
        └── Default/
            └── Ores/
                ├── Zone0/
                │   ├── Coal.json
                │   └── CoalPlacement.json
                ├── Zone1/
                │   ├── Coal.json
                │   └── CoalPlacement.json
                ├── Zone2/
                │   ├── Coal.json
                │   └── CoalPlacement.json
                ├── Zone3/
                │   ├── Coal.json
                │   └── CoalPlacement.json
                └── Zone4/
                    ├── Coal.json
                    └── CoalPlacement.json
```

## Configuration

The overlay is configured in `Server/WorldGenOverlays/overlays.json`:

```json
{
  "Overlays": [
    {
      "Name": "Coal Ore Overlay",
      "Generator": "Default",
      "Ores": ["Coal"],
      "Zones": [
        "Zone0",
        "Zone1",
        "Zone2",
        "Zone3",
        "Zone4"
      ]
    }
  ]
}
```

**Configuration Fields:**
- `Name` (optional): Display name for the overlay
- `Generator` (required): World generator name (usually `"Default"`)
- `Ores` (required): Array of ore names (e.g., `["Coal"]`) - all ores share the same zones
- `Zones` (required): Array of ore zone numbers (`"Zone0"` through `"Zone4"`) - applies to all ores

## Dependencies

- **WorldGenOverlayLib**: Required for overlay discovery and merging
- **Hytale:WorldGen**: Required for world generation

## Building

Since this is a data-driven mod with no Java code, building is simple:

```bash
./gradlew build
```

The build will:
- Process resources (including manifest.json)
- Package everything into a JAR
- No Java compilation needed!

## Installation

1. Build WorldGenOverlayLib first:
   ```bash
   cd ../WorldGenOverlayLib
   ./gradlew build publishToMavenLocal
   ```

2. Build CoalMod:
   ```bash
   cd ../CoalMod
   ./gradlew build
   ```

3. Install both mods to your Hytale server:
   - Copy `WorldGenOverlayLib/build/libs/WorldGenOverlayLib-1.0.0.jar` to your server's `mods/` folder
   - Copy `CoalMod/build/libs/CoalMod-1.0.0.jar` to your server's `mods/` folder

## Benefits of Data-Driven Approach

- **No Java Knowledge Required**: Just edit JSON files
- **Easy to Modify**: Change zones by editing the config
- **Easy to Share**: Distribute as a resource pack - no compilation needed
- **Version Control Friendly**: JSON files are easy to diff and merge
- **Quick Iteration**: Change config and rebuild - no code compilation

## Customization

To customize CoalMod:

1. **Add/Remove Zones**: Edit `overlays.json` and modify the `Zones` array
2. **Modify Ore Generation**: Edit the `Coal.json` files in each zone folder
3. **Adjust Ore Placement**: Edit the `CoalPlacement.json` files to change density, height ranges, block masks, etc.

## Example: Adding a New Zone

Simply add the zone number to the `Zones` array in `overlays.json`:

```json
"Zones": [
  "Zone0",
  "Zone1",
  "Zone2",
  "Zone3",
  "Zone4",
  "Zone5"  // Add this (if Zone5 exists in Hytale)
]
```

Make sure you have the corresponding `Coal.json` and `CoalPlacement.json` files in:

```
Server/World/Default/Ores/Zone5/
```

## License

See [LICENSE](LICENSE) file for details.
