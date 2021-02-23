package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.AreaOcclusionProvider
import com.extollit.gaming.ai.path.model.IColumnarSpace
import com.extollit.gaming.ai.path.model.IInstanceSpace
import com.extollit.gaming.ai.path.model.IOcclusionProviderFactory

internal object AreaOcclusionProviderFactory : IOcclusionProviderFactory {

    override fun fromInstanceSpace(
        instance: IInstanceSpace,
        centerXFrom: Int,
        centerZFrom: Int,
        centerXTo: Int,
        centerZTo: Int
    ): AreaOcclusionProvider {

        /*
        Creates a 2d array with X and Z coordinates with regular (16x16) chunk sizes
        It grabs the delta from centerFrom and centerTo + 1 to make up for arrays starting at 0
         */
        val array = Array(centerZTo - centerZFrom + 1) { arrayOfNulls<IColumnarSpace>(centerXTo - centerXFrom + 1) }

        // Loop through every single X and Z.
        for (centerZ in centerZFrom..centerZTo) {
            for (centerX in centerXFrom..centerXTo) {

                // Creates "columnar" spaces representing a 16x16x256 chunk area.
                val columnarSpace = instance.columnarSpaceAt(centerX, centerZ)

                // If its not null (aka successful), set it in the array
                if (columnarSpace != null) array[centerZ - centerZFrom][centerX - centerXFrom] = columnarSpace
            }
        }

        // Creates the OcclusionProvider, providing data about everything in this instance space.
        return AreaOcclusionProvider(array, centerXFrom, centerZFrom)
    }
}