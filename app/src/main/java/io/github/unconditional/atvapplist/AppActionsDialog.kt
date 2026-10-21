package io.github.unconditional.atvapplist

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/** Per-app actions shown over a focused tile (launch / force stop / info...). */
internal object AppActionsDialog {

    const val ACTION_LAUNCH = "launch"
    const val ACTION_FORCE_STOP = "force_stop"
    const val ACTION_APP_INFO = "app_info"
    const val ACTION_UNINSTALL = "uninstall"
    const val ACTION_HIDE = "hide"
    const val ACTION_UNHIDE = "unhide"
    const val ACTION_CANCEL = "cancel"

    fun show(
        activity: Activity,
        entry: AppEntry,
        prefs: Prefs,
        onAction: (String) -> Unit
    ): Dialog {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_actions)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)

        dialog.findViewById<TextView>(R.id.dialog_title).text = AppSorts.displayName(entry)

        val rows = dialog.findViewById<LinearLayout>(R.id.dialog_rows)

        addRow(activity, rows, activity.getString(R.string.action_launch)) {
            dialog.dismiss()
            onAction(ACTION_LAUNCH)
        }
        addRow(activity, rows, activity.getString(R.string.action_force_stop)) {
            dialog.dismiss()
            onAction(ACTION_FORCE_STOP)
        }
        addRow(activity, rows, activity.getString(R.string.action_app_info)) {
            dialog.dismiss()
            onAction(ACTION_APP_INFO)
        }
        addRow(activity, rows, activity.getString(R.string.action_uninstall)) {
            dialog.dismiss()
            onAction(ACTION_UNINSTALL)
        }
        val hidden = prefs.isHidden(entry.packageName)
        addRow(
            activity,
            rows,
            activity.getString(if (hidden) R.string.action_unhide else R.string.action_hide)
        ) {
            dialog.dismiss()
            onAction(if (hidden) ACTION_UNHIDE else ACTION_HIDE)
        }
        addRow(activity, rows, activity.getString(R.string.action_cancel)) {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            UiCompat.dip(activity, 520),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val first = rows.getChildAt(0)
        first?.post { first.requestFocus() }
        return dialog
    }

    /** Reusable focusable row for dialogs (actions and the hidden-apps list). */
    fun addRow(
        context: Context,
        container: LinearLayout,
        label: CharSequence,
        onClick: () -> Unit
    ) {
        val view = TextView(context)
        view.text = label
        view.textSize = 18f
        view.gravity = Gravity.CENTER_VERTICAL
        view.setPadding(
            UiCompat.dip(context, 22),
            UiCompat.dip(context, 15),
            UiCompat.dip(context, 22),
            UiCompat.dip(context, 15)
        )
        view.minimumHeight = UiCompat.dip(context, 52)
        view.background = UiCompat.drawable(context, R.drawable.action_item_bg)
        view.setTextColor(UiCompat.color(context, R.color.text_primary))
        view.isFocusable = true
        view.isClickable = true

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = UiCompat.dip(context, 6)
        container.addView(view, lp)
        view.setOnClickListener { onClick() }
    }
}
