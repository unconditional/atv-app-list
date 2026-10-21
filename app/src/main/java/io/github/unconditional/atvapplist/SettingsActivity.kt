package io.github.unconditional.atvapplist

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

internal class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var usageStore: UsageStore

    private var usageValueView: TextView? = null
    private var hiddenValueView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)
        usageStore = UsageStore(this)
        buildRows()
    }

    override fun onResume() {
        super.onResume()
        usageValueView?.text = usageAccessText()
    }

    private fun buildRows() {
        val container = findViewById<LinearLayout>(R.id.settings_rows)
        container.removeAllViews()

        addRow(
            container,
            getString(R.string.settings_show_system),
            systemAppsText()
        ) { valueView ->
            prefs.showSystemApps = !prefs.showSystemApps
            valueView.text = systemAppsText()
        }

        hiddenValueView = addRow(
            container,
            getString(R.string.settings_hidden),
            hiddenAppsText()
        ) { _ ->
            showHiddenAppsDialog()
        }

        usageValueView = addRow(
            container,
            getString(R.string.settings_usage_access),
            usageAccessText()
        ) { _ ->
            // Always route to the system screen: also works for revoking.
            openUsageAccessSettings()
        }

        addRow(
            container,
            getString(R.string.settings_about),
            versionText()
        ) { _ ->
            showAbout()
        }
    }

    private fun addRow(
        container: LinearLayout,
        title: CharSequence,
        value: CharSequence,
        onClick: (TextView) -> Unit
    ): TextView {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = UiCompat.drawable(this, R.drawable.row_bg)
        row.isFocusable = true
        row.isClickable = true
        row.minimumHeight = UiCompat.dip(this, 64)
        row.setPadding(
            UiCompat.dip(this, 22),
            UiCompat.dip(this, 14),
            UiCompat.dip(this, 22),
            UiCompat.dip(this, 14)
        )

        val titleView = TextView(this)
        titleView.text = title
        titleView.textSize = 18f
        titleView.setTextColor(UiCompat.color(this, R.color.text_primary))
        row.addView(
            titleView,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        val valueView = TextView(this)
        valueView.text = value
        valueView.textSize = 15f
        valueView.setTextColor(UiCompat.color(this, R.color.text_dim))
        row.addView(
            valueView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val rowParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rowParams.topMargin = UiCompat.dip(this, 10)
        container.addView(row, rowParams)

        row.setOnClickListener { onClick(valueView) }
        return valueView
    }

    private fun systemAppsText(): String =
        getString(if (prefs.showSystemApps) R.string.on else R.string.off)

    private fun hiddenAppsText(): String =
        getString(R.string.hidden_count, prefs.hiddenPackages().size)

    private fun usageAccessText(): String =
        getString(
            if (usageStore.isUsageAccessGranted()) R.string.usage_access_granted
            else R.string.usage_access_open
        )

    private fun openUsageAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHiddenAppsDialog() {
        if (prefs.hiddenPackages().isEmpty()) {
            Toast.makeText(this, R.string.none_hidden, Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_actions)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)

        dialog.findViewById<TextView>(R.id.dialog_title).text =
            getString(R.string.settings_hidden)
        val rows = dialog.findViewById<LinearLayout>(R.id.dialog_rows)

        fun refreshValues() {
            hiddenValueView?.text = hiddenAppsText()
        }

        fun rebuildRows() {
            rows.removeAllViews()
            val hidden = prefs.hiddenPackages()
            if (hidden.isEmpty()) {
                dialog.dismiss()
                return
            }
            for (pkg in hidden.toSortedSet()) {
                AppActionsDialog.addRow(this, rows, labelFor(pkg)) {
                    prefs.unhide(pkg)
                    refreshValues()
                    rebuildRows()
                }
            }
            AppActionsDialog.addRow(this, rows, getString(R.string.action_unhide_all)) {
                prefs.unhideAll()
                refreshValues()
                dialog.dismiss()
            }
        }

        rebuildRows()
        dialog.show()
        dialog.window?.setLayout(
            UiCompat.dip(this, 520),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        rows.getChildAt(0)?.post { rows.getChildAt(0).requestFocus() }
        dialog.setOnDismissListener { refreshValues() }
    }

    private fun labelFor(pkg: String): String = try {
        packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.about_body, versionText()))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun versionText(): String = try {
        val info = packageManager.getPackageInfo(packageName, 0)
        "" + info.versionName + " (" + info.versionCode + ")"
    } catch (e: Exception) {
        ""
    }
}
