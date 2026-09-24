package com.fyp.basarattradeinvest.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ImageLoader {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun loadImage(imageView: ImageView, url: String?) {
        if (url.isNullOrBlank()) {
            imageView.visibility = View.GONE
            return
        }

        val cachedBitmap = memoryCache.get(url)
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap)
            imageView.visibility = View.VISIBLE
            return
        }

        imageView.tag = url

        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                downloadBitmap(url)
            }
            if (bitmap != null) {
                memoryCache.put(url, bitmap)
                if (imageView.tag == url) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                }
            } else {
                if (imageView.tag == url) {
                    imageView.visibility = View.GONE
                }
            }
        }
    }

    private fun downloadBitmap(urlStr: String): Bitmap? {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            connection.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }
}
