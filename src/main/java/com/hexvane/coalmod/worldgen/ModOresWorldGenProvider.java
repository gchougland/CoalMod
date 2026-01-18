package com.hexvane.coalmod.worldgen;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.procedurallib.json.SeedString;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.universe.world.worldgen.WorldGenLoadException;
import com.hypixel.hytale.server.worldgen.HytaleWorldGenProvider;
import com.hypixel.hytale.server.worldgen.SeedStringResource;
import com.hypixel.hytale.server.worldgen.loader.ChunkGeneratorJsonLoader;
import com.hypixel.hytale.server.worldgen.prefab.PrefabStoreRoot;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends HytaleWorldGenProvider so WorldConfig.save() can encode us with
 * HytaleWorldGenProvider.CODEC when WorldGen's registration overwrites ours.
 * Overrides getGenerator() to copy the base worldgen, overlay mod ore nodes, then load.
 */
public class ModOresWorldGenProvider extends HytaleWorldGenProvider {

    /** Same config shape as parent for encode/decode compatibility. */
    public static final BuilderCodec<ModOresWorldGenProvider> CODEC = BuilderCodec
            .builder(ModOresWorldGenProvider.class, ModOresWorldGenProvider::new)
            .documentation("Hytale world generator with mod ore overlays (e.g. coal).")
            .<String>append(new KeyedCodec<>("Name", Codec.STRING), (c, s) -> { c.name = s; setParentField(c, "name", s); }, c -> c.name)
            .documentation("The name of the generator to use. \"Default\" if not provided.")
            .add()
            .<String>append(new KeyedCodec<>("Path", Codec.STRING), (c, s) -> { c.path = s; setParentField(c, "path", s); }, c -> c.path)
            .documentation("The path to the world generation configuration. Defaults to the server worldgen folder if not set.")
            .add()
            .build();

    private static final String RESOURCE_PREFIX = "Server/World/";
    private static final String COAL_PREFIX = "Ores.Coal.";

    private static final String[] ZONES = {
            "Oceans", "Zone1_Shallow_Ocean", "Zone1_Spawn", "Zone1_Temple",
            "Zone1_Tier1", "Zone1_Tier2", "Zone1_Tier3",
            "Zone2_Shallow_Ocean", "Zone2_Tier1", "Zone2_Tier2", "Zone2_Tier3",
            "Zone3_Shallow_Ocean_Tier1", "Zone3_Shallow_Ocean_Tier2", "Zone3_Shallow_Ocean_Tier3",
            "Zone3_Tier1", "Zone3_Tier2", "Zone3_Tier3",
            "Zone4_Tier4", "Zone4_Tier5"
    };

    /** Coal nodes: Spread→Column→VeinSmall/VeinLarge, 6 variants. Same shape as vanilla Copper. */
    private static final String[] COAL_NODE_FILES = {
            "Coal/CoalSpread.node.json", "Coal/CoalSpread1.node.json", "Coal/CoalSpread2.node.json",
            "Coal/CoalSpread3.node.json", "Coal/CoalSpread4.node.json", "Coal/CoalSpread5.node.json",
            "Coal/CoalColumn.node.json", "Coal/CoalColumn1.node.json", "Coal/CoalColumn2.node.json",
            "Coal/CoalColumn3.node.json", "Coal/CoalColumn4.node.json", "Coal/CoalColumn5.node.json",
            "Coal/CoalVeinSmall.node.json", "Coal/CoalVeinSmall1.node.json", "Coal/CoalVeinSmall2.node.json",
            "Coal/CoalVeinSmall3.node.json", "Coal/CoalVeinSmall4.node.json", "Coal/CoalVeinSmall5.node.json",
            "Coal/CoalVeinLarge.node.json", "Coal/CoalVeinLarge1.node.json", "Coal/CoalVeinLarge2.node.json",
            "Coal/CoalVeinLarge3.node.json", "Coal/CoalVeinLarge4.node.json", "Coal/CoalVeinLarge5.node.json"
    };

    // Keep our own copy for getGenerator(); parent's privates are synced for encode via *Refl.
    private String name = "Default";
    private String path;

    private static void setParentField(@Nonnull Object self, @Nonnull String fieldName, Object value) {
        try {
            Field f = HytaleWorldGenProvider.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(self, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Reflection for " + fieldName, e);
        }
    }

    @Nonnull
    @Override
    public IWorldGen getGenerator() throws WorldGenLoadException {
        Path worldGenPath = this.path != null
                ? PathUtil.get(this.path)
                : Universe.getWorldGenPath();

        if (!"Default".equals(this.name) || !Files.exists(worldGenPath.resolve("World.json"))) {
            worldGenPath = worldGenPath.resolve(this.name);
        }

        List<String> overlayPaths = buildOverlayPaths(this.name);
        ClassLoader cl = getClass().getClassLoader();

        Path workDir;
        try {
            workDir = Files.createTempDirectory("coalmod_worldgen");
            WorldGenOverlayHelper.prepare(
                    worldGenPath,
                    this.name,
                    workDir,
                    cl,
                    overlayPaths,
                    List.of(COAL_PREFIX));
        } catch (IOException ex) {
            throw new WorldGenLoadException("Failed to prepare worldgen overlay: " + ex.getMessage(), ex);
        }

        try {
            return new ChunkGeneratorJsonLoader(
                    new SeedString<>("ChunkGenerator", new SeedStringResource(PrefabStoreRoot.DEFAULT, workDir)),
                    workDir)
                    .load();
        } catch (Error err) {
            throw new WorldGenLoadException("Failed to load world gen!", err);
        }
    }

    private static List<String> buildOverlayPaths(String generatorName) {
        String base = RESOURCE_PREFIX + generatorName + "/Zones/";
        List<String> out = new ArrayList<>();
        for (String zone : ZONES) {
            String zoneBase = base + zone + "/Cave/Ores/";
            out.add(zoneBase + "Entry.node.json");
            for (String f : COAL_NODE_FILES) {
                out.add(zoneBase + f);
            }
        }
        return out;
    }

    @Nonnull
    @Override
    public String toString() {
        return "ModOresWorldGenProvider{name='" + name + "', path='" + path + "'}";
    }
}
