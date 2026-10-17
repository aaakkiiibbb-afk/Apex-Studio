package com.apex.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache

object LowRamBitmapLoader {
    private val maxCacheBytes = 12 * 1024 * 1024
    private val bitmapCache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun decodeOptimized(context: Context, uri: Uri, maxWidth: Int = 1280, maxHeight: Int = 720): Bitmap? {
        val cacheKey = "$uri-${maxWidth}x$maxHeight"
        bitmapCache.get(cacheKey)?.let { if (!it.isRecycled) return it }

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }

        val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, maxWidth, maxHeight)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }

        if (decoded != null) {
            bitmapCache.put(cacheKey, decoded)
        }
        return decoded
    }

    private fun calculateInSampleSize(rawW: Int, rawH: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (rawH > reqH || rawW > reqW) {
            val halfH = rawH / 2
            val halfW = rawW / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun trimMemory() {
        bitmapCache.evictAll()
        System.gc()
    }
}
