package io.github.unconditional.atvapplist

import java.text.Collator
import java.util.Locale

enum class SortMode {
    NAME,
    LAST_USED,
    INSTALLED
}

/** Comparators for [SortMode]; never-used / unknown entries group at the end, alphabetical. */
internal object AppSorts {

    private val collator: Collator by lazy { Collator.getInstance(Locale.getDefault()) }

    fun displayName(entry: AppEntry): String =
        entry.label.ifBlank { entry.packageName }

    fun compare(a: AppEntry, b: AppEntry, mode: SortMode): Int {
        val name = collator.compare(displayName(a), displayName(b))
        return when (mode) {
            SortMode.NAME -> name
            SortMode.LAST_USED -> {
                val diff = b.lastUsed - a.lastUsed // descending by used time
                tieBreak(diff, name)
            }
            SortMode.INSTALLED -> {
                val diff = b.firstInstallTime - a.firstInstallTime
                tieBreak(diff, name)
            }
        }
    }

    private fun tieBreak(diff: Long, name: Int): Int = when {
        diff > 0 -> 1   // b is more recent -> b (strictly) before a -> positive
        diff < 0 -> -1
        else -> name
    }
}
