package com.waterbuddy.app

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import java.nio.ByteBuffer

/** Loads an animated buddy asset (WebP or GIF) by base name and plays it.
 *  This is the ONLY place that touches the character files, so swapping in
 *  Gemini art is just: drop your files in app/src/main/assets keeping the
 *  same names (buddy_walk / buddy_idle / buddy_wave / buddy_drink). */
object Buddy {
    const val WALK = "buddy_walk"
    const val IDLE = "buddy_idle"
    const val WAVE = "buddy_wave"
    const val DRINK = "buddy_drink"

    private val EXTS = listOf("webp", "gif", "png")

    private fun assetFor(c: Context, base: String): String? {
        val files = runCatching { c.assets.list("")?.toList() ?: emptyList() }.getOrDefault(emptyList())
        for (e in EXTS) {
            val name = "$base.$e"
            if (files.contains(name)) return name
        }
        return null
    }

    /** Load and start looping. Returns the drawable so callers can stop it. */
    fun play(iv: ImageView, base: String, loop: Boolean = true): Drawable? {
        val c = iv.context
        val name = assetFor(c, base) ?: return null
        return try {
            val bytes = c.assets.open(name).use { it.readBytes() }
            val src = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            val d = ImageDecoder.decodeDrawable(src)
            iv.setImageDrawable(d)
            if (d is AnimatedImageDrawable) {
                d.repeatCount = if (loop) AnimatedImageDrawable.REPEAT_INFINITE else 0
                d.start()
            }
            d
        } catch (e: Exception) {
            null
        }
    }
}
