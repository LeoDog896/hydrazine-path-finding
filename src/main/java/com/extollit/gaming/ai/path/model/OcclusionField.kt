package com.extollit.gaming.ai.path.model

import com.extollit.linalg.immutable.VertexOffset
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

open class OcclusionField : IOcclusionProvider {
    enum class AreaInit {
        north(0, -1), south(0, +1), west(-1, 0), east(+1, 0), northEast(+1, -1), northWest(-1, -1), southEast(
            +1,
            +1
        ),
        southWest(-1, +1), up(1), down(-1);

        @kotlin.jvm.JvmField
        val offset: VertexOffset
        val mask = (1 shl ordinal).toShort()

        constructor(dx: Int, dz: Int) {
            offset = VertexOffset(dx, 0, dz)
        }

        constructor(dy: Int) {
            offset = VertexOffset(0, dy, 0)
        }

        fun `in`(flags: Short): Boolean {
            return flags and mask != 0.toShort()
        }

        fun to(flags: Short): Short {
            return (flags or mask)
        }

        companion object {
            @kotlin.jvm.JvmStatic
            fun given(dx: Int, dy: Int, dz: Int): AreaInit? {
                if (dx == -1 && dz == -1) return northWest else if (dx == 0 && dz == -1) return north else if (dx == +1 && dz == -1) return northEast else if (dx == +1 && dz == 0) return east else if (dx == +1 && dz == +1) return southEast else if (dx == 0 && dz == +1) return south else if (dx == -1 && dz == +1) return southWest else if (dx == -1 && dz == 0) return west else if (dx == 0 && dz == 0) if (dy == -1) return down else if (dy == +1) return up
                return null
            }
        }
    }

    private var words: LongArray? = null
    private var singleton: Byte = 0
    private var areaInit: Short = 0

    fun areaInitFull(): Boolean = areaInit == FULLY_AREA_INIT

    fun areaInitAt(direction: AreaInit): Boolean = direction.`in`(areaInit)

    // TODO document what is this?
    fun loadFrom(columnarSpace: IColumnarSpace, cx: Int, cy: Int, cz: Int) {
        singleton = 0
        words = LongArray(DIMENSION_SQUARE_SIZE * DIMENSION_SIZE * ELEMENT_LENGTH / WORD_LENGTH)
        var compress = true
        var lastFlags = singleton
        val x0 = cx shl DIMENSION_ORDER.toInt()
        val y0 = cy shl DIMENSION_ORDER.toInt()
        val yN = y0 + DIMENSION_SIZE
        val z0 = cz shl DIMENSION_ORDER.toInt()
        val words = words
        val yNi = yN - 1
        var y = yNi
        var i = LAST_INDEX
        while (y >= y0) {
            for (z in DIMENSION_EXTENT downTo 0) {
                var x = DIMENSION_SIZE - ELEMENTS_PER_WORD
                while (x >= 0) {
                    var word: Long = 0
                    for (b in WORD_LAST_OFFSET downTo 0) {
                        val xx = x + b
                        val blockDescription = columnarSpace.blockAt(xx, y, z)
                        val flags = flagsFor(columnarSpace, x0 + xx, y, z0 + z, blockDescription)
                        compress = compress and (lastFlags == flags || i == LAST_INDEX && b == WORD_LAST_OFFSET.toInt())
                        lastFlags = flags
                        word = word shl (1 shl ELEMENT_LENGTH_SHL.toInt())
                        word = word or flags.toLong()
                        if (blockDescription!!.isFenceLike && y < yNi) {
                            val indexUp = i + (DIMENSION_SQUARE_SIZE shr COORDINATE_TO_INDEX_SHR.toInt())
                            words!![indexUp] = modifyWord(words[indexUp], b, flags)
                        }
                    }
                    words!![i--] = word
                    x -= ELEMENTS_PER_WORD.toInt()
                }
            }
            --y
        }
        if (compress) {
            this.words = null
            singleton = lastFlags
        } else areaInit()
    }

    private fun fenceOrDoorLike(flags: Byte): Boolean {
        return Element.earth.flagsIn(flags) && Logic.fuzzy.`in`(flags) || Logic.doorway.`in`(flags)
    }

    private fun decompress() {
        val word = singletonWord()
        words = LongArray(DIMENSION_SQUARE_SIZE * DIMENSION_SIZE * ELEMENT_LENGTH / WORD_LENGTH)
        words?.let { Arrays.fill(words!!, word) }
        singleton = 0
    }

    private fun singletonWord(): Long {
        val singleton = singleton
        var word: Long = 0
        for (b in ELEMENTS_PER_WORD downTo 1) {
            word = word shl (1 shl ELEMENT_LENGTH_SHL.toInt())
            word = word or singleton.toLong()
        }
        return word
    }

    private fun areaInit() {
        val words = words ?: return
        var y = 0
        var index = DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
        while (y < DIMENSION_SIZE) {
            for (z in 1 until DIMENSION_EXTENT) {
                var x = 0
                while (x < DIMENSION_SIZE) {
                    var word = words[index]
                    val northWord = words[index - (DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt())]
                    val southWord = words[index + (DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt())]
                    var b = if (x == 0) 1 else 0
                    val bN = ELEMENTS_PER_WORD - if (x + ELEMENTS_PER_WORD >= DIMENSION_SIZE) 1 else 0
                    while (b < bN) {
                        val westWord = words[index - (b - 1 shr COORDINATE_TO_INDEX_SHR.toInt())]
                        val eastWord = words[index + (b + 1 shr COORDINATE_TO_INDEX_SHR.toInt())]
                        word = areaWordFor(word, b, northWord, eastWord, southWord, westWord)
                        ++b
                    }
                    words[index++] = word
                    x += ELEMENTS_PER_WORD.toInt()
                }
            }
            index += 2 * DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
            ++y
        }
    }

    fun areaInitNorth(other: OcclusionField?) {
        areaInitZPlane(other, false)
        areaInit = AreaInit.north.to(areaInit)
    }

    fun areaInitSouth(other: OcclusionField?) {
        areaInitZPlane(other, true)
        areaInit = AreaInit.south.to(areaInit)
    }

    fun areaInitWest(other: OcclusionField?) {
        areaInitXPlane(other, false)
        areaInit = AreaInit.west.to(areaInit)
    }

    fun areaInitEast(other: OcclusionField?) {
        areaInitXPlane(other, true)
        areaInit = AreaInit.east.to(areaInit)
    }

    fun areaInitNorthEast(horizontal: OcclusionField?, depth: OcclusionField?) {
        areaInitVerticalEdge(horizontal, depth, true, false)
        areaInit = AreaInit.northEast.to(areaInit)
    }

    fun areaInitSouthEast(horizontal: OcclusionField?, depth: OcclusionField?) {
        areaInitVerticalEdge(horizontal, depth, true, true)
        areaInit = AreaInit.southEast.to(areaInit)
    }

    fun areaInitNorthWest(horizontal: OcclusionField?, depth: OcclusionField?) {
        areaInitVerticalEdge(horizontal, depth, false, false)
        areaInit = AreaInit.northWest.to(areaInit)
    }

    fun areaInitSouthWest(horizontal: OcclusionField?, depth: OcclusionField?) {
        areaInitVerticalEdge(horizontal, depth, false, true)
        areaInit = AreaInit.southWest.to(areaInit)
    }

    fun areaInitUp(columnarSpace: IColumnarSpace?, cy: Int, other: OcclusionField?) {
        resolveTruncatedFencesAndDoors(columnarSpace, cy, other, true)
        areaInit = AreaInit.up.to(areaInit)
    }

    fun areaInitDown(columnarSpace: IColumnarSpace?, cy: Int, other: OcclusionField?) {
        resolveTruncatedFencesAndDoors(columnarSpace, cy, other, false)
        areaInit = AreaInit.down.to(areaInit)
    }

    private fun resolveTruncatedFencesAndDoors(
        columnarSpace: IColumnarSpace?,
        cy: Int,
        other: OcclusionField?,
        end: Boolean
    ) {
        var cy = cy
        if (other == null) return
        var i = LAST_INDEX - (DIMENSION_SQUARE_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()) + 1
        val subject: OcclusionField
        val `object`: OcclusionField
        if (end) {
            subject = this
            `object` = other
            cy++
        } else {
            subject = other
            `object` = this
        }
        val y = (cy shl DIMENSION_ORDER.toInt()) - 1
        val words = subject.words
        val singleton = subject.singleton
        var word: Long = 0
        for (z in 0 until DIMENSION_SIZE) {
            var x = 0
            while (x < DIMENSION_SIZE) {
                if (words != null) word = words[i++]
                for (b in 0 until ELEMENTS_PER_WORD) {
                    val xx = x + b
                    val flags: Byte
                    flags = if (words != null) (word and ELEMENT_MASK).toByte() else singleton
                    val fenceLike = Element.earth.flagsIn(flags) && Logic.fuzzy.`in`(flags)
                    val doorLike = Logic.doorway.`in`(flags)
                    if (fenceLike || doorLike) {
                        val block = columnarSpace!!.blockAt(xx, y, z)
                        if (fenceLike && block!!.isFenceLike || doorLike && block!!.isDoor) `object`[xx, 0, z] = flags
                    }
                    word = word shr (1 shl ELEMENT_LENGTH_SHL.toInt())
                }
                x += ELEMENTS_PER_WORD.toInt()
            }
        }
    }

    private fun areaInitZPlane(neighbor: OcclusionField?, end: Boolean) {
        var words = words
        val z0 = if (end) DIMENSION_EXTENT else 0
        val disposition = (z0 / DIMENSION_EXTENT shl 1) - 1
        val neighborWords = neighbor!!.words
        val singletonWord = if (words == null) singletonWord() else 0
        val neighborSingletonWord = if (neighborWords == null) neighbor.singletonWord() else 0
        var y = 0
        var index = z0 * DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
        var northIndex = (DIMENSION_SIZE - z0 - 1) * DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
        while (y < DIMENSION_SIZE) {
            var x = 0
            while (x < DIMENSION_SIZE) {
                var word = words?.get(index) ?: singletonWord
                val northWord: Long
                val southWord: Long
                run {
                    val primary =
                        if (words == null) singletonWord else words!![index + -disposition * (DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt())]
                    val secondary = neighborWords?.get(northIndex) ?: neighborSingletonWord
                    if (disposition < 0) {
                        northWord = secondary
                        southWord = primary
                    } else {
                        northWord = primary
                        southWord = secondary
                    }
                }
                var b = if (x == 0) 1 else 0
                val bN = ELEMENTS_PER_WORD - if (x + ELEMENTS_PER_WORD >= DIMENSION_SIZE) 1 else 0
                while (b < bN) {
                    val westWord: Long
                    val eastWord: Long
                    if (words == null) {
                        westWord = singletonWord
                        eastWord = westWord
                    } else {
                        westWord = words[index - (b - 1 shr COORDINATE_TO_INDEX_SHR.toInt())]
                        eastWord = words[index + (b + 1 shr COORDINATE_TO_INDEX_SHR.toInt())]
                    }
                    word = areaWordFor(word, b, northWord, eastWord, southWord, westWord)
                    ++b
                }
                if (words == null && word != singletonWord) {
                    decompress()
                    words = this.words
                }
                if (words != null) words[index] = word
                index++
                northIndex++
                x += ELEMENTS_PER_WORD.toInt()
            }
            val di = DIMENSION_EXTENT * DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
            index += di
            northIndex += di
            ++y
        }
    }

    private fun areaInitXPlane(neighbor: OcclusionField?, end: Boolean) {
        var words = words
        val x0 = if (end) DIMENSION_EXTENT else 0
        val disposition = (x0 / DIMENSION_EXTENT shl 1) - 1
        val offset = (disposition + 1 shr 1) * WORD_LAST_OFFSET
        val neighborWords = neighbor!!.words
        val singletonWord = if (words == null) singletonWord() else 0
        val neighborSingletonWord = if (neighborWords == null) neighbor.singletonWord() else 0
        var y = 0
        var index = x0 + DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
        var neighborIndex = DIMENSION_SIZE - x0 - 1 + DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
        while (y < DIMENSION_SIZE) {
            for (z in 1 until DIMENSION_EXTENT) {
                var word = words?.get(index) ?: singletonWord
                val westWord: Long
                val eastWord: Long
                val northWord: Long
                val southWord: Long
                run {
                    val primary = if (words == null) singletonWord else words!![index]
                    val secondary = neighborWords?.get(neighborIndex) ?: neighborSingletonWord
                    if (disposition < 0) {
                        westWord = secondary
                        eastWord = primary
                    } else {
                        westWord = primary
                        eastWord = secondary
                    }
                }
                if (words == null) {
                    northWord = singletonWord
                    southWord = northWord
                } else {
                    northWord = words[index - (DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt())]
                    southWord = words[index + (DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt())]
                }
                word = areaWordFor(word, offset, northWord, eastWord, southWord, westWord)
                if (words == null && word != singletonWord) {
                    decompress()
                    words = this.words
                }
                if (words != null) words[index] = word
                val di = DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
                index += di
                neighborIndex += di
            }
            val di = DIMENSION_SIZE * 2 shr COORDINATE_TO_INDEX_SHR.toInt()
            index += di
            neighborIndex += di
            ++y
        }
    }

    private fun areaInitVerticalEdge(
        horizNeighbor: OcclusionField?,
        depthNeighbor: OcclusionField?,
        horizEnd: Boolean,
        depthEnd: Boolean
    ) {
        var words = words
        val x0 = if (horizEnd) DIMENSION_EXTENT else 0
        val z0 = if (depthEnd) DIMENSION_EXTENT else 0
        val xd = (x0 / DIMENSION_EXTENT shl 1) - 1
        val zd = (z0 / DIMENSION_EXTENT shl 1) - 1
        val offset = (xd + 1 shr 1) * WORD_LAST_OFFSET
        val horizNeighborWords = horizNeighbor!!.words
        val depthNeighborWords = depthNeighbor!!.words
        val singletonWord = if (words == null) singletonWord() else 0
        val horizNeighborSingletonWord = if (horizNeighborWords == null) horizNeighbor.singletonWord() else 0
        val depthNeighborSingletonWord = if (depthNeighborWords == null) depthNeighbor.singletonWord() else 0
        var y = 0
        var index = z0 * DIMENSION_SIZE + x0 shr COORDINATE_TO_INDEX_SHR.toInt()
        var horizNeighborIndex = z0 * DIMENSION_SIZE + (DIMENSION_SIZE - x0 - 1) shr COORDINATE_TO_INDEX_SHR.toInt()
        var depthNeighborIndex = (DIMENSION_SIZE - z0 - 1) * DIMENSION_SIZE + x0 shr COORDINATE_TO_INDEX_SHR.toInt()
        while (y < DIMENSION_SIZE) {
            var word = words?.get(index) ?: singletonWord
            val westWord: Long
            val eastWord: Long
            val northWord: Long
            val southWord: Long
            run {
                val horizSecondary = horizNeighborWords?.get(horizNeighborIndex) ?: horizNeighborSingletonWord
                val depthSecondary = depthNeighborWords?.get(depthNeighborIndex) ?: depthNeighborSingletonWord
                if (xd < 0) {
                    westWord = horizSecondary
                    eastWord = word
                } else {
                    westWord = word
                    eastWord = horizSecondary
                }
                if (zd < 0) {
                    northWord = depthSecondary
                    southWord = word
                } else {
                    northWord = word
                    southWord = depthSecondary
                }
            }
            word = areaWordFor(word, offset, northWord, eastWord, southWord, westWord)
            if (words == null && word != singletonWord) {
                decompress()
                words = this.words
            }
            if (words != null) words[index] = word
            val di = DIMENSION_SIZE * DIMENSION_SIZE shr COORDINATE_TO_INDEX_SHR.toInt()
            index += di
            horizNeighborIndex += di
            depthNeighborIndex += di
            ++y
        }
    }

    private fun areaWordFor(
        centerWord: Long,
        offset: Int,
        northWord: Long,
        eastWord: Long,
        southWord: Long,
        westWord: Long
    ): Long {
        var centerWord = centerWord
        var centerFlags = elementAt(centerWord, offset)
        val northFlags = elementAt(northWord, offset)
        val southFlags = elementAt(southWord, offset)
        val westFlags = westFlags(offset, westWord)
        val eastFlags = eastFlags(offset, eastWord)
        centerFlags = areaFlagsFor(centerFlags, northFlags, eastFlags, southFlags, westFlags)
        centerWord = modifyWord(centerWord, offset, centerFlags)
        return centerWord
    }

    private fun fenceAndDoorAreaWordFor(
        columnarSpace: IColumnarSpace,
        dx: Int,
        y: Int,
        dz: Int,
        centerWord: Long,
        offset: Int,
        upWord: Long,
        downWord: Long,
        handlingFenceTops: Boolean
    ): Long {
        var centerWord = centerWord
        var centerFlags = elementAt(centerWord, offset)
        val upFlags = elementAt(upWord, offset)
        val downFlags = elementAt(downWord, offset)
        centerFlags =
            fenceAndDoorAreaFlagsFor(columnarSpace, dx, y, dz, centerFlags, upFlags, downFlags, handlingFenceTops)
        centerWord = modifyWord(centerWord, offset, centerFlags)
        return centerWord
    }

    private fun eastFlags(offset: Int, eastWord: Long): Byte {
        return elementAt(eastWord, (offset + 1) % ELEMENTS_PER_WORD)
    }

    private fun westFlags(offset: Int, westWord: Long): Byte {
        return elementAt(westWord, (offset + ELEMENTS_PER_WORD - 1) % ELEMENTS_PER_WORD)
    }

    private fun areaFlagsFor(
        centerFlags: Byte,
        northFlags: Byte,
        eastFlags: Byte,
        southFlags: Byte,
        westFlags: Byte
    ): Byte {
        var centerFlags = centerFlags
        val northElem: Element = Element.of(northFlags)
        val southElem: Element = Element.of(southFlags)
        val westElem: Element = Element.of(westFlags)
        val eastElem: Element = Element.of(eastFlags)
        val centerElem: Element = Element.of(centerFlags)
        if (Logic.ladder.`in`(centerFlags) && (northElem == Element.earth || eastElem == Element.earth || southElem == Element.earth || westElem == Element.earth)) centerFlags =
            Element.earth.to(centerFlags) else if (centerElem == Element.air && Logic.nothing.`in`(centerFlags)
            && (fuzziable(centerElem, northFlags) ||
                    fuzziable(centerElem, eastFlags) ||
                    fuzziable(centerElem, southFlags) ||
                    fuzziable(centerElem, westFlags))
        ) centerFlags = Logic.fuzzy.to(centerFlags)
        return centerFlags
    }

    private fun fenceAndDoorAreaFlagsFor(
        columnarSpace: IColumnarSpace,
        dx: Int,
        y: Int,
        dz: Int,
        centerFlags: Byte,
        upFlags: Byte,
        downFlags: Byte,
        handlingFenceTops: Boolean
    ): Byte {
        val downFenceOrDoorLike = fenceOrDoorLike(downFlags)
        val centerFenceOrDoorLike = fenceOrDoorLike(centerFlags)
        val centerBlock = columnarSpace.blockAt(dx, y, dz)
        val downBlock = columnarSpace.blockAt(dx, y - 1, dz)
        if (!centerBlock!!.isImpeding) {
            if (downFenceOrDoorLike && !centerFenceOrDoorLike && downBlock!!.isFenceLike || !handlingFenceTops && !downFenceOrDoorLike && centerFenceOrDoorLike && !centerBlock.isFenceLike ||
                downFenceOrDoorLike && centerFenceOrDoorLike &&
                (Logic.doorway.`in`(downFlags) && downBlock!!.isFenceLike && downBlock.isDoor && (Element.earth.flagsIn(
                    downFlags
                ) || !(centerBlock.isDoor && centerBlock.isFenceLike))
                        ||
                        Logic.doorway.`in`(centerFlags) && !(downBlock!!.isFenceLike && downBlock.isDoor))
            ) return downFlags
        } else if (downFenceOrDoorLike && centerFenceOrDoorLike &&
            Logic.doorway.`in`(downFlags) && Logic.doorway.`in`(centerFlags) &&
            downBlock!!.isDoor && centerBlock.isDoor
        ) return downFlags
        return centerFlags
    }

    private fun fuzziable(centerElem: Element, otherFlags: Byte): Boolean {
        val otherElement: Element = Element.of(otherFlags)
        return centerElem != otherElement && !(otherElement == Element.earth && Logic.fuzzy.`in`(otherFlags))
    }

    private fun modifyWord(word: Long, offset: Int, flags: Byte): Long {
        val shl = offset shl ELEMENT_LENGTH_SHL.toInt()
        return word and (ELEMENT_MASK shl shl).inv() or (flags.toLong() shl shl)
    }

    operator fun set(columnarSpace: IColumnarSpace, x: Int, y: Int, z: Int, blockDescription: IBlockDescription?) {
        val dx = x and DIMENSION_MASK
        val dy = y and DIMENSION_MASK
        val dz = z and DIMENSION_MASK
        val flags = flagsFor(columnarSpace, x, y, z, blockDescription)
        if (set(dx, dy, dz, flags)) {
            val dzb = dz > 0 && dz < DIMENSION_EXTENT
            val dxb = dx > 0 && dx < DIMENSION_EXTENT
            val dyb = dy > 0 && dy < DIMENSION_EXTENT
            if (dzb && dxb && dyb) areaComputeAt(dx, dy, dz) else greaterAreaComputeAt(columnarSpace, x, y, z)
            if (dx > 1 && dzb) areaComputeAt(dx - 1, dy, dz) else greaterAreaComputeAt(columnarSpace, x - 1, y, z)
            if (dx < DIMENSION_EXTENT - 1 && dzb) areaComputeAt(dx + 1, dy, dz) else greaterAreaComputeAt(
                columnarSpace,
                x + 1,
                y,
                z
            )
            if (dz > 1 && dxb) areaComputeAt(dx, dy, dz - 1) else greaterAreaComputeAt(columnarSpace, x, y, z - 1)
            if (dz < DIMENSION_EXTENT - 1 && dxb) areaComputeAt(dx, dy, dz + 1) else greaterAreaComputeAt(
                columnarSpace,
                x,
                y,
                z + 1
            )
            if (dy > 0 && dy < DIMENSION_EXTENT) fencesAndDoorsComputeAt(
                columnarSpace,
                dx,
                y,
                dz,
                true
            ) else if (y > 0 && y < (DIMENSION_SIZE shl 4) - 2) greaterFencesAndDoorsComputeAt(
                columnarSpace,
                x,
                y,
                z,
                true
            )
            if (dy > 1) fencesAndDoorsComputeAt(
                columnarSpace,
                dx,
                y - 1,
                dz,
                false
            ) else if (y > 1) greaterFencesAndDoorsComputeAt(columnarSpace, x, y - 1, z, false)
            if (dy < DIMENSION_EXTENT - 1) fencesAndDoorsComputeAt(
                columnarSpace,
                dx,
                y + 1,
                dz,
                false
            ) else if (y < (DIMENSION_SIZE shl 4) - 2) greaterFencesAndDoorsComputeAt(columnarSpace, x, y + 1, z, false)
        }
    }

    private operator fun set(dx: Int, dy: Int, dz: Int, flags: Byte): Boolean {
        if (words == null && flags != singleton) decompress()
        if (words != null) {
            val index = index(dx, dy, dz)
            val word = words!![index]
            words!![index] = modifyWord(word, dx % ELEMENTS_PER_WORD, flags)
            return true
        }
        return false
    }

    private fun index(dx: Int, dy: Int, dz: Int): Int {
        return dy * DIMENSION_SQUARE_SIZE + dz * DIMENSION_SIZE + dx shr COORDINATE_TO_INDEX_SHR.toInt()
    }

    private fun areaComputeAt(dx: Int, dy: Int, dz: Int) {
        val words = words
        val offset = dx % ELEMENTS_PER_WORD
        val index = index(dx, dy, dz)
        val northWord = words!![index(dx, dy, dz - 1)]
        val southWord = words[index(dx, dy, dz + 1)]
        val westWord = words[index(dx - 1, dy, dz)]
        val eastWord = words[index(dx + 1, dy, dz)]
        val centerWord = words[index]
        words[index] = areaWordFor(centerWord, offset, northWord, eastWord, southWord, westWord)
    }

    private fun fencesAndDoorsComputeAt(
        columnarSpace: IColumnarSpace,
        dx: Int,
        y: Int,
        dz: Int,
        handlingFenceTops: Boolean
    ) {
        val words = words
        val dy = y and DIMENSION_MASK
        val offset = dx % ELEMENTS_PER_WORD
        val index = index(dx, dy, dz)
        val downWord = words!![index(dx, dy - 1, dz)]
        val upWord = words[index(dx, dy + 1, dz)]
        val centerWord = words[index]
        words[index] =
            fenceAndDoorAreaWordFor(columnarSpace, dx, y, dz, centerWord, offset, upWord, downWord, handlingFenceTops)
    }

    private fun greaterAreaComputeAt(columnarSpace: IColumnarSpace, x: Int, y: Int, z: Int) {
        val dx = x and DIMENSION_MASK
        val dy = y and DIMENSION_MASK
        val dz = z and DIMENSION_MASK
        val cx = x shr DIMENSION_ORDER.toInt()
        val cy = y shr DIMENSION_ORDER.toInt()
        val cz = z shr DIMENSION_ORDER.toInt()
        val instance = columnarSpace.instance()
        val center: OcclusionField = ColumnarOcclusionFieldList.optionalOcclusionFieldAt(instance, cx, cy, cz)
            ?: return
        val north = ColumnarOcclusionFieldList.optionalOcclusionFieldAt(
            instance,
            cx,
            cy,
            z - 1 shr DIMENSION_ORDER.toInt()
        )
        val east = ColumnarOcclusionFieldList.optionalOcclusionFieldAt(
            instance,
            x + 1 shr DIMENSION_ORDER.toInt(),
            cy,
            cz
        )
        val south = ColumnarOcclusionFieldList.optionalOcclusionFieldAt(
            instance,
            cx,
            cy,
            z + 1 shr DIMENSION_ORDER.toInt()
        )
        val west = ColumnarOcclusionFieldList.optionalOcclusionFieldAt(
            instance,
            x - 1 shr DIMENSION_ORDER.toInt(),
            cy,
            cz
        )
        val centerFlags = center.elementAt(dx, dy, dz)
        val northFlags = north?.elementAt(dx, dy, dz - 1 and DIMENSION_MASK) ?: 0
        val southFlags = south?.elementAt(dx, dy, dz + 1 and DIMENSION_MASK) ?: 0
        val westFlags = west?.elementAt(dx - 1 and DIMENSION_MASK, dy, dz) ?: 0
        val eastFlags = east?.elementAt(dx + 1 and DIMENSION_MASK, dy, dz) ?: 0
        val flags = areaFlagsFor(centerFlags, northFlags, eastFlags, southFlags, westFlags)
        center[dx, dy, dz] = flags
    }

    private fun greaterFencesAndDoorsComputeAt(
        columnarSpace: IColumnarSpace,
        x: Int,
        y: Int,
        z: Int,
        handlingFenceTops: Boolean
    ) {
        val dx = x and DIMENSION_MASK
        val dy = y and DIMENSION_MASK
        val dz = z and DIMENSION_MASK
        val cx = x shr DIMENSION_ORDER.toInt()
        val cy = y shr DIMENSION_ORDER.toInt()
        val cz = z shr DIMENSION_ORDER.toInt()
        val instance = columnarSpace.instance()
        val center: OcclusionField = ColumnarOcclusionFieldList.optionalOcclusionFieldAt(instance, cx, cy, cz)
            ?: return
        val up: OcclusionField? = ColumnarOcclusionFieldList.optionalOcclusionFieldAt(
            instance,
            cx,
            y + 1 shr DIMENSION_ORDER.toInt(),
            cz
        )
        val down: OcclusionField? = ColumnarOcclusionFieldList.optionalOcclusionFieldAt(
            instance,
            cx,
            y - 1 shr DIMENSION_ORDER.toInt(),
            cz
        )
        val centerFlags = center.elementAt(dx, dy, dz)
        val upFlags = up?.elementAt(dx, dy + 1 and DIMENSION_MASK, dz) ?: 0
        val downFlags = down?.elementAt(dx, dy - 1 and DIMENSION_MASK, dz) ?: 0
        val flags =
            fenceAndDoorAreaFlagsFor(columnarSpace, dx, y, dz, centerFlags, upFlags, downFlags, handlingFenceTops)
        center[dx, dy, dz] = flags
    }

    override fun elementAt(x: Int, y: Int, z: Int): Byte {
        val element: Byte
        element = if (words != null) {
            val word = words!![index(x, y, z)]
            elementAt(word, x % ELEMENTS_PER_WORD)
        } else singleton
        return element
    }

    private fun elementAt(word: Long, offset: Int) =
        ((word shr (offset shl ELEMENT_LENGTH_SHL.toInt())).toByte()) and ELEMENT_MASK.toByte()

    override fun visualizeAt(dy: Int): String {
        return visualizeAt(this, dy, 0, 0, DIMENSION_SIZE, DIMENSION_SIZE)
    }

    private fun flagsFor(columnarSpace: IColumnarSpace, x: Int, y: Int, z: Int, block: IBlockDescription?): Byte {
        var flags: Byte = 0
        val instance = columnarSpace.instance()
        val doorway = block!!.isDoor
        if (doorway) flags = flags or (if (instance!!.blockObjectAt(
                x,
                y,
                z
            ).isImpeding
        ) Element.earth else Element.air).mask else if (!block.isImpeding) if (block.isLiquid) if (block.isIncinerating) flags =
            flags or Element.fire.mask else flags = flags or Element.water.mask else if (block.isIncinerating) flags =
            flags or (Element.fire.mask or Logic.fuzzy.mask) else flags =
            flags or Element.air.mask else if (block.isIncinerating) flags = flags or Element.fire.mask else flags =
            flags or Element.earth.mask
        if (doorway) flags = Logic.doorway.to(flags) else if (block.isClimbable) flags =
            Logic.ladder.to(flags) else if (Element.earth.flagsIn(flags) && !block.isFullyBounded) flags =
            Logic.fuzzy.to(flags)
        return flags
    }

    companion object {
        private const val ELEMENT_LENGTH_SHL: Byte = 2
        private const val ELEMENT_LENGTH = (1 shl ELEMENT_LENGTH_SHL.toInt()).toByte()
        private const val WORD_LENGTH: Byte = 64
        private const val ELEMENTS_PER_WORD = (WORD_LENGTH / ELEMENT_LENGTH).toByte()
        private const val WORD_LAST_OFFSET = (ELEMENTS_PER_WORD - 1).toByte()
        private const val COORDINATE_TO_INDEX_SHR: Byte = 4
        private const val DIMENSION_ORDER: Byte = 4
        const val DIMENSION_SIZE = 1 shl DIMENSION_ORDER.toInt()
        const val DIMENSION_MASK = (1 shl DIMENSION_ORDER.toInt()) - 1
        const val DIMENSION_EXTENT = DIMENSION_SIZE - 1
        private const val DIMENSION_SQUARE_SIZE = DIMENSION_SIZE * DIMENSION_SIZE
        private const val LAST_INDEX = DIMENSION_SIZE * DIMENSION_SQUARE_SIZE - 1 shr COORDINATE_TO_INDEX_SHR.toInt()
        private const val ELEMENT_MASK = ((1 shl ELEMENT_LENGTH.toInt()) - 1).toLong()
        private const val FULLY_AREA_INIT: Short = 0x3FF
        fun fuzzyOpenIn(element: Byte): Boolean {
            return Element.air.flagsIn(element) || Element.earth.flagsIn(element) && Logic.fuzzy.`in`(element)
        }

        fun visualizeAt(provider: IOcclusionProvider, dy: Int, x0: Int, z0: Int, xN: Int, zN: Int): String {
            val sb = StringBuilder()
            for (z in z0 until zN) {
                for (x in x0 until xN) {
                    val ch: Char
                    val flags = provider.elementAt(x, dy, z)
                    ch = when (Element.of(flags)) {
                        Element.air -> if (Logic.fuzzy.`in`(flags)) '░' else ' '
                        Element.earth -> if (Logic.climbable(flags)) '#' else if (Logic.fuzzy.`in`(
                                flags
                            )
                        ) '▄' else '█'
                        Element.fire -> 'X'
                        Element.water -> '≋'
                        else -> '?'
                    }
                    sb.append(ch)
                }
                sb.append(System.lineSeparator())
            }
            return sb.toString()
        }
    }
}