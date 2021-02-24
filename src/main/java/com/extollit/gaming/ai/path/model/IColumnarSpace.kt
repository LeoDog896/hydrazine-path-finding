package com.extollit.gaming.ai.path.model

/**
 * Represents a logical columnar division of space 16x16 blocks wide and deep (x/z plane) and 256 blocks high (along the y-axis)
 *
 * @see ColumnarOcclusionFieldList
 */
interface IColumnarSpace {
    /**
     * Returns a basic description of the block located at the specified coordinates relative to the columnar space.
     *
     * This is only the basic block description, the result is never downcasted to a [IBlockObject] object by
     * the engine.
     *
     * @param x relative x-coordinate
     * @param y relative y-coordinate
     * @param z relative z-coordinate
     * @return invariant concrete type of [IBlockDescription] at the aforementioned relative coordinates
     */
    fun blockAt(x: Int, y: Int, z: Int): IBlockDescription?

    /**
     * Returns the Notchian meta-data of the block located at the specified coordinates relative to the columnar space.
     *
     * This is currently not used.
     *
     * @param x relative x-coordinate
     * @param y relative y-coordinate
     * @param z relative z-coordinate
     * @return Notchian block meta-data nibble for the block at the aforementioned relative coordinates
     */
    fun metaDataAt(x: Int, y: Int, z: Int): Int

    /**
     * Retrieves the list of occlusion fields (aligned-along the y-axis) associated with this columnar space.
     * Typically this would retrieve a final field member that the implementor defines on the concrete
     * [IColumnarSpace] type.
     *
     * @return the object that manages the occlusion fields stored in this columnar space.
     */
    fun occlusionFields(): ColumnarOcclusionFieldList

    /**
     * Each columnar space belongs to an instance in a many-to-one (columnar-to-instance) relationship, this is its parent.
     *
     * @return parent instance that contains this columnar space
     */
    fun instance(): IInstanceSpace
}