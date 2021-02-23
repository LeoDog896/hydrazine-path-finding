package com.extollit.gaming.ai.path.model

class FlagSampler(private val occlusionProvider: IOcclusionProvider?) {
    var volatility = 0
        private set

    fun flagsAt(x: Int, y: Int, z: Int): Byte {
        val flags = occlusionProvider!!.elementAt(x, y, z)
        if (volatileIn(flags)) volatility++
        return flags
    }

    private fun volatileIn(flags: Byte): Boolean = Logic.doorway.flagsIn(flags)
}