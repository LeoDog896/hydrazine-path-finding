package com.extollit.gaming.ai.path.model

/**
 * A one-dimensional store for occlusion fields aligned along the y-axis distributed according to chunk coordinates.
 * Define an object of this class as a final field member of your concrete implementation of [IColumnarSpace] then
 * implement [IColumnarSpace.occlusionFields] to return this object.
 *
 * Additionally, some members of this type must be called upon certain server events.  Methods that the implementor should
 * be aware of are documented whereas methods below that remain undocumented are for internal use.
 *
 * @see IColumnarSpace
 *
 * @see OcclusionField
 */
open class ColumnarOcclusionFieldList
/**
 * Construct a new object bound to the specified columnar space container, this is what [.container] will be
 * bound to in a one-to-one relationship.
 *
 * @param container columnar space that owns this object
 */(
    /**
     * Containing columnar space that owns this object.  This object will typically have a final field member that points
     * to this object and returns it via [IColumnarSpace.occlusionFields]
     */
    val container: IColumnarSpace
) {
    private var fields: Array<OcclusionField?>? = null

    /**
     * Completely erases all data in this object, this must be called by the implementor prior to loading a chunk or
     * unloading a chunk to prevent stale state creep.  This effectively forces lazy-reinitialization of the occlusion
     * field cache.
     */
    fun reset() {
        fields = null
    }

    /**
     * Notifies the occlusion field cache that a block in the containing columnar space has changed (i.e has been added,
     * removed, changed type, or has had its meta-data changed).  The implementor must call this method whenever this
     * change occurs in the server, it causes the associated occlusion field data to update accordingly.
     *
     * This method's coordinate parameters are exceptional because they are absolute (relative to the instance) rather
     * than relative (to the parent columnar space)
     *
     * @param x absolute (relative to the instance space, not the columnar space) x coordinate of the block that changed
     * @param y absolute (relative to the instance space, not the columnar space) y coordinate of the block that changed
     * @param z absolute (relative to the instance space, not the columnar space) z coordinate of the block that changed
     * @param description description of the new block replacing what existed previously
     * @param metaData meta-data for the new block replacing what existed previously
     */
    fun onBlockChanged(x: Int, y: Int, z: Int, description: IBlockDescription?, metaData: Int) {
        if (fields == null) return
        val field = fields!![y shr 4 and 0xF] ?: return
        field[container, x, y, z] = description
    }

    fun occlusionFieldAt(cx: Int, cy: Int, cz: Int): OcclusionField {
        if (fields == null) fields = arrayOfNulls(OcclusionField.DIMENSION_SIZE)
        val result = fields!![cy]
        return if (result != null) {
            result
        } else {
            val occlusionField = createOcclusionField(cx, cy, cz)
            occlusionField.apply { fields!![cy] = this }
        }
    }

    fun optionalOcclusionFieldAt(cy: Int): OcclusionField? = if (fields == null) null else fields!![cy]

    protected open fun createOcclusionField(cx: Int, cy: Int, cz: Int): OcclusionField {
        val occlusionField = OcclusionField()
        occlusionField.loadFrom(container, cx, cy, cz)
        return occlusionField
    }

    companion object {
        fun optionalOcclusionFieldAt(instance: IInstanceSpace?, cx: Int, cy: Int, cz: Int): OcclusionField? {
            val columnarSpace = instance!!.columnarSpaceAt(cx, cz) ?: return null
            return columnarSpace.occlusionFields().optionalOcclusionFieldAt(cy)
        }
    }
}