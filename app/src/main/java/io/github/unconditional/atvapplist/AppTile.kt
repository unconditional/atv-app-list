package io.github.unconditional.atvapplist

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.text.TextUtils

/**
 * One grid tile: icon + name + optional meta line (e.g. "2 h ago").
 *
 * Tap/click and OK launch the app. Holding OK (or long touch) triggers the
 * actions dialog; OK long-press is detected via key repeats because plain
 * hardware-key long presses do not call performLongClick on every remote.
 */
internal class AppTile @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val iconView = ImageView(context)
    private val labelView = TextView(context)
    private val metaView = TextView(context)

    var entry: AppEntry? = null
        private set

    var onClickAction: ((AppTile) -> Unit)? = null
    var onLongPressAction: ((AppTile) -> Unit)? = null

    private var longPressFired = false

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isFocusable = true
        isClickable = true
        isLongClickable = true
        UiCompat.drawable(context, R.drawable.tile_bg)?.let { background = it }

        val pad = resources.getDimensionPixelSize(R.dimen.tile_padding)
        setPadding(pad, pad, pad, pad)

        val iconSize = resources.getDimensionPixelSize(R.dimen.tile_icon_size)
        iconView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, iconSize)
        iconView.scaleType = ImageView.ScaleType.FIT_CENTER
        addView(iconView)

        val labelParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        labelParams.topMargin = resources.getDimensionPixelSize(R.dimen.tile_label_top)
        labelView.layoutParams = labelParams
        labelView.gravity = Gravity.CENTER
        labelView.textSize = 15f
        labelView.setTextColor(UiCompat.colorStateList(context, R.color.tile_label))
        labelView.isDuplicateParentStateEnabled = true
        labelView.maxLines = 1
        labelView.ellipsize = TextUtils.TruncateAt.END
        addView(labelView)

        metaView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        metaView.gravity = Gravity.CENTER
        metaView.textSize = 12f
        metaView.setTextColor(UiCompat.colorStateList(context, R.color.tile_meta))
        metaView.isDuplicateParentStateEnabled = true
        metaView.maxLines = 1
        metaView.ellipsize = TextUtils.TruncateAt.END
        metaView.visibility = GONE
        addView(metaView)
    }

    fun bind(app: AppEntry, meta: CharSequence?) {
        entry = app
        iconView.setImageDrawable(app.icon)
        labelView.text = AppSorts.displayName(app)
        if (meta.isNullOrEmpty()) {
            metaView.text = ""
            metaView.visibility = GONE
        } else {
            metaView.text = meta
            metaView.visibility = VISIBLE
        }
        contentDescription = AppSorts.displayName(app)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        val scale = if (gainFocus) 1.05f else 1.0f
        animate().scaleX(scale).scaleY(scale).setDuration(120L).start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isConfirmKey(keyCode) && event.repeatCount >= LONG_PRESS_REPEATS && !longPressFired) {
            longPressFired = true
            isPressed = false
            onLongPressAction?.invoke(this)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isConfirmKey(keyCode) && longPressFired) {
            longPressFired = false
            isPressed = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun isConfirmKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER

    companion object {
        private const val LONG_PRESS_REPEATS = 3
    }
}
