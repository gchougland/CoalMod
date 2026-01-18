package com.hexvane.coalmod.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prepares a working worldgen directory by copying a base path and overlaying
 * resources from a classloader (e.g. mod JAR). Supports merging Entry.node.json
 * Children arrays so mod ores can be added without removing base ores.
 *
 * <p>Designed to be extracted into a library mod for reuse by other ore/worldgen mods.
 */
public final class WorldGenOverlayHelper {

    private static final HytaleLogger LOG = HytaleLogger.forEnclosingClass();
    private static final String ENTRY_NODE_FILE = "Cave/Ores/Entry.node.json";

    /**
     * Copies the base worldgen folder into workDir, then overlays the given
     * resources from the classloader. For {@value #ENTRY_NODE_FILE}, children
     * from the overlay whose Node[0] starts with any of {@code mergeEntryPrefixes}
     * are merged into the base; other overlay files overwrite.
     *
     * @param basePath            base worldgen folder (e.g. .../Default) to copy
     * @param generatorName       generator name used in resource paths (e.g. "Default")
     * @param workDir             directory to create (will contain World.json, Zones/, etc.)
     * @param resourceClassLoader classloader to load overlay resources (e.g. plugin's)
     * @param overlayPaths        full resource paths (e.g. "Server/World/Default/Zones/.../Entry.node.json")
     * @param mergeEntryPrefixes  for Entry.node.json, add overlay children whose Node[0] starts with any of these (e.g. "Ores.Coal.")
     * @return workDir (for convenience)
     */
    @Nonnull
    public static Path prepare(
            @Nonnull Path basePath,
            @Nonnull String generatorName,
            @Nonnull Path workDir,
            @Nonnull ClassLoader resourceClassLoader,
            @Nonnull List<String> overlayPaths,
            @Nonnull List<String> mergeEntryPrefixes) throws IOException {

        String prefixToStrip = "Server/World/" + generatorName + "/";

        LOG.atInfo().log("WorldGen overlay: basePath=%s workDir=%s overlayPaths=%d", basePath, workDir, overlayPaths.size());

        // 1) Copy base into workDir
        copyRecursive(basePath, workDir);

        // 1b) Strip OverrideDataFolder from World.json so loading stays in workDir
        Path worldJson = workDir.resolve("World.json");
        if (Files.exists(worldJson)) {
            try {
                JsonObject w;
                try (var r = Files.newBufferedReader(worldJson, StandardCharsets.UTF_8)) {
                    w = JsonParser.parseReader(r).getAsJsonObject();
                }
                if (w.has("OverrideDataFolder")) {
                    w.remove("OverrideDataFolder");
                    Files.writeString(worldJson, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(w), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                LOG.atWarning().withCause(e).log("Could not strip OverrideDataFolder from World.json");
            }
        }

        // 2) Overlay each resource
        int merged = 0, copied = 0, skipped = 0;
        for (String path : overlayPaths) {
            if (!path.startsWith(prefixToStrip)) { skipped++; continue; }

            String rel = path.substring(prefixToStrip.length());
            Path dest = workDir.resolve(rel);

            if (rel.endsWith(ENTRY_NODE_FILE) && !mergeEntryPrefixes.isEmpty()) {
                if (mergeEntryNode(dest, path, resourceClassLoader, mergeEntryPrefixes)) merged++;
            } else {
                if (copyResource(path, dest, resourceClassLoader)) copied++;
            }
        }
        LOG.atInfo().log("WorldGen overlay: merged=%d copied=%d skipped=%d", merged, copied, skipped);

        // 3) Optional: verify a sample zone
        Path zonesDir = workDir.resolve("Zones");
        Path sampleEntry = zonesDir.resolve("Zone1_Tier3").resolve("Cave").resolve("Ores").resolve("Entry.node.json");
        Path sampleCoal = zonesDir.resolve("Zone1_Tier3").resolve("Cave").resolve("Ores").resolve("Coal").resolve("CoalSpread.node.json");
        Path sampleVein = zonesDir.resolve("Zone1_Tier3").resolve("Cave").resolve("Ores").resolve("Coal").resolve("CoalVeinSmall.node.json");
        if (Files.exists(sampleEntry)) {
            String entryContents = Files.readString(sampleEntry, StandardCharsets.UTF_8);
            boolean hasCoal = entryContents.contains("Ores.Coal.CoalSpread");
            boolean coalFileExists = Files.exists(sampleCoal);
            boolean coalVeinExists = Files.exists(sampleVein);
            boolean fillingOk = coalVeinExists && Files.readString(sampleVein, StandardCharsets.UTF_8).contains("Ore_Coal_Stone");
            if (hasCoal && coalFileExists && fillingOk) {
                LOG.atInfo().log("WorldGen overlay verify: Zone1_Tier3 Entry has Ores.Coal.CoalSpread, CoalSpread+CoalVeinSmall present, Filling Ore_Coal_Stone ok");
            } else {
                LOG.atWarning().log("WorldGen overlay verify: Zone1_Tier3 hasCoal=%s CoalSpread=%s CoalVeinSmall=%s fillingOk=%s (inspect workDir=%s)", hasCoal, coalFileExists, coalVeinExists, fillingOk, workDir);
            }
        } else {
            LOG.atWarning().log("WorldGen overlay verify: Zones/Zone1_Tier3/.../Entry.node.json missing (base copy may lack Zones). workDir=%s zonesExists=%s", workDir, Files.isDirectory(zonesDir));
        }

        return workDir;
    }

    /**
     * Copies src into dest. Uses relativize().toString() when resolving against dest
     * to avoid ProviderMismatchException when src is on a different FileSystem (e.g.
     * zip/jar) than dest (default filesystem).
     */
    private static void copyRecursive(Path src, Path dest) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String rel = src.relativize(dir).toString();
                if (rel.isEmpty()) {
                    Files.createDirectories(dest);
                    return FileVisitResult.CONTINUE;
                }
                Path target = dest.resolve(rel.replace('/', dest.getFileSystem().getSeparator().charAt(0)));
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String rel = src.relativize(file).toString().replace('/', dest.getFileSystem().getSeparator().charAt(0));
                Path target = dest.resolve(rel);
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean mergeEntryNode(Path dest, String resourcePath, ClassLoader cl, List<String> mergeEntryPrefixes) throws IOException {
        JsonObject base;
        if (Files.exists(dest)) {
            try (var r = Files.newBufferedReader(dest, StandardCharsets.UTF_8)) {
                base = JsonParser.parseReader(r).getAsJsonObject();
            }
        } else {
            base = new JsonObject();
            base.add("Name", new JsonPrimitive("Distribution Wheel"));
            base.add("Children", new JsonArray());
            base.add("Type", new JsonPrimitive("EMPTY_LINE"));
            JsonArray len = new JsonArray();
            len.add(0);
            base.add("Length", len);
            base.add("YawAdd", new JsonPrimitive(0));
        }

        JsonArray baseChildren = base.getAsJsonArray("Children");
        if (baseChildren == null) {
            baseChildren = new JsonArray();
            base.add("Children", baseChildren);
        }

        Set<String> existing = existingNodeNames(baseChildren);
        List<JsonObject> toPrepend = new ArrayList<>();

        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in == null) {
                LOG.atWarning().log("Overlay resource not found: %s", resourcePath);
                return false;
            }
            JsonObject overlay = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray overlayChildren = overlay.getAsJsonArray("Children");
            if (overlayChildren == null) return false;

            for (JsonElement e : overlayChildren) {
                if (!e.isJsonObject()) continue;
                JsonObject child = e.getAsJsonObject();
                if (!child.has("Node") || !child.get("Node").isJsonArray()) continue;
                JsonArray nodeArr = child.getAsJsonArray("Node");
                if (nodeArr.isEmpty()) continue;
                String nodeName = nodeArr.get(0).getAsString();
                boolean fromOverlay = mergeEntryPrefixes.stream().anyMatch(nodeName::startsWith);
                if (fromOverlay && !existing.contains(nodeName)) {
                    toPrepend.add(child.deepCopy());
                    existing.add(nodeName);
                }
            }
        }

        int added = toPrepend.size();
        if (added > 0) {
            JsonArray newChildren = new JsonArray();
            toPrepend.forEach(newChildren::add);
            for (JsonElement e : baseChildren) {
                newChildren.add(e);
            }
            base.add("Children", newChildren);
        }

        Files.createDirectories(dest.getParent());
        Files.writeString(dest, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(base), StandardCharsets.UTF_8);
        if (added > 0) LOG.atInfo().log("Merge Entry %s: added %d coal children", dest.getFileName(), added);
        return added > 0;
    }

    private static Set<String> existingNodeNames(JsonArray children) {
        return java.util.stream.StreamSupport.stream(children.spliterator(), false)
                .filter(JsonElement::isJsonObject)
                .map(e -> e.getAsJsonObject())
                .filter(o -> o.has("Node") && o.get("Node").isJsonArray())
                .map(o -> o.getAsJsonArray("Node"))
                .filter(a -> !a.isEmpty())
                .map(a -> a.get(0).getAsString())
                .collect(Collectors.toSet());
    }

    private static boolean copyResource(String resourcePath, Path dest, ClassLoader cl) throws IOException {
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in == null) {
                LOG.atWarning().log("Overlay resource not found: %s", resourcePath);
                return false;
            }
            Files.createDirectories(dest.getParent());
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
    }
}
