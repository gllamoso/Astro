package dev.gtcl.reddit.repositories

import androidx.annotation.MainThread
import dev.gtcl.reddit.network.GfycatApi
import dev.gtcl.reddit.network.RedgifsApi

class GfycatRepository private constructor(){
    @MainThread
    fun getGfycatInfo(gfyid: String) = GfycatApi.retrofit.getGfycatInfo(gfyid)

    @MainThread
    fun getGfycatInfoFromRedgifs(gfyid: String) = RedgifsApi.retrofit.getGfycatInfo(gfyid)

    companion object{
        private lateinit var INSTANCE: GfycatRepository
        fun getInstance(): GfycatRepository{
            if(!Companion::INSTANCE.isInitialized){
                INSTANCE = GfycatRepository()
            }
            return INSTANCE
        }
    }
}