package org.valkyrienskies.mod.util

import net.minecraft.core.BlockPos
import net.minecraft.world.Clearable
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.Rotation.NONE
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.valkyrienskies.core.api.ships.ServerShip

private val AIR = Blocks.AIR.defaultBlockState()

/**
 * Relocate block
 *
 * @param fromChunk
 * @param from coordinate (can be local or global coord)
 * @param toChunk
 * @param to coordinate (can be local or global coord)
 * @param toShip should be set when you're relocating to a ship
 * @param rotation Rotation.NONE is no change in direction, Rotation.CLOCKWISE_90 is 90 degrees clockwise, etc.
 */
fun relocateBlock(
    fromChunk: LevelChunk, from: BlockPos, toChunk: LevelChunk, to: BlockPos, doUpdate: Boolean, toShip: ServerShip?,
    rotation: Rotation = NONE
) {
    var state = fromChunk.getBlockState(from)
    val entity = fromChunk.getBlockEntity(from)

    val tag = entity?.let {
        val tag = it.saveWithFullMetadata()
        tag.putInt("x", to.x)
        tag.putInt("y", to.y)
        tag.putInt("z", to.z)

        // so that it won't drop its contents
        if (it is Clearable) {
            it.clearContent()
        }

        tag
    }

    state = state.rotate(rotation)

    val level = toChunk.level

    fromChunk.setBlockState(from, AIR, false)
    toChunk.setBlockState(to, state, false)

    if (doUpdate) {
        updateBlock(level, from, to, state)
    }

    tag?.let {
        val be = level.getBlockEntity(to)!!

        be.load(it)
    }
}

/**
 * Relocate block
 *
 * @param level
 * @param from coordinate (can be local or global coord)
 * @param toChunk
 * @param to coordinate (can be local or global coord)
 * @param toShip should be set when you're relocating to a ship
 * @param rotation Rotation.NONE is no change in direction, Rotation.CLOCKWISE_90 is 90 degrees clockwise, etc.
 */
fun relocateBlocks(
    level: Level, blocks: Map<BlockPos, BlockPos>,
) {
    val blockPosToClear = arrayListOf<BlockEntity>()
    for (block in blocks) {
        val fromChunk = level.getChunk(block.key)
        val toChunk = level.getChunk(block.value)
        var state = fromChunk.getBlockState(block.key)
        val entity = fromChunk.getBlockEntity(block.key)
        val tag = entity?.let {
            val tag = it.saveWithFullMetadata()
            tag.putInt("x", block.value.x)
            tag.putInt("y", block.value.y)
            tag.putInt("z", block.value.z)

            tag
        }
        toChunk.setBlockState(block.value, state, false)

        tag?.let {
            val be = level.getBlockEntity(block.value)!!

            be.load(it)
        }
        entity?.let {
            blockPosToClear.add(it)
        }
        if (entity == null) {
            fromChunk.setBlockState(block.key, AIR, false)
        }
    }
    for (clear in blockPosToClear) {
        if (clear is Clearable) {
            clear.clearContent()
        }
        level.getChunk(clear.blockPos).setBlockState(clear.blockPos, AIR, false)
    }
}

/**
 * Update block after relocate
 *
 * @param level
 * @param fromPos old position coordinate
 * @param toPos new position coordinate
 * @param toState new blockstate at toPos
 */
fun updateBlock(level: Level, fromPos: BlockPos, toPos: BlockPos, toState: BlockState) {

    // 75 = flag 1 (block update) & flag 2 (send to clients) + flag 8 (force rerenders)
    val flags = 11

    //updateNeighbourShapes recurses through nearby blocks, recursionLeft is the limit
    val recursionLeft = 511

    level.setBlocksDirty(fromPos, toState, AIR)
    level.sendBlockUpdated(fromPos, toState, AIR, flags)
    level.blockUpdated(fromPos, AIR.block)
    // This handles the update for neighboring blocks in worldspace
    AIR.updateIndirectNeighbourShapes(level, fromPos, flags, recursionLeft - 1)
    AIR.updateNeighbourShapes(level, fromPos, flags, recursionLeft)
    AIR.updateIndirectNeighbourShapes(level, fromPos, flags, recursionLeft)
    //This updates lighting for blocks in worldspace
    level.chunkSource.lightEngine.checkBlock(fromPos)

    level.setBlocksDirty(toPos, AIR, toState)
    level.sendBlockUpdated(toPos, AIR, toState, flags)
    level.blockUpdated(toPos, toState.block)
    if (!level.isClientSide && toState.hasAnalogOutputSignal()) {
        level.updateNeighbourForOutputSignal(toPos, toState.block)
    }
    //This updates lighting for blocks in shipspace
    level.chunkSource.lightEngine.checkBlock(toPos)
}

/**
 * Relocate block
 *
 * @param from coordinate (can be local or global coord)
 * @param to coordinate (can be local or global coord)
 * @param doUpdate update blocks after moving
 * @param toShip should be set when you're relocating to a ship
 * @param rotation Rotation.NONE is no change in direction, Rotation.CLOCKWISE_90 is 90 degrees clockwise, etc.
 */
fun Level.relocateBlock(from: BlockPos, to: BlockPos, doUpdate: Boolean, toShip: ServerShip?, rotation: Rotation) =
    relocateBlock(getChunkAt(from), from, getChunkAt(to), to, doUpdate, toShip, rotation)
