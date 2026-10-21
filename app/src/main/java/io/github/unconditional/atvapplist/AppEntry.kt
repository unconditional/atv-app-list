package io.github.unconditional.atvapplist

import android.content.ComponentName
import android.graphics.drawable.Drawable

/** Immutable description of a launchable app. */
data class AppEntry(
    val packageName: String,
    val componentName: ComponentName,
    val label: String,
    val icon: Drawable?,
    val firstInstallTime: Long,
    val lastUsed: Long,
    val isSystem: Boolean,
    val fromLeanback: Boolean
)
