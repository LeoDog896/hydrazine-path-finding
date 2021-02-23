package com.extollit.gaming.ai.path.model

import com.extollit.gaming.ai.path.model.OcclusionField.AreaInit

class AreaOcclusionProvider(
    /** Represents all colum spaces in both the X ad Z planes. */
    private val columnarSpaces: Array<Array<IColumnarSpace?>>,
    private val cx0: Int,
    private val cz0: Int
) : IOcclusionProvider {

    private val cxN: Int = columnarSpaces[0].size + cx0 - 1
    private val czN: Int = columnarSpaces.size + cz0 - 1

    override fun elementAt(x: Int, y: Int, z: Int): Byte {
        val columnarSpaces = columnarSpaces
        val cx = x shr 4
        val cz = z shr 4
        val cy = y shr 4
        if (cx in cx0..cxN && cz >= cz0 && cz <= czN && cy >= 0 && cy < OcclusionField.DIMENSION_SIZE) {
            val czz = cz - cz0
            val cxx = cx - cx0
            val columnarSpace = columnarSpaces[czz][cxx]
            if (columnarSpace != null) {
                val field = columnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz)
                if (!field.areaInitFull()) areaInit(field, x, y, z)
                return field.elementAt(
                    x and OcclusionField.DIMENSION_MASK,
                    y and OcclusionField.DIMENSION_MASK,
                    z and OcclusionField.DIMENSION_MASK
                )
            }
        }
        return 0
    }

    private fun areaInit(field: OcclusionField?, x: Int, y: Int, z: Int) {
        val columnarSpaces = columnarSpaces
        val cx = x shr 4
        val cy = y shr 4
        val cz = z shr 4
        val cxN: Int = columnarSpaces[0].size - 1
        val czN = columnarSpaces.size - 1
        val czz = cz - cz0
        val cxx = cx - cx0
        val xx = x and OcclusionField.DIMENSION_MASK
        val yy = y and OcclusionField.DIMENSION_MASK
        val zz = z and OcclusionField.DIMENSION_MASK
        val centerColumnarSpace = columnarSpaces[czz][cxx]
        if (xx == 0 && zz == 0 && !field!!.areaInitAt(AreaInit.northWest) && cxx > 0 && czz > 0) {
            val westColumnarSpace = columnarSpaces[czz - 1][cxx]
            val northColumnarSpace = columnarSpaces[czz][cxx - 1]
            if (northColumnarSpace != null && westColumnarSpace != null) field.areaInitNorthWest(
                northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz),
                westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1)
            )
        } else if (xx == OcclusionField.DIMENSION_EXTENT && zz == 0 && !field!!.areaInitAt(AreaInit.northEast) && cxx < cxN && czz > 0) {
            val westColumnarSpace = columnarSpaces[czz - 1][cxx]
            val eastColumnarSpace = columnarSpaces[czz][cxx + 1]
            if (eastColumnarSpace != null && westColumnarSpace != null) field.areaInitNorthEast(
                eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz),
                westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1)
            )
        } else if (xx == 0 && zz == OcclusionField.DIMENSION_EXTENT && !field!!.areaInitAt(AreaInit.southWest) && cxx > 0 && czz < czN) {
            val northColumnarSpace = columnarSpaces[czz][cxx - 1]
            val southColumnarSpace = columnarSpaces[czz + 1][cxx]
            if (northColumnarSpace != null && southColumnarSpace != null) field.areaInitSouthWest(
                northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz),
                southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1)
            )
        } else if (xx == OcclusionField.DIMENSION_EXTENT && zz == OcclusionField.DIMENSION_EXTENT && !field!!.areaInitAt(
                AreaInit.southEast
            ) && cxx < cxN && czz < czN
        ) {
            val eastColumnarSpace = columnarSpaces[czz][cxx + 1]
            val southColumnarSpace = columnarSpaces[czz + 1][cxx]
            if (eastColumnarSpace != null && southColumnarSpace != null) field.areaInitSouthEast(
                eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz),
                southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1)
            )
        } else if (xx == 0 && !field!!.areaInitAt(AreaInit.west) && cxx > 0) {
            val northColumnarSpace = columnarSpaces[czz][cxx - 1]
            if (northColumnarSpace != null) field.areaInitWest(
                northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz)
            )
        } else if (xx == OcclusionField.DIMENSION_EXTENT && !field!!.areaInitAt(AreaInit.east) && cxx < cxN) {
            val eastColumnarSpace = columnarSpaces[czz][cxx + 1]
            if (eastColumnarSpace != null) field.areaInitEast(
                eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz)
            )
        } else if (zz == 0 && !field!!.areaInitAt(AreaInit.north) && czz > 0) {
            val westColumnarSpace = columnarSpaces[czz - 1][cxx]
            if (westColumnarSpace != null) field.areaInitNorth(
                westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1)
            )
        } else if (zz == OcclusionField.DIMENSION_EXTENT && !field!!.areaInitAt(AreaInit.south) && czz < czN) {
            val southColumnarSpace = columnarSpaces[czz + 1][cxx]
            if (southColumnarSpace != null) field.areaInitSouth(
                southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1)
            )
        }
        if (yy == OcclusionField.DIMENSION_EXTENT && !field!!.areaInitAt(AreaInit.up)) {
            field.areaInitUp(
                centerColumnarSpace,
                cy,
                if (cy < OcclusionField.DIMENSION_EXTENT) centerColumnarSpace!!.occlusionFields()
                    .occlusionFieldAt(cx, cy + 1, cz) else null
            )
        } else if (yy == 0 && !field!!.areaInitAt(AreaInit.down)) {
            field.areaInitDown(
                centerColumnarSpace,
                cy,
                if (cy > 0) centerColumnarSpace!!.occlusionFields().occlusionFieldAt(cx, cy - 1, cz) else null
            )
        }
    }

    override fun visualizeAt(y: Int): String =
        OcclusionField.visualizeAt(this, y, cx0 shl 4, cz0 shl 4, cxN + 1 shl 4, czN + 1 shl 4)

}