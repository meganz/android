package mega.privacy.android.app.utils

import mega.privacy.android.shared.resources.R as sharedR
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.domain.entity.AccountType
import timber.log.Timber
import mega.privacy.android.shared.resources.R as sharedResR

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
        showOverDiskQuotaPaywallWarning(MegaApplication.getInstance().currentActivity, false)
    }

    /**
     * Shows the ODQ Paywall warning. It will not be shown if it is already being displayed or if
     * the user is upgrading to PRO (i.e. in any place of the checkout process).
     *
     * @param loginFinished Indicates if the login process has already finished.
     */
    @JvmStatic
    fun showOverDiskQuotaPaywallWarning(activity: Activity?, loginFinished: Boolean) {
        // If app is doing login, the ODQ will be displayed at login finish
        if (activity is LoginActivity && !loginFinished) {
            return
        }

        if (activity is OverDiskQuotaPaywallActivity) {
            return
        }

        val intent = Intent(
            MegaApplication.getInstance().applicationContext,
            OverDiskQuotaPaywallActivity::class.java
        )
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        MegaApplication.getInstance().startActivity(intent)
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
        removeText.text = context.getString(R.string.context_remove_link_warning_text)

        val scaleW = Util.getScaleW(displayMetrics, displayMetrics.density)
        removeText.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, REMOVE_LINK_DIALOG_TEXT_SIZE * scaleW
        )

        builder.setView(dialogLayout)
            .setPositiveButton(context.getString(R.string.context_remove)) { _, _ ->
                onPositive()
            }
            .setNegativeButton(context.getString(sharedR.string.general_dialog_cancel_button), null)
            .create()
            .show()
    }

    /**
     * Shows a warning indicating something can not be added to an incoming share because the
     * owner is on storage over quota state.
     */
    @JvmStatic
    fun showForeignStorageOverQuotaWarningDialog(context: Context) {
        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setMessage(context.getString(R.string.warning_share_owner_storage_quota))
            .setPositiveButton(context.getString(sharedResR.string.general_ok), null)
            .setCancelable(false)
            .create()
            .show()
    }

    @JvmStatic
    fun askForCustomizedPlan(context: Context, myEmail: String?, accountType: AccountType) {
        Timber.d("askForCustomizedPlan")
        val body = StringBuilder()
        body.append(context.getString(R.string.subject_mail_upgrade_plan))
            .append("\n\n\n\n\n\n\n")
            .append(
                """${context.getString(R.string.settings_about_app_version)} v${
                    context.getString(
                        R.string.app_version
                    )
                }"""
            )
            .append(context.getString(R.string.user_account_feedback) + "  " + myEmail)

        when (accountType) {
            AccountType.FREE -> body.append(" (" + context.getString(R.string.my_account_free) + ")")
            AccountType.PRO_I -> body.append(" (" + context.getString(R.string.my_account_pro1) + ")")
            AccountType.PRO_II -> body.append(" (" + context.getString(R.string.my_account_pro2) + ")")
            AccountType.PRO_III -> body.append(" (" + context.getString(R.string.my_account_pro3) + ")")
            AccountType.PRO_LITE -> body.append(" (" + context.getString(R.string.my_account_prolite_feedback_email) + ")")
            else -> body.append(" (" + context.getString(R.string.my_account_free) + ")")
        }

        val emailAndroid = Constants.MAIL_SUPPORT
        val subject = context.getString(R.string.title_mail_upgrade_plan)
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
            .setTitle(activity.getString(R.string.error_file_not_available))
            .setMessage(activity.getString(R.string.error_takendown_file))
            .setNegativeButton(activity.getString(R.string.general_dismiss)) { _, _ ->
                if (!activity.isFinishing) activity.finish()
            }
            .create().apply {
                setCancelable(false)
                show()
            }
}
