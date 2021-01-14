package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop

class AlertsAndWarnings {

    companion object {
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
            if (app.currentActivity is LoginActivityLollipop && !loginFinished) {
                return
            }

            if (app.currentActivity is OverDiskQuotaPaywallActivity) {
                return
            }

            if (app.currentActivity is ManagerActivityLollipop) {
                val upAFL = (app.currentActivity as ManagerActivityLollipop).upgradeAccountFragment
                if (upAFL != null && upAFL.isVisible) {
                    return
                }
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
                    && context.resumeTransfersWarning.isShowing) {
                return
            }

            val resumeTransfersDialogBuilder = AlertDialog.Builder(context, R.style.ResumeTransfersWarning)

            resumeTransfersDialogBuilder.setTitle(R.string.warning_resume_transfers)
                        .setMessage(R.string.warning_message_resume_transfers)
                    .setCancelable(false)
                    .setPositiveButton(R.string.button_resume_individual_transfer) { dialog, _ ->
                        MegaApplication.getInstance().megaApi.pauseTransfers(false)

                        if (context is ChatActivityLollipop) {
                            context.updatePausedUploadingMessages()
                        }

                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.general_cancel) { dialog, _ ->
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
    }
}