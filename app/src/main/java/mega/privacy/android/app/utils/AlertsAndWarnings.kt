package mega.privacy.android.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.lollipop.LoginActivity
import mega.privacy.android.app.lollipop.megachat.ChatActivity
import mega.privacy.android.app.utils.StringResourcesUtils.getString

object AlertsAndWarnings {
    private const val REMOVE_LINK_DIALOG_TEXT_MARGIN_LEFT = 25
    private const val REMOVE_LINK_DIALOG_TEXT_MARGIN_TOP = 20
    private const val REMOVE_LINK_DIALOG_TEXT_MARGIN_RIGHT = 10
    private const val REMOVE_LINK_DIALOG_TEXT_SIZE = 15

    /**
     * Shows the ODQ Paywall warning. It will not be shown if it is already being displayed or if
     * the user is upgrading to PRO (i.e. in any place of the checkout process).
     */
    @JvmStatic
    fun showOverDiskQuotaPaywallWarning() {
        showOverDiskQuotaPaywallWarning(false)
    }

    /**
     * Shows the ODQ Paywall warning. It will not be shown if it is already being displayed or if
     * the user is upgrading to PRO (i.e. in any place of the checkout process).
     *
     * @param loginFinished Indicates if the login process has already finished.
     */
    @JvmStatic
    fun showOverDiskQuotaPaywallWarning(loginFinished: Boolean) {
        val app = MegaApplication.getInstance()

        // If app is doing login, the ODQ will be displayed at login finish
        if (app.currentActivity is LoginActivity && !loginFinished) {
            return
        }

        if (app.currentActivity is OverDiskQuotaPaywallActivity) {
            return
        }

        val intent = Intent(app.applicationContext, OverDiskQuotaPaywallActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        app.startActivity(intent)
    }

    /**
     * Shows a resume transfers warning.
     * It will be displayed if the queue of transfers is paused and a new chat upload starts.
     *
     * @param context current Context.
     */
    @JvmStatic
    fun showResumeTransfersWarning(context: Context) {
        if (context is BaseActivity
            && context.resumeTransfersWarning != null
            && context.resumeTransfersWarning.isShowing
        ) {
            return
        }

        val resumeTransfersDialogBuilder =
            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        resumeTransfersDialogBuilder.setTitle(getString(R.string.warning_resume_transfers))
            .setMessage(getString(R.string.warning_message_resume_transfers))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.button_resume_individual_transfer)) { dialog, _ ->
                MegaApplication.getInstance().megaApi.pauseTransfers(false)

                if (context is ChatActivity) {
                    context.updatePausedUploadingMessages()
                }

                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.general_cancel)) { dialog, _ ->
                dialog.dismiss()
            }.setOnDismissListener {
                if (context is BaseActivity) {
                    context.setIsResumeTransfersWarningShown(false)
                }
            }

        if (context is BaseActivity) {
            context.setIsResumeTransfersWarningShown(true)
            context.resumeTransfersWarning = resumeTransfersDialogBuilder.create()
        }

        resumeTransfersDialogBuilder.show()
    }

    /**
     * Shows a confirm remove link alert dialog.
     *
     * @param context current Context
     * @param onPositive callback when positive button is clicked
     */
    @JvmStatic
    fun showConfirmRemoveLinkDialog(context: Context, onPositive: () -> Unit) {
        val builder =
            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        val dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_link, null)

        dialogLayout.findViewById<TextView>(R.id.dialog_link_link_url).isVisible = false
        dialogLayout.findViewById<TextView>(R.id.dialog_link_link_key).isVisible = false
        dialogLayout.findViewById<TextView>(R.id.dialog_link_symbol).isVisible = false

        val displayMetrics = context.resources.displayMetrics

        val removeText = dialogLayout.findViewById<TextView>(R.id.dialog_link_text_remove)
        (removeText.layoutParams as RelativeLayout.LayoutParams).setMargins(
            Util.scaleWidthPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_LEFT, displayMetrics),
            Util.scaleHeightPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_TOP, displayMetrics),
            Util.scaleWidthPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_RIGHT, displayMetrics),
            0
        )
        removeText.visibility = View.VISIBLE
        removeText.text = getString(R.string.context_remove_link_warning_text)

        val scaleW = Util.getScaleW(displayMetrics, displayMetrics.density)
        removeText.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, REMOVE_LINK_DIALOG_TEXT_SIZE * scaleW
        )

        builder.setView(dialogLayout)
            .setPositiveButton(getString(R.string.context_remove)) { _, _ ->
                onPositive()
            }
            .setNegativeButton(getString(R.string.general_cancel), null)
            .create()
            .show()
    }

    @JvmStatic
    fun showSaveToDeviceConfirmDialog(activity: Activity): (message: String, onConfirmed: (Boolean) -> Unit) -> Unit {
        return { message, onConfirmed ->
            val customView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_confirm_with_not_show_again, null)
            val notShowAgain = customView.findViewById<CheckBox>(R.id.not_show_again)

            MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setView(customView)
                .setMessage(message)
                .setPositiveButton(
                    getString(R.string.general_save_to_device)
                ) { _, _ ->
                    onConfirmed(notShowAgain.isChecked)
                }
                .setNegativeButton(getString(R.string.general_cancel)) { _, _ -> }
                .create()
                .show()
        }
    }

    /**
     * Shows a warning indicating something can not be added to an incoming share because the
     * owner is on storage over quota state.
     */
    @JvmStatic
    fun showForeignStorageOverQuotaWarningDialog(context: Context) {
        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setMessage(getString(R.string.warning_share_owner_storage_quota))
            .setPositiveButton(getString(R.string.general_ok), null)
            .setCancelable(false)
            .create()
            .show()
    }

    @JvmStatic
    fun askForCustomizedPlan(context: Context, myEmail:String, accountType: Int) {
        LogUtil.logDebug("askForCustomizedPlan")
        val body = StringBuilder()
        body.append(getString(R.string.subject_mail_upgrade_plan))
            .append("\n\n\n\n\n\n\n")
            .append("""${getString(R.string.settings_about_app_version)} v${getString(R.string.app_version)}""")
            .append(getString(R.string.user_account_feedback).toString() + "  " + myEmail)

        when (accountType) {
            0 -> body.append(" (" + getString(R.string.my_account_free) + ")")
            1 -> body.append(" (" + getString(R.string.my_account_pro1) + ")")
            2 -> body.append(" (" + getString(R.string.my_account_pro2) + ")")
            3 -> body.append(" (" + getString(R.string.my_account_pro3) + ")")
            4 -> body.append(" (" + getString(R.string.my_account_prolite_feedback_email) + ")")
            else -> body.append(" (" + getString(R.string.my_account_free) + ")")
        }

        val emailAndroid = Constants.MAIL_SUPPORT
        val subject = getString(R.string.title_mail_upgrade_plan)
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$emailAndroid"))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body.toString())

        context.startActivity(Intent.createChooser(emailIntent, " "))
    }

    /**
     * Shows a taken down alert.
     *
     * @param activity   Required to create the dialog and finish the activity.
     */
    @JvmStatic
    fun showTakenDownAlert(activity: Activity): AlertDialog =
        MaterialAlertDialogBuilder(activity)
            .setTitle(getString(R.string.general_not_available))
            .setMessage(getString(R.string.error_download_takendown_node))
            .setNegativeButton(getString(R.string.general_dismiss)) { _, _ ->
                if (!activity.isFinishing) activity.finish()
            }
            .create().apply {
                setCancelable(false)
                show()
            }

}
