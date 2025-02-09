package com.ferreusveritas.dynamictrees.worldgen;

import com.ferreusveritas.dynamictrees.api.worldgen.GroundFinder;
import com.ferreusveritas.dynamictrees.systems.poissondisc.PoissonDisc;
import com.ferreusveritas.dynamictrees.util.LevelContext;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;
import java.util.Optional;

public class CaveRootedTreeFeature extends DynamicTreeFeature {

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ResourceLocation dimensionName = level.getLevel().dimension().location();

        // Do not generate if the current dimension is blacklisted.
        if (BiomeDatabases.isBlacklisted(dimensionName)) {
            return false;
        }

        BlockPos originPos = context.origin();
        ChunkPos chunkPos = level.getChunk(originPos).getPos();
        LevelContext levelContext = LevelContext.create(level);

        PoissonDisc disc = getDisc(levelContext, chunkPos, originPos).orElse(null);
        if (disc == null) {
            return false;
        }

        List<BlockPos> groundPositions = GroundFinder.getGroundFinder(level.getLevel()).findGround(level, originPos, null);
        if (groundPositions.isEmpty()) {
            return false;
        }

        BiomeDatabase.CaveRootedEntry entry = BiomeDatabases.getDefault().getEntry(level.getLevel().getBiome(originPos).value()).getCaveRootedEntry();
        BlockPos groundPos = entry.shouldGenerateOnSurface() ? groundPositions.get(groundPositions.size() - 1)
                : getNextGroundPos(originPos, groundPositions).orElse(null);
        if (groundPos == null || groundPos.getY() - originPos.getY() > entry.getMaxDistToSurface()) {
            return false;
        }

        return this.generateTree(levelContext, entry, disc, originPos, groundPos, SafeChunkBounds.ANY_WG) == GeneratorResult.GENERATED;
    }

    private Optional<PoissonDisc> getDisc(LevelContext levelContext, ChunkPos chunkPos, BlockPos originPos) {
        return DISC_PROVIDER.getPoissonDiscs(levelContext, chunkPos).stream()
                .filter(disc -> disc.x == originPos.getX() && disc.z == originPos.getZ())
                .findFirst();
    }

    private Optional<BlockPos> getNextGroundPos(BlockPos originPos, List<BlockPos> groundPositions) {
        for (BlockPos groundPos: groundPositions) {
            if (groundPos.getY() > originPos.getY()) {
                return Optional.of(groundPos);
            }
        }
        return Optional.empty();
    }

}
