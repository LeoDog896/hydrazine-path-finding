package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.AreaOcclusionProvider
import com.extollit.gaming.ai.path.model.IColumnarSpace
import com.extollit.gaming.ai.path.model.IInstanceSpace
import com.extollit.gaming.ai.path.model.IOcclusionProviderFactory

internal object AreaOcclusionProviderFactory : IOcclusionProviderFactory {

    /**
     * Creates an [AreaOcclusionProvider] from an instance space.
     *
     * @param instance The instance where it will grab columnar spaces from.
     *
     * TODO figure out what c(x/z)(0/N) is
     */
    override fun fromInstanceSpace(
        instance: IInstanceSpace,
        cx0: Int,
        cz0: Int,
        cxN: Int,
        czN: Int
    ): AreaOcclusionProvider {
        // Creates a 2d array with X and Z coordinates with regular (16x16) chunk sizes
        val array = Array(czN - cz0 + 1) { arrayOfNulls<IColumnarSpace>(cxN - cx0 + 1) }
        for (cz in cz0..czN) {
            for (cx in cx0..cxN) {
                // Creates "columnar" spaces representing a 16x16x256 chunk area.
                val columnarSpace = instance.columnarSpaceAt(cx, cz)

                // If its not null (aka successful), set it in the array
                if (columnarSpace != null) array[cz - cz0][cx - cx0] = columnarSpace
            }
        }


        return AreaOcclusionProvider(array, cx0, cz0)
    }
}