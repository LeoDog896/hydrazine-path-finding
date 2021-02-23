package com.extollit.gaming.ai.path.model

/**
 * A static utility object which can create other [AreaOcclusionProvider]s.
 *
 * @see [fromInstanceSpace]
 */
interface IOcclusionProviderFactory {

    /**
     * Creates an [AreaOcclusionProvider] from an instance space.
     *
     * @param instance The instance where it will grab columnar spaces from.
     * @param centerXFrom The
     */
    fun fromInstanceSpace(
        instance: IInstanceSpace,
        centerXFrom: Int, centerZFrom: Int,
        centerXTo: Int, centerZTo: Int
    ): IOcclusionProvider
}