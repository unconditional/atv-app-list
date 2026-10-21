package io.github.unconditional.atvapplist

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Vertical scroll container that lays out rows of [AppTile]s.
 * A plain ScrollView keeps focus-based auto-scrolling working with the D-pad.
 */
internal class AppGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    interface Listener {
        fun onTileFocused(tile: AppTile, index: Int)
        fun onTileClicked(entry: AppEntry)
        fun onTileLongPressed(entry: AppEntry)
    }

    var listener: Listener? = null

    private val content = LinearLayout(context)
    private val pool = ArrayList<AppTile>()
    private val activeTiles = ArrayList<AppTile>()
    private var lastIndex = -1

    /** Columns the last rebuild laid out; drives re-flow when the width changes. */
    private var builtColumns = 0

    private var entries: List<AppEntry> = emptyList()
    private var metaProvider: ((AppEntry) -> CharSequence?)? = null

    val tileCount: Int get() = activeTiles.size

    init {
        content.orientation = LinearLayout.VERTICAL
        addView(
            content,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        isVerticalScrollBarEnabled = false
        clipToPadding = false
    }

    fun focusedIndex(): Int = lastIndex.takeIf { it in activeTiles.indices } ?: -1

    fun tileAt(index: Int): AppTile? = activeTiles.getOrNull(index)

    fun indexOfPackage(pkg: String): Int {
        for (i in activeTiles.indices) {
            if (activeTiles[i].entry?.packageName == pkg) return i
        }
        return -1
    }

    fun setApps(newEntries: List<AppEntry>, metaFor: (AppEntry) -> CharSequence?) {
        entries = newEntries
        metaProvider = metaFor
        rebuild()
    }

    fun requestFocusTile(index: Int): Boolean {
        val tile = activeTiles.getOrNull(index) ?: return false
        return tile.requestFocus()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Late first layout (list loaded before the first layout pass): re-fill columns.
        if (w > 0 && w != oldw && entries.isNotEmpty() && builtColumns != columnCount(w)) {
            rebuild()
        }
    }

    private fun rebuild() {
        builtColumns = columnCount(width)
        pool.addAll(activeTiles)
        activeTiles.clear()
        lastIndex = -1
        content.removeAllViews()

        val columns = columnCount(width)
        var start = 0
        while (start < entries.size) {
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            val rowParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            rowParams.topMargin = if (content.childCount == 0) 0
            else resources.getDimensionPixelSize(R.dimen.row_gap)
            content.addView(row, rowParams)

            val end = minOf(start + columns, entries.size)
            for (i in start until end) {
                val tile = obtainTile()
                tile.bind(entries[i], metaProvider?.invoke(entries[i]))
                activeTiles.add(tile)
                row.addView(tile, tileParams())
            }
            start = end
        }
    }

    private fun columnCount(widthPx: Int): Int {
        val minTile = resources.getDimensionPixelSize(R.dimen.tile_min_width)
        val available = (widthPx - paddingLeft - paddingRight).coerceAtLeast(minTile)
        val count = available / minTile
        return if (count < 1) 1 else count
    }

    private fun tileParams(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        val gap = UiCompat.dip(context, 8)
        lp.setMargins(gap, 0, gap, 0)
        return lp
    }

    private fun obtainTile(): AppTile {
        val recycled = if (pool.isNotEmpty()) pool.removeAt(pool.size - 1) else null
        val tile = recycled ?: AppTile(context)
        (tile.parent as? ViewGroup)?.removeView(tile)
        if (recycled == null) {
            tile.onClickAction = { clicked -> onTileClicked(clicked) }
            tile.onLongPressAction = { clicked -> onTileLongPressed(clicked) }
            tile.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val idx = indexOfTile(tile)
                    if (idx >= 0) {
                        lastIndex = idx
                        ensureVisible(tile)
                        listener?.onTileFocused(tile, idx)
                    }
                }
            }
        }
        return tile
    }

    private fun onTileClicked(tile: AppTile) {
        val idx = indexOfTile(tile)
        val entry = tile.entry ?: return
        if (lastIndex != idx) {
            lastIndex = idx  // touch safety net: visit() may fire before focus
            listener?.onTileFocused(tile, idx)
        }
        listener?.onTileClicked(entry)
    }

    private fun onTileLongPressed(tile: AppTile) {
        val entry = tile.entry ?: return
        listener?.onTileLongPressed(entry)
    }

    private fun indexOfTile(tile: AppTile): Int {
        for (i in activeTiles.indices) {
            if (activeTiles[i] === tile) return i
        }
        return -1
    }

    private fun ensureVisible(tile: AppTile) {
        val row = tile.parent as? View ?: return
        val top = content.top + row.top + tile.top
        val bottom = content.top + row.top + tile.bottom

        val minScroll = top - paddingTop
        val maxScroll = bottom - height + paddingBottom
        val target = when {
            scrollY < minScroll -> minScroll
            scrollY > maxScroll -> maxScroll
            else -> scrollY
        }
        val maxAllowed = maxOf(0, content.height + paddingTop + paddingBottom - height)
        val clamped = target.coerceIn(0, maxAllowed)
        if (clamped != scrollY) {
            smoothScrollTo(0, clamped)
        }
    }
}
