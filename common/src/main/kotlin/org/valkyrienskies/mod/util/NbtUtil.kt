package org.valkyrienskies.mod.util

import net.minecraft.nbt.CompoundTag
import org.joml.Vector3d
import org.joml.Vector3dc

fun CompoundTag.putVector3d(prefix: String, vector3d: Vector3dc) =
    with(vector3d) {
        putDouble(prefix + "x", x())
        putDouble(prefix + "y", y())
        putDouble(prefix + "z", z())
    }

fun CompoundTag.getVector3d(prefix: String): Vector3d? {
    return if (
        !prefix.contains(prefix + "x") ||
        !prefix.contains(prefix + "y") ||
        !prefix.contains(prefix + "z")
    ) {
        null
    } else {
        Vector3d(
            this.getDouble(prefix + "x"),
            this.getDouble(prefix + "y"),
            this.getDouble(prefix + "z")
        )
    }
}

/**
 * Modifies all the positions in the CompoundTag that start with the given prefix.
 * @param tagKey The key of the tag to modify.
 * @param modifier The function to modify the position with.
 * @param filter A function that returns true if the key should be modified. first is the key, second is the prefix of position.
 */
fun CompoundTag.modifyPositions(tagKey: String, modifier: (CoordVal, Int) -> Int, filter: (String, String) -> Boolean) {
    for (key in this.allKeys) {
        val lowercase = key.lowercase()
        val childTag = this.get(key)
        if (childTag is CompoundTag) {
            childTag.modifyPositions(key, modifier, filter)
            continue
        }
        if (CoordVal.values().any { lowercase.endsWith(it.suffix) }) {
            if (filter(tagKey, key.substring(0, key.length - 1))) {
                val coordVal = CoordVal.values().first { lowercase.endsWith(it.suffix) }
                val value = this.getInt(key)
                this.putInt(key, modifier(coordVal, value))
            }
        }
    }
}

enum class CoordVal(val suffix: String) {
    X("x"),
    Y("y"),
    Z("z")
}
