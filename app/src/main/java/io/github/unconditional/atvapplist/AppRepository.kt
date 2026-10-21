package io.github.unconditional.atvapplist

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache

internal class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Icons are cached under a byte budget, not an entry count: sideloaded
     * apps can carry very large launcher bitmaps and must not balloon the
     * heap on low-RAM TV devices.
     */
    private val iconCache = object : LruCache<String, Drawable>(ICON_CACHE_MAX_BYTES) {
        override fun sizeOf(key: String, value: Drawable): Int {
            val bitmap = (value as? BitmapDrawable)?.bitmap
            return bitmap?.byteCount ?: ICON_CACHE_FALLBACK_ENTRY_BYTES
        }
    }

    /**
     * Lists every visible launchable app. An app is launchable when it exposes a
     * LAUNCHER (phone-style) or LEANBACK_LAUNCHER (TV-style) main activity. Apps
     * appearing in both get the leanback component preferred.
     */
    fun loadApps(lastUsedOf: (String) -> Long): List<AppEntry> {
        val byPackage = HashMap<String, AppEntry>()

        collect(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) },
            byPackage,
            true,
            lastUsedOf
        )
        collect(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            byPackage,
            false,
            lastUsedOf
        )

        val self = context.packageName
        return byPackage.values.filter { it.packageName != self }
    }

    private fun collect(
        intent: Intent,
        into: MutableMap<String, AppEntry>,
        fromLeanback: Boolean,
        lastUsedOf: (String) -> Long
    ) {
        val results: List<ResolveInfo> = try {
            packageManager.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        for (ri in results) {
            val info = ri.activityInfo ?: continue
            val pkg = info.packageName ?: continue
            if (pkg == context.packageName) continue
            val className = info.name?.takeIf { it.isNotBlank() } ?: continue

            val rawLabel = try {
                ri.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                null
            }
            val label = rawLabel?.trim()?.takeIf { it.isNotEmpty() } ?: pkg

            val firstInstallTime = try {
                packageManager.getPackageInfo(pkg, 0)?.firstInstallTime ?: 0L
            } catch (e: Exception) {
                0L
            }

            val flags = info.applicationInfo?.flags ?: 0
            val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val entry = AppEntry(
                packageName = pkg,
                componentName = ComponentName(pkg, className),
                label = label,
                icon = loadIcon(pkg, ri),
                firstInstallTime = firstInstallTime,
                lastUsed = lastUsedOf(pkg),
                isSystem = isSystem,
                fromLeanback = fromLeanback
            )

            val existing = into[pkg]
            if (existing == null || (fromLeanback && !existing.fromLeanback)) {
                into[pkg] = entry
            }
        }
    }

    private fun loadIcon(pkg: String, ri: ResolveInfo): Drawable? {
        iconCache.get(pkg)?.let { return it }
        val drawable = try {
            ri.loadIcon(packageManager)
        } catch (e: Exception) {
            null
        }
        if (drawable != null) {
            iconCache.put(pkg, drawable)
        }
        return drawable
    }

    companion object {
        /** 16 MiB budget shared by all launcher icons. */
        private const val ICON_CACHE_MAX_BYTES = 16 * 1024 * 1024

        /** Assumed cost of drawables whose size cannot be measured (vectors, etc.). */
        private const val ICON_CACHE_FALLBACK_ENTRY_BYTES = 16 * 1024
    }
}
