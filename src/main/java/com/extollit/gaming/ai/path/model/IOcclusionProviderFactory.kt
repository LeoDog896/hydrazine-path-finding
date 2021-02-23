package com.extollit.gaming.ai.path.model

interface IOcclusionProviderFactory {
    fun fromInstanceSpace(instance: IInstanceSpace, cx0: Int, cz0: Int, cxN: Int, czN: Int): IOcclusionProvider
}