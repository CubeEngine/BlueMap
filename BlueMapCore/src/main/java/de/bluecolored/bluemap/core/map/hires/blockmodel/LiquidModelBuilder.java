/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.map.hires.blockmodel;

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.map.hires.BlockModelView;
import de.bluecolored.bluemap.core.map.hires.HiresTileModel;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resourcepack.BlockColorCalculatorFactory;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resourcepack.blockmodel.TransformedBlockModelResource;
import de.bluecolored.bluemap.core.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM3f;
import de.bluecolored.bluemap.core.util.math.VectorM2f;
import de.bluecolored.bluemap.core.util.math.VectorM3f;
import de.bluecolored.bluemap.core.world.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.ExtendedBlock;

/**
 * A model builder for all liquid blocks
 */
public class LiquidModelBuilder {
    private static final float BLOCK_SCALE = 1f / 16f;
    private static final MatrixM3f FLOWING_UV_SCALE = new MatrixM3f()
            .identity()
            .translate(-0.5f, -0.5f)
            .scale(0.5f, 0.5f, 1)
            .translate(0.5f, 0.5f);

    private final BlockColorCalculatorFactory.BlockColorCalculator blockColorCalculator;
    private final RenderSettings renderSettings;

    private final VectorM3f[] corners;
    private final VectorM2f[] uvs = new VectorM2f[4];

    private BlockNeighborhood<?> block;
    private BlockState blockState;
    private TransformedBlockModelResource blockModelResource;
    private BlockModelView blockModel;
    private Color blockColor;

    public LiquidModelBuilder(ResourcePack resourcePack, RenderSettings renderSettings) {
        this.blockColorCalculator = resourcePack.getBlockColorCalculatorFactory().createCalculator();
        this.renderSettings = renderSettings;

        corners = new VectorM3f[]{
                new VectorM3f( 0, 0, 0 ),
                new VectorM3f( 0, 0, 16 ),
                new VectorM3f( 16, 0, 0 ),
                new VectorM3f( 16, 0, 16 ),
                new VectorM3f( 0, 16, 0 ),
                new VectorM3f( 0, 16, 16 ),
                new VectorM3f( 16, 16, 0 ),
                new VectorM3f( 16, 16, 16 ),
        };

        for (int i = 0; i < uvs.length; i++) uvs[i] = new VectorM2f(0, 0);
    }

    public void build(BlockNeighborhood<?> block, BlockState blockState, TransformedBlockModelResource bmr, BlockModelView blockModel, Color color) {
        this.block = block;
        this.blockState = blockState;
        this.blockModelResource = bmr;
        this.blockModel = blockModel;
        this.blockColor = color;

        build();
    }

    private final Color tintcolor = new Color();
    private void build() {
        // filter out blocks that are in a "cave" that should not be rendered
        if (
                this.block.getY() < renderSettings.getRemoveCavesBelowY() &&
                (renderSettings.isCaveDetectionUsesBlockLight() ? block.getBlockLightLevel() : block.getSunLightLevel()) == 0f
        ) return;

        int level = getLiquidLevel(blockState);

        if (level < 8 && !(level == 0 && isSameLiquid(block.getNeighborBlock(0, 1, 0)))){
            corners[4].y = getLiquidCornerHeight(-1,  -1);
            corners[5].y = getLiquidCornerHeight(-1,  0);
            corners[6].y = getLiquidCornerHeight(0,  -1);
            corners[7].y = getLiquidCornerHeight(0,  0);
        } else {
            corners[4].y = 16f;
            corners[5].y = 16f;
            corners[6].y = 16f;
            corners[7].y = 16f;
        }

        Texture stillTexture = blockModelResource.getModel().getTexture("still");
        Texture flowTexture = blockModelResource.getModel().getTexture("flow");

        int stillTextureId = stillTexture.getId();
        int flowTextureId = flowTexture.getId();

        tintcolor.set(1f, 1f, 1f, 1f, true);
        if (blockState.isWater()) {
            blockColorCalculator.getWaterAverageColor(block, tintcolor);
        }

        int modelStart = blockModel.getStart();

        VectorM3f[] c = corners;
        createElementFace(Direction.DOWN, c[0], c[2], c[3], c[1], tintcolor, stillTextureId, flowTextureId);
        boolean upFaceRendered =
                createElementFace(Direction.UP, c[5], c[7], c[6], c[4], tintcolor, stillTextureId, flowTextureId);
        createElementFace(Direction.NORTH, c[2], c[0], c[4], c[6], tintcolor, stillTextureId, flowTextureId);
        createElementFace(Direction.SOUTH, c[1], c[3], c[7], c[5], tintcolor, stillTextureId, flowTextureId);
        createElementFace(Direction.WEST, c[0], c[1], c[5], c[4], tintcolor, stillTextureId, flowTextureId);
        createElementFace(Direction.EAST, c[3], c[2], c[6], c[7], tintcolor, stillTextureId, flowTextureId);

        blockModel.initialize(modelStart);

        //scale down
        blockModel.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);

        //calculate mapcolor
        if (upFaceRendered) {
            blockColor.set(stillTexture.getColorPremultiplied());
            blockColor.multiply(tintcolor);

            // apply light
            float combinedLight = Math.max(block.getSunLightLevel() / 15f, block.getBlockLightLevel() / 15f);
            combinedLight = (renderSettings.getAmbientLight() + combinedLight) / (renderSettings.getAmbientLight() + 1f);
            blockColor.r *= combinedLight;
            blockColor.g *= combinedLight;
            blockColor.b *= combinedLight;
        } else {
            blockColor.set(0, 0, 0, 0, true);
        }
    }

    private float getLiquidCornerHeight(int x, int z){
        int ix, iz;

        for (ix = x; ix <= x+1; ix++){
            for (iz = z; iz<= z+1; iz++){
                if (isSameLiquid(block.getNeighborBlock(ix, 1, iz))){
                    return 16f;
                }
            }
        }

        float sumHeight = 0f;
        int count = 0;
        ExtendedBlock<?> neighbor;
        BlockState neighborBlockState;

        for (ix = x; ix <= x+1; ix++){
            for (iz = z; iz<= z+1; iz++){
                neighbor = block.getNeighborBlock(ix, 0, iz);
                neighborBlockState = neighbor.getBlockState();
                if (isSameLiquid(neighbor)){
                    if (getLiquidLevel(neighborBlockState) == 0) return 14f;

                    sumHeight += getLiquidBaseHeight(neighborBlockState);
                    count++;
                }

                else if (!isLiquidBlockingBlock(neighborBlockState)){
                    count++;
                }
            }
        }

        //should both never happen
        if (sumHeight == 0) return 3f;
        if (count == 0) return 3f;

        return sumHeight / count;
    }

    private boolean isLiquidBlockingBlock(BlockState blockState){
        return !blockState.equals(BlockState.AIR);
    }

    private boolean isSameLiquid(ExtendedBlock<?> block){
        if (block.getBlockState().getFullId().equals(this.blockState.getFullId())) return true;
        return this.blockState.isWater() && (block.getBlockState().isWaterlogged() || block.getProperties().isAlwaysWaterlogged());
    }

    private float getLiquidBaseHeight(BlockState block){
        int level = getLiquidLevel(block);
        return level >= 8 ? 16f : 14f - level * 1.9f;
    }

    private int getLiquidLevel(BlockState block){
        String levelString = block.getProperties().get("level");
        return levelString != null ? Integer.parseInt(levelString) : 0;
    }

    private final MatrixM3f uvTransform = new MatrixM3f();
    private boolean createElementFace(Direction faceDir, VectorM3f c0, VectorM3f c1, VectorM3f c2, VectorM3f c3, Color color, int stillTextureId, int flowTextureId) {
        Vector3i faceDirVector = faceDir.toVector();

        //face culling
        ExtendedBlock<?> bl = block.getNeighborBlock(
                faceDirVector.getX(),
                faceDirVector.getY(),
                faceDirVector.getZ()
        );

        if (isSameLiquid(bl) || (faceDir != Direction.UP && bl.getProperties().isCulling())) return false;

        // initialize the faces
        blockModel.initialize();
        blockModel.add(2);

        HiresTileModel tileModel = blockModel.getHiresTile();
        int face1 = blockModel.getStart();
        int face2 = face1 + 1;

        // ####### positions
        tileModel.setPositions(face1,
                c0.x, c0.y, c0.z,
                c1.x, c1.y, c1.z,
                c2.x, c2.y, c2.z
        );
        tileModel.setPositions(face2,
                c0.x, c0.y, c0.z,
                c2.x, c2.y, c2.z,
                c3.x, c3.y, c3.z
        );

        //UV
        uvs[0].set(0, 1);
        uvs[1].set(1, 1);
        uvs[2].set(1, 0);
        uvs[3].set(0, 0);

        // still/flow ?
        boolean flow = false;
        if (faceDir == Direction.UP) {
            int flowAngle = getFlowingAngle();
            if (flowAngle != -1) {
                flow = true;
                uvTransform
                        .identity()
                        .translate(-0.5f, -0.5f)
                        .scale(0.5f, 0.5f, 1)
                        .rotate(-flowAngle, 0, 0, 1)
                        .translate(0.5f, 0.5f);

                uvs[0].transform(uvTransform);
                uvs[1].transform(uvTransform);
                uvs[2].transform(uvTransform);
                uvs[3].transform(uvTransform);
            }
        } else if (faceDir != Direction.DOWN) {
            flow = true;

            uvs[0].transform(FLOWING_UV_SCALE);
            uvs[1].transform(FLOWING_UV_SCALE);
            uvs[2].transform(FLOWING_UV_SCALE);
            uvs[3].transform(FLOWING_UV_SCALE);
        }

        tileModel.setUvs(face1,
                uvs[0].x, uvs[0].y,
                uvs[1].x, uvs[1].y,
                uvs[2].x, uvs[2].y
        );

        tileModel.setUvs(face2,
                uvs[0].x, uvs[0].y,
                uvs[2].x, uvs[2].y,
                uvs[3].x, uvs[3].y
        );

        // texture index
        tileModel.setMaterialIndex(face1, flow ? flowTextureId : stillTextureId);
        tileModel.setMaterialIndex(face2, flow ? flowTextureId : stillTextureId);

        // color
        tileModel.setColor(face1, color.r, color.g, color.b);
        tileModel.setColor(face2, color.r, color.g, color.b);

        //ao
        tileModel.setAOs(face1, 1, 1, 1);
        tileModel.setAOs(face2, 1, 1, 1);

        // light
        int blockLight, sunLight;
        if (faceDir == Direction.UP) {
            blockLight = block.getBlockLightLevel();
            sunLight = block.getSunLightLevel();
        } else {
            blockLight = bl.getBlockLightLevel();
            sunLight = bl.getSunLightLevel();
        }

        tileModel.setBlocklight(face1, blockLight);
        tileModel.setBlocklight(face2, blockLight);

        tileModel.setSunlight(face1, sunLight);
        tileModel.setSunlight(face2, sunLight);

        return true;
    }

    private final VectorM2f flowingVector = new VectorM2f(0, 0);
    private int getFlowingAngle() {
        float own = getLiquidBaseHeight(blockState) * BLOCK_SCALE;
        if (own > 0.8) return -1;

        flowingVector.set(0, 0);

        flowingVector.x += compareLiquidHeights(own, -1, 0);
        flowingVector.x -= compareLiquidHeights(own,  1, 0);

        flowingVector.y -= compareLiquidHeights(own, 0, -1);
        flowingVector.y += compareLiquidHeights(own, 0,  1);

        if (flowingVector.x == 0 && flowingVector.y == 0) return -1; // not flowing

        int angle = (int) (flowingVector.angleTo(0, -1) * TrigMath.RAD_TO_DEG);
        return flowingVector.x < 0 ? angle : -angle;
    }

    private float compareLiquidHeights(float ownHeight, int dx, int dz) {
        ExtendedBlock<?> neighbor = block.getNeighborBlock(dx, 0,  dz);
        if (neighbor.getBlockState().isAir()) return 0;
        if (!isSameLiquid(neighbor)) return 0;

        float otherHeight = getLiquidBaseHeight(neighbor.getBlockState()) * BLOCK_SCALE;
        return otherHeight - ownHeight;
    }

}