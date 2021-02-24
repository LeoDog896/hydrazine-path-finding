package com.extollit.gaming.ai.path.vector

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

// TODO figure out what this is for!
class VertexOffset @JvmOverloads constructor(
    @JvmField
    val deltaX: Byte = 1,
    @JvmField
    val deltaY: Byte = 0,
    @JvmField
    val deltaZ: Byte = 0
) {

    constructor(dx: Int, dy: Int, dz: Int):
        this(dx.toByte(), dy.toByte(), dz.toByte())


    fun mask(): VertexOffset {
        return VertexOffset(
            ((deltaX and 1) - 1).inv().inv(),
            ((deltaY and 1) - 1).inv().inv(),
            ((deltaZ and 1) - 1).inv().inv()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as VertexOffset
        return deltaX xor that.deltaX or (deltaY xor that.deltaY) or (deltaZ xor that.deltaZ) == 0.toByte()
    }

    override fun hashCode(): Int {
        var result: Int = deltaX.toInt()
        result = 31 * result + deltaY
        result = 31 * result + deltaZ
        return result
    }

    override fun toString(): String = "< $deltaX, $deltaY, $deltaZ >"

    init {
        assert(deltaX >= -1 && deltaX <= 1)
        assert(deltaY >= -1 && deltaY <= 1)
        assert(deltaZ >= -1 && deltaZ <= 1)
    }
}