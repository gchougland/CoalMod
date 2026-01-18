package com.hexvane.coalmod;

import com.hypixel.hytale.codec.lookup.Priority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.hypixel.hytale.server.worldgen.HytaleWorldGenProvider;
import com.hexvane.coalmod.worldgen.ModOresWorldGenProvider;

public class CoalMod extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public CoalMod(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Replace the built-in "Hytale" worldgen provider with our overlay provider so
        // mod ore resources (e.g. coal) are merged into the base worldgen. Load-after
        // WorldGenPlugin is best; if we run first, the built-in may overwrite us.
        IWorldGenProvider.CODEC.remove(HytaleWorldGenProvider.class);
        IWorldGenProvider.CODEC.register(
                Priority.DEFAULT.before(1),
                "Hytale",
                ModOresWorldGenProvider.class,
                ModOresWorldGenProvider.CODEC);
        LOGGER.atInfo().log("Registered ModOresWorldGenProvider for Hytale worldgen");
    }
}
