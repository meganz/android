package mega.privacy.android.app.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants.Companion.EXTRA_ACCOUNT_TYPE
import mega.privacy.android.app.constants.IntentConstants.Companion.EXTRA_ASK_PERMISSIONS
import mega.privacy.android.app.constants.IntentConstants.Companion.EXTRA_UPGRADE_ACCOUNT
import mega.privacy.android.app.listeners.GetUserDataListener
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.DBUtil.callToAccountDetails
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.TimeUtils.*
import java.util.concurrent.TimeUnit

class OverDiskQuotaPaywallActivity : PinActivityLollipop(), View.OnClickListener{

    private var timer: CountDownTimer? = null

    private var scrollContentLayout: ScrollView? = null
    private var overDiskQuotaPaywallText: TextView? = null
    private var deletionWarningText: TextView? = null
    private var dismissButton: Button? = null
    private var upgradeButton: Button? = null
    private var proPlanNeeded: Int? = 0

    private var deadlineTs: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(updateAccountDetailsReceiver,
                IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS))
        registerReceiver(updateUserDataReceiver,
                IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_USER_DATA))

        // Request storage details only if is not already requested recently
        if (callToAccountDetails()) {
            megaApi.getSpecificAccountDetails(true, false, false)
        }

        megaApi.getUserData(GetUserDataListener(this))

        setContentView(R.layout.activity_over_disk_quota_paywall)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.status_bar_red_alert)

        scrollContentLayout = findViewById(R.id.scroll_content_layout)

        overDiskQuotaPaywallText = findViewById(R.id.over_disk_quota_paywall_text)
        deletionWarningText = findViewById(R.id.over_disk_quota_paywall_deletion_warning)

        updateStrings()

        dismissButton = findViewById(R.id.dismiss_button)
        dismissButton?.setOnClickListener(this)

        upgradeButton = findViewById(R.id.upgrade_button)
        upgradeButton?.setOnClickListener(this)
    }

    override fun onDestroy() {
        unregisterReceiver(updateAccountDetailsReceiver)
        unregisterReceiver(updateUserDataReceiver)
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.dismiss_button -> {
                logInfo("Over Disk Quota Paywall warning dismissed")
                if (isTaskRoot) {
                    var askPermissions: Boolean? = true
                    if (dbH?.preferences?.firstTime != null) {
                        askPermissions = dbH?.preferences?.firstTime?.toBoolean()
                    }
                    val intent = Intent(applicationContext, ManagerActivityLollipop::class.java)
                            .putExtra(EXTRA_ASK_PERMISSIONS, askPermissions)
                    startActivity(intent)
                }
                finish()
            }
            R.id.upgrade_button -> {
                logInfo("Starting upgrade process after Over Disk Quota Paywall")

                val intent = Intent(applicationContext, ManagerActivityLollipop::class.java)
                        .putExtra(EXTRA_UPGRADE_ACCOUNT, true)
                        .putExtra(EXTRA_ACCOUNT_TYPE, proPlanNeeded)
                startActivity(intent)
                finish()
            }
        }
    }

    private val updateAccountDetailsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateStrings()
        }
    }

    private val updateUserDataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateStrings()
        }
    }

    /**
     * Update the strings of the ODQ Paywall warning dialog with all the info needed.
     * NOTE: call this method any time the related info is updated.
     */
    private fun updateStrings() {

        val email = megaApi.myEmail
        val warningsTs = megaApi.overquotaWarningsTs
        val files = megaApi.numNodes
        val size = app.myAccountInfo.usedFormatted
        deadlineTs = megaApi.overquotaDeadlineTs

        if (warningsTs == null || warningsTs.size() == 0) {
            overDiskQuotaPaywallText?.text = getString(R.string.over_disk_quota_paywall_text_no_warning_dates_info,
                    email, files.toString(), size, getProPlanNeeded())
        }else if (warningsTs.size() == 1) {
            overDiskQuotaPaywallText?.text = resources.getQuantityString(R.plurals.over_disk_quota_paywall_text, 1,
                    email, formatDate(this, warningsTs.get(0), DATE_LONG_FORMAT, false), files, size, getProPlanNeeded())
        } else {
            var dates = String()
            val lastWarningIndex: Int = warningsTs.size() - 1
            for (i in 0 until lastWarningIndex) {
                if (dates.isEmpty()) {
                    dates += formatDate(this, warningsTs.get(i), DATE_LONG_FORMAT, false)
                } else if (i != lastWarningIndex) {
                    dates = dates + ", " + formatDate(this, warningsTs.get(i), DATE_LONG_FORMAT, false)
                }
            }

            overDiskQuotaPaywallText?.text = resources.getQuantityString(R.plurals.over_disk_quota_paywall_text, warningsTs.size(),
                    email, dates, formatDate(this, warningsTs.get(lastWarningIndex), DATE_LONG_FORMAT, false), files, size, getProPlanNeeded())
        }

        updateDeletionWarningText()
    }

    /**
     * Updates the deletion warning text of the ODQ Paywall warning dialog depending on the remaining time.
     * Uses a @see CountDownTimer to update the remaining time.
     */
    private fun updateDeletionWarningText() {
        var text: String
        val time = TimeUnit.SECONDS.toMillis(deadlineTs) - System.currentTimeMillis()

        when {
            deadlineTs < 0 -> {
                text = String.format(getString(R.string.over_disk_quota_paywall_deletion_warning_no_data))
            }
            TimeUnit.MILLISECONDS.toSeconds(time) <= 0 -> {
                text = String.format(getString(R.string.over_disk_quota_paywall_deletion_warning_no_time_left))
            }
            else -> {
                text = String.format(getString(R.string.over_disk_quota_paywall_deletion_warning), getHumanizedTimeMs(time))

                if (timer == null) {
                    timer = object: CountDownTimer(time, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            updateDeletionWarningText()
                        }

                        override fun onFinish() {
                            updateDeletionWarningText()
                            timer = null
                        }
                    }.start()
                }
            }
        }

        try {
            text = text.replace("[B]", "<b>")
            text = text.replace("[/B]", "</b>")
            text = text.replace("[M]", "<font color='" + ColorUtils.getThemeColorHexString(applicationContext, R.attr.colorError) + "'>")
            text = text.replace("[/M]", "</font>")
        } catch (e: Exception) {
            logWarning("Exception formatting string", e)
        }

        deletionWarningText?.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Gets the PRO plan needed to be displayed in the ODQ Paywall warning depending on the storage
     * space used by the user.
     */
    private fun getProPlanNeeded(): String {
        val plans = app.myAccountInfo.pricing ?: return getString(R.string.pro_account)

        val gb = 1073741824 // 1024(KB) * 1024(MB) * 1024(GB)

        for (i in 0 until plans.numProducts) {
            if (plans.getGBStorage(i) > app.myAccountInfo.usedStorage / gb) {
                proPlanNeeded = plans.getProLevel(i)
                return when(plans.getProLevel(i)) {
                    1 -> getString(R.string.pro1_account)
                    2 -> getString(R.string.pro2_account)
                    3 -> getString(R.string.pro3_account)
                    4 -> getString(R.string.prolite_account)
                    else -> getString(R.string.pro_account)
                }
            }
        }

        return getString(R.string.pro_account)
    }
}