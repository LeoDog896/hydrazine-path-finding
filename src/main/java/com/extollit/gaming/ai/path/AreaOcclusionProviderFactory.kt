package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.AreaOcclusionProvider
import com.extollit.gaming.ai.path.model.IColumnarSpace
import com.extollit.gaming.ai.path.model.IInstanceSpace
import com.extollit.gaming.ai.path.model.IOcclusionProviderFactory

internal object AreaOcclusionProviderFactory : IOcclusionProviderFactory {
    override fun fromInstanceSpace(
        instance: IInstanceSpace,
        cx0: Int,
        cz0: Int,
        cxN: Int,
        czN: Int
    ): AreaOcclusionProvider {
        val array = Array(czN - cz0 + 1) { arrayOfNulls<IColumnarSpace>(cxN - cx0 + 1) }
        for (cz in cz0..czN) for (cx in cx0..cxN) {
            val columnarSpace = instance.columnarSpaceAt(cx, cz)
            if (columnarSpace != null) array[cz - cz0][cx - cx0] = columnarSpace
        }
        return AreaOcclusionProvider(array, cx0, cz0)
    }
}