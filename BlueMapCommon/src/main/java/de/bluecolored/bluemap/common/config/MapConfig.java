package de.bluecolored.bluemap.common.config;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.map.MapSettings;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.nio.file.Path;
import java.util.Optional;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class MapConfig implements MapSettings {

    private String name = null;

    private Path world = null;

    private int sorting = 0;

    private Vector2i startPos = null;

    private String skyColor = "#7dabff";

    private float ambientLight = 0;

    private int worldSkyLight = 15;

    private int removeCavesBelowY = 55;

    private boolean caveDetectionUsesBlockLight = false;

    private int minX = Integer.MIN_VALUE;
    private int maxX = Integer.MAX_VALUE;
    private int minZ = Integer.MIN_VALUE;
    private int maxZ = Integer.MAX_VALUE;
    private int minY = Integer.MIN_VALUE;
    private int maxY = Integer.MAX_VALUE;

    private transient Vector3i min = null;
    private transient Vector3i max = null;

    private boolean renderEdges = true;

    private boolean saveHiresLayer = true;

    private String storage = "file";

    private boolean ignoreMissingLightData = false;

    private ConfigurationNode markerSets = null;

    // hidden config fields
    private int hiresTileSize = 32;
    private int lowresTileSize = 500;
    private int lodCount = 3;
    private int lodFactor = 5;

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Path getWorld() {
        return world;
    }

    public int getSorting() {
        return sorting;
    }

    public Optional<Vector2i> getStartPos() {
        return Optional.ofNullable(startPos);
    }

    public String getSkyColor() {
        return skyColor;
    }

    public float getAmbientLight() {
        return ambientLight;
    }

    public int getWorldSkyLight() {
        return worldSkyLight;
    }

    public int getRemoveCavesBelowY() {
        return removeCavesBelowY;
    }

    public boolean isCaveDetectionUsesBlockLight() {
        return caveDetectionUsesBlockLight;
    }

    public Vector3i getMinPos() {
        if (min == null) min = new Vector3i(minX, minY, minZ);
        return min;
    }

    public Vector3i getMaxPos() {
        if (max == null) max = new Vector3i(maxX, maxY, maxZ);
        return max;
    }

    public boolean isRenderEdges() {
        return renderEdges;
    }

    @Override
    public boolean isSaveHiresLayer() {
        return saveHiresLayer;
    }

    public String getStorage() {
        return storage;
    }

    public boolean isIgnoreMissingLightData() {
        return ignoreMissingLightData;
    }

    @Nullable
    public ConfigurationNode getMarkerSets() {
        return markerSets;
    }

    public int getHiresTileSize() {
        return hiresTileSize;
    }

    public int getLowresTileSize() {
        return lowresTileSize;
    }

    public int getLodCount() {
        return lodCount;
    }

    public int getLodFactor() {
        return lodFactor;
    }

}
