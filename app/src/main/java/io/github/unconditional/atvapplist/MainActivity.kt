package io.github.unconditional.atvapplist

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

internal class MainActivity : Activity(), AppGrid.Listener {

    private lateinit var grid: AppGrid
    private lateinit var countView: TextView
    private lateinit var emptyView: TextView

    private lateinit var prefs: Prefs
    private lateinit var usageStore: UsageStore
    private lateinit var repository: AppRepository

    private var sortMode: SortMode = SortMode.NAME
    private var showSystem = false
    private var allApps: List<AppEntry> = emptyList()
    private var focusedEntry: AppEntry? = null
    private var pendingFocusPackage: String? = null
    private var destroyed = false

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mainHandler.post { reloadApps() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        grid = findViewById(R.id.grid)
        countView = findViewById(R.id.count_view)
        emptyView = findViewById(R.id.empty_view)

        prefs = Prefs(this)
        usageStore = UsageStore(this)
        repository = AppRepository(applicationContext)

        sortMode = prefs.sortMode
        showSystem = prefs.showSystemApps

        grid.listener = this
        setupChips()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(packageChangeReceiver, filter)
        }
    }

    override fun onStop() {
        try {
            unregisterReceiver(packageChangeReceiver)
        } catch (e: Exception) {
            // not registered
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        reloadApps()
    }

    override fun onDestroy() {
        destroyed = true
        executor.shutdownNow()
        super.onDestroy()
    }

    // ---- AppGrid.Listener -------------------------------------------------

    override fun onTileFocused(tile: AppTile, index: Int) {
        focusedEntry = tile.entry
    }

    override fun onTileClicked(entry: AppEntry) {
        pendingFocusPackage = entry.packageName
        launch(entry)
    }

    override fun onTileLongPressed(entry: AppEntry) {
        showActionsDialog(entry)
    }

    // ---- chips ------------------------------------------------------------

    private fun setupChips() {
        findViewById<TextView>(R.id.chip_alpha)?.setOnClickListener { setSortMode(SortMode.NAME) }
        findViewById<TextView>(R.id.chip_last_used)?.setOnClickListener { setSortMode(SortMode.LAST_USED) }
        findViewById<TextView>(R.id.chip_installed)?.setOnClickListener { setSortMode(SortMode.INSTALLED) }
        findViewById<TextView>(R.id.chip_settings)?.setOnClickListener { openSettings() }
        updateChipStates()
    }

    private fun updateChipStates() {
        findViewById<TextView>(R.id.chip_alpha)?.isSelected = sortMode == SortMode.NAME
        findViewById<TextView>(R.id.chip_last_used)?.isSelected = sortMode == SortMode.LAST_USED
        findViewById<TextView>(R.id.chip_installed)?.isSelected = sortMode == SortMode.INSTALLED
    }

    private fun setSortMode(mode: SortMode) {
        sortMode = mode
        prefs.sortMode = mode
        updateChipStates()
        refreshGrid()
    }

    private fun openSettings() {
        try {
            startActivity(Intent(this, SettingsActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // ---- actions ----------------------------------------------------------

    private fun showActionsDialog(entry: AppEntry) {
        pendingFocusPackage = entry.packageName
        val dialog = AppActionsDialog.show(this, entry, prefs) { key ->
            handleAction(entry, key)
        }
        dialog.setOnDismissListener { focusPackageAfterDialog(entry.packageName) }
    }

    private fun focusPackageAfterDialog(pkg: String) {
        grid.post {
            if (destroyed) return@post
            val idx = grid.indexOfPackage(pkg)
            if (idx >= 0) {
                grid.requestFocusTile(idx)
            }
        }
    }

    private fun handleAction(entry: AppEntry, key: String) {
        when (key) {
            AppActionsDialog.ACTION_LAUNCH -> {
                pendingFocusPackage = entry.packageName
                launch(entry)
            }
            AppActionsDialog.ACTION_FORCE_STOP -> openAppDetails(entry.packageName, stopHint = true)
            AppActionsDialog.ACTION_APP_INFO -> openAppDetails(entry.packageName, stopHint = false)
            AppActionsDialog.ACTION_UNINSTALL -> requestUninstall(entry.packageName)
            AppActionsDialog.ACTION_HIDE -> {
                prefs.hide(entry.packageName)
                reloadApps()
            }
            AppActionsDialog.ACTION_UNHIDE -> {
                prefs.unhide(entry.packageName)
                reloadApps()
            }
            else -> {
                // cancel / unknown: nothing to do
            }
        }
    }

    private fun launch(entry: AppEntry) {
        try {
            usageStore.record(entry.packageName)
            val intent = Intent(Intent.ACTION_MAIN).apply {
                component = entry.componentName
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Force stop itself cannot be triggered from a normal app, so we open the
     * system App info screen where the user can trigger it with one more OK.
     */
    private fun openAppDetails(pkg: String, stopHint: Boolean) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
        }
        if (stopHint) {
            Toast.makeText(this, R.string.force_stop_hint, Toast.LENGTH_LONG).show()
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestUninstall(pkg: String) {
        try {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.uninstall_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // ---- data flow --------------------------------------------------------

    private fun reloadApps() {
        if (destroyed) return
        executor.execute {
            val systemStats = usageStore.querySystemUsageStats()
            val fresh = repository.loadApps { pkg ->
                val own = usageStore.lastUsed(pkg)
                val sys = systemStats[pkg] ?: 0L
                if (sys > own) sys else own
            }
            if (destroyed) return@execute
            mainHandler.post {
                if (destroyed) return@post
                allApps = fresh
                refreshGrid()
            }
        }
    }

    private fun refreshGrid() {
        val hidden = prefs.hiddenPackages()
        val visible = ArrayList<AppEntry>(allApps.size)
        for (app in allApps) {
            if (app.packageName in hidden) continue
            if (!showSystem && app.isSystem) continue
            visible.add(app)
        }
        visible.sortWith { a, b -> AppSorts.compare(a, b, sortMode) }

        val wanted = pendingFocusPackage
        if (wanted == null) {
            val currentFocusTile = window.currentFocus as? AppTile
            focusedEntry = currentFocusTile?.entry
        }
        val focusWanted = wanted ?: focusedEntry?.packageName
        pendingFocusPackage = null

        countView.text = getString(R.string.app_count, visible.size)
        emptyView.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE

        grid.setApps(visible) { entry -> metaFor(entry) }
        restoreFocus(focusWanted)
    }

    private fun metaFor(entry: AppEntry): CharSequence? {
        if (sortMode != SortMode.LAST_USED) return null
        return if (entry.lastUsed > 0L) {
            DateUtils.getRelativeTimeSpanString(entry.lastUsed)
        } else {
            getString(R.string.never_used)
        }
    }

    private fun restoreFocus(wanted: String?) {
        grid.post { restoreFocusNow(wanted, allowRetry = true) }
    }

    private fun restoreFocusNow(wanted: String?, allowRetry: Boolean) {
        if (destroyed) return
        if (grid.width <= 0 && allowRetry) {
            grid.post { restoreFocusNow(wanted, allowRetry = false) }
            return
        }
        val idx = wanted?.let { grid.indexOfPackage(it) } ?: -1
        if (idx >= 0) {
            grid.requestFocusTile(idx)
        } else if (window.currentFocus == null && grid.tileCount > 0) {
            grid.requestFocusTile(0)
        }
    }
}
