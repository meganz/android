package mega.privacy.android.app.utils

import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

class AlertsAndWarnings {

    companion object {
        /**
         * Shows the ODQ Paywall warning. It will not be shown if it is already being displayed or if
         * the user is upgrading to PRO (i.e. in any place of the checkout process).
         */
        @JvmStatic
        fun showOverDiskQuaotaPaywallWarning() {
            val app = MegaApplication.getInstance()
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
    }
}