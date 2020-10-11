package dev.gtcl.astro.repositories

import dev.gtcl.astro.network.GfycatApi
import dev.gtcl.astro.network.RedgifsApi

class GfycatRepository private constructor() {
    fun getGfycatInfo(gfyid: String) = GfycatApi.retrofit.getGfycatInfo(gfyid)

    fun getGfycatInfoFromRedgifs(gfyid: String) = RedgifsApi.retrofit.getGfycatInfo(gfyid)

    companion object {
        private lateinit var INSTANCE: GfycatRepository
        fun getInstance(): GfycatRepository {
            if (!Companion::INSTANCE.isInitialized) {
                INSTANCE = GfycatRepository()
            }
            return INSTANCE
        }
    }
}