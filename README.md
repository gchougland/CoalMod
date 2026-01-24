# CoalMod

A data-driven mod for Hytale that adds coal ore to world generation using only JSON files - no Java code required!

## Overview

CoalMod demonstrates the **data-driven approach** to adding ores using WorldGenOverlayLib. This mod contains:
- **Zero Java code** - completely JSON-based
- Overlay configuration file
- Ore node definitions
- Entry.node.json files for ore distribution

## How It Works

CoalMod uses WorldGenOverlayLib's auto-discovery feature:

1. The overlay configuration is defined in `Server/WorldGenOverlays/overlays.json`
2. WorldGenOverlayLib's `AutoOverlayPlugin` automatically discovers and registers the overlay
3. All coal ore node files are placed in the standard worldgen structure
4. Entry.node.json files are automatically merged with base worldgen

## File Structure

```
src/main/resources/
└── Server/
    ├── WorldGenOverlays/
    │   └── overlays.json          (overlay configuration - defines zones and node files)
    └── World/
        └── Default/
            └── Zones/
                └── [Zone]/
                    └── Cave/
                        └── Ores/
                            ├── Entry.node.json          (ore distribution - merged automatically)
                            └── Coal/
                                ├── CoalSpread.node.json
                                ├── CoalSpread1.node.json
                                ├── CoalColumn.node.json
                                ├── CoalVeinSmall.node.json
                                └── CoalVeinLarge.node.json
                                ... (and variants 1-5 for each type)
```

## Configuration

The overlay is configured in `Server/WorldGenOverlays/overlays.json`:

```json
{
  "Overlays": [
    {
      "Name": "Coal Ore Overlay",
      "Generator": "Default",
      "MergePrefix": "Ores.Coal.",
      "OreName": "Coal",
      "Zones": [
        "Oceans",
        "Zone1_Shallow_Ocean",
        "Zone1_Spawn",
        "Zone1_Temple",
        "Zone1_Tier1",
        "Zone1_Tier2",
        "Zone1_Tier3",
        "Zone2_Shallow_Ocean",
        "Zone2_Tier1",
        "Zone2_Tier2",
        "Zone2_Tier3",
        "Zone3_Shallow_Ocean_Tier1",
        "Zone3_Shallow_Ocean_Tier2",
        "Zone3_Shallow_Ocean_Tier3",
        "Zone3_Tier1",
        "Zone3_Tier2",
        "Zone3_Tier3",
        "Zone4_Tier4",
        "Zone4_Tier5"
      ],
      "NodeFiles": [
        "Coal/CoalSpread.node.json",
        "Coal/CoalSpread1.node.json",
        ... (all coal node files)
      ]
    }
  ]
}
```

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
   ./gradlew build publish
   ```

2. Build CoalMod:
   ```bash
   cd ../CoalMod
   ./gradlew build
   ```

3. Install both mods to your Hytale server

## Benefits of Data-Driven Approach

- **No Java Knowledge Required**: Just edit JSON files
- **Easy to Modify**: Change zones, add/remove node files by editing the config
- **Easy to Share**: Distribute as a resource pack - no compilation needed
- **Version Control Friendly**: JSON files are easy to diff and merge
- **Quick Iteration**: Change config and rebuild - no code compilation

## Customization

To customize CoalMod:

1. **Add/Remove Zones**: Edit `overlays.json` and modify the `Zones` array
2. **Change Node Files**: Edit the `NodeFiles` array in `overlays.json`
3. **Modify Ore Distribution**: Edit the `Entry.node.json` files in each zone
4. **Adjust Ore Generation**: Edit the individual node files (CoalSpread, CoalColumn, etc.)

## Example: Adding a New Zone

Simply add the zone name to the `Zones` array in `overlays.json`:

```json
"Zones": [
  "Zone1_Tier1",
  "Zone1_Tier2",
  "NewZoneName"  // Add this
]
```

Make sure you have the corresponding `Entry.node.json` and node files in:
```
Server/World/Default/Zones/NewZoneName/Cave/Ores/
```

## License

Same as WorldGenOverlayLib project.
