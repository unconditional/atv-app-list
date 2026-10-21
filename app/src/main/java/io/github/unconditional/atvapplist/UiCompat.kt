package io.github.unconditional.atvapplist

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build

/** Small helpers that keep minSdk 21 support without AndroidX. */
internal object UiCompat {

    fun color(context: Context, id: Int): Int =
        if (Build.VERSION.SDK_INT >= 23) context.getColor(id)
        else context.resources.getColor(id)

    fun colorStateList(context: Context, id: Int): ColorStateList =
        if (Build.VERSION.SDK_INT >= 23) context.getColorStateList(id)
        else context.resources.getColorStateList(id)

    fun drawable(context: Context, id: Int): Drawable? =
        if (Build.VERSION.SDK_INT >= 23) context.getDrawable(id)
        else context.resources.getDrawable(id)

    fun dip(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}
