package com.extollit.gaming.ai.path.vector

import com.extollit.linalg.mutable.Vec3i
import java.text.MessageFormat
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

// TODO figure out what this is for!
class VertexOffset @JvmOverloads constructor(
    @JvmField
    val dx: Byte = 1,
    @JvmField
    val dy: Byte = 0,
    @JvmField
    val dz: Byte = 0
) {

    constructor(dx: Int, dy: Int, dz: Int):
        this(dx.toByte(), dy.toByte(), dz.toByte())


    fun mask(): VertexOffset {
        return VertexOffset(
            ((dx and 1) - 1).inv().inv(),
            ((dy and 1) - 1).inv().inv(),
            ((dz and 1) - 1).inv().inv()
        )
    }

    fun pcross(other: VertexOffset): VertexOffset {
        val dx = dy * other.dz - dz * other.dy
        val dy = dz * other.dx - this.dx * other.dz
        val dz = this.dx * other.dy - this.dy * other.dx
        return VertexOffset(dx, dy, dz)
    }

    fun orthog(): VertexOffset {
        return VertexOffset(
            dy xor dz,
            dx xor dz,
            dy xor dx
        )
    }

    fun offset(vec: Vec3i): Vec3i {
        vec.x += dx.toInt()
        vec.y += dy.toInt()
        vec.z += dz.toInt()
        return vec
    }

    fun sq(): VertexOffset {
        return VertexOffset(dx * dx, dy * dy, dz * dz)
    }

    fun mul(offset: VertexOffset): VertexOffset {
        return VertexOffset(
            dx * offset.dx,
            dy * offset.dy,
            dz * offset.dz
        )
    }

    fun sub(offset: VertexOffset): VertexOffset {
        return VertexOffset(
            dx - offset.dx,
            dy - offset.dy,
            dz - offset.dz
        )
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as VertexOffset
        return dx xor that.dx or (dy xor that.dy) or (dz xor that.dz) == 0.toByte()
    }

    override fun hashCode(): Int {
        var result: Int = dx.toInt()
        result = 31 * result + dy
        result = 31 * result + dz
        return result
    }

    override fun toString(): String = MessageFormat.format("< {0}, {1}, {2} >", dx, dy, dz)

    init {
        assert(dx >= -1 && dx <= 1)
        assert(dy >= -1 && dy <= 1)
        assert(dz >= -1 && dz <= 1)
    }
}