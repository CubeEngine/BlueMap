package de.bluecolored.bluemap.core.map.renderstate;

import de.bluecolored.bluenbt.NBTName;
import de.bluecolored.bluenbt.NBTPostDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static de.bluecolored.bluemap.core.map.renderstate.MapTileState.SHIFT;

public class TileInfoRegion implements CellStorage.Cell {

    private static final int REGION_LENGTH = 1 << SHIFT;
    private static final int REGION_MASK = REGION_LENGTH - 1;
    private static final int TILES_PER_REGION = REGION_LENGTH * REGION_LENGTH;

    @NBTName("last-render-times")
    private int[] lastRenderTimes;

    @NBTName("tile-states")
    private TileState[] tileStates;

    @Getter
    private transient boolean modified;

    private TileInfoRegion() {}

    @NBTPostDeserialize
    public void init() {
        if (lastRenderTimes == null || lastRenderTimes.length != TILES_PER_REGION)
            lastRenderTimes = new int[TILES_PER_REGION];

        if (tileStates == null || tileStates.length != TILES_PER_REGION) {
            tileStates = new TileState[TILES_PER_REGION];
            Arrays.fill(tileStates, TileState.UNKNOWN);
        }
    }

    public TileInfo get(int x, int z) {
        int index = index(x, z);
        return new TileInfo(
                lastRenderTimes[index],
                tileStates[index]
        );
    }

    public TileInfo set(int x, int z, TileInfo info) {
        int index = index(x, z);

        TileInfo previous = new TileInfo(
                lastRenderTimes[index],
                tileStates[index]
        );

        lastRenderTimes[index] = info.getRenderTime();
        tileStates[index] = Objects.requireNonNull(info.getState());

        if (!previous.equals(info))
            this.modified = true;

        return previous;
    }

    int findLatestRenderTime() {
        if (lastRenderTimes == null) return -1;
        return Arrays.stream(lastRenderTimes)
                .max()
                .orElse(-1);
    }

    void populateSummaryMap(Map<TileState, Integer> map) {
        for (int i = 0; i < TILES_PER_REGION; i++) {
            TileState tileState = tileStates[i];
            map.merge(tileState, 1, Integer::sum);
        }
    }

    private static int index(int x, int z) {
        return (z & REGION_MASK) << SHIFT | (x & REGION_MASK);
    }

    @Data
    @AllArgsConstructor
    public static class TileInfo {

        private int renderTime;
        private TileState state;

    }

    public static TileInfoRegion create() {
        TileInfoRegion region = new TileInfoRegion();
        region.init();
        return region;
    }

}
