package dev.gtcl.astro

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MyGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        val bitmapPoolSizeBytes = 1024 * 1024 * 0L
        val memoryCacheSizeBytes = 1024 * 1024 * 0L
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes))
        builder.setBitmapPool(LruBitmapPool(bitmapPoolSizeBytes))
    }
}