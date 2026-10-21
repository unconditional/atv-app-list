package io.github.unconditional.atvapplist

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process

/**
 * Tracks when this launcher started each app (always available) and, when the
 * user grants "Usage access", merges the system's own per-app last-used times.
 */
internal class UsageStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("usage_store", Context.MODE_PRIVATE)

    fun lastUsed(packageName: String): Long = prefs.getLong(packageName, 0L)

    fun record(packageName: String, time: Long = System.currentTimeMillis()) {
        if (time > lastUsed(packageName)) {
            prefs.edit().putLong(packageName, time).apply()
        }
    }

    fun isUsageAccessGranted(): Boolean = try {
        val manager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            manager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            manager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }

    /** One bulk query of system last-used times; empty when access is missing. */
    fun querySystemUsageStats(): Map<String, Long> {
        if (!isUsageAccessGranted()) return emptyMap()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        return try {
            val end = System.currentTimeMillis()
            val begin = end - USAGE_WINDOW_MS
            val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begin, end)
                ?: return emptyMap()
            val result = HashMap<String, Long>()
            for (stat in stats) {
                val pkg = stat.packageName ?: continue
                val usedAt = stat.lastTimeUsed
                if (usedAt > 0L && usedAt > (result[pkg] ?: 0L)) {
                    result[pkg] = usedAt
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val USAGE_WINDOW_MS = 45L * 24L * 60L * 60L * 1000L
    }
}
