package mega.privacy.android.app.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.GetUserDataListener
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.TimeUtils.*

class OverDiskQuotaPaywallActivity : PinActivityLollipop(), View.OnClickListener{

    private var scrollContentLayout: ScrollView? = null
    private var overDiskQuotaPaywallText: TextView? = null
    private var deletionWarningText: TextView? = null
    private var dismissButton: Button? = null
    private var upgradeButton: Button? = null
    private var proPlanNeeded: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalBroadcastManager.getInstance(this).registerReceiver(updateUserDataReceiver,
                IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_USER_DATA))

        megaApi.getUserData(GetUserDataListener(this))

        setContentView(R.layout.activity_over_disk_quota_paywall)
        window.statusBarColor = resources.getColor(R.color.status_bar_red_alert)

        scrollContentLayout = findViewById(R.id.scroll_content_layout)

        overDiskQuotaPaywallText = findViewById(R.id.over_disk_quota_paywall_text)
        deletionWarningText = findViewById(R.id.over_disk_quota_paywall_deletion_warning)

        updateStrings()

        dismissButton = findViewById(R.id.dismiss_button)
        dismissButton?.setOnClickListener(this)

        upgradeButton = findViewById(R.id.upgrade_button)
        upgradeButton?.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.dismiss_button -> {
                logInfo("Over Disk Quota Paywall warning dismissed")
                finish()
            }
            R.id.upgrade_button -> {
                logInfo("Starting upgrade process after Over Disk Quota Paywall")

                val intent: Intent? = Intent(applicationContext, ManagerActivityLollipop::class.java)
                intent?.putExtra("upgradeAccount", true)
                intent?.putExtra("accountType", proPlanNeeded)
                startActivity(intent)
                finish()
            }
        }
    }

    private val updateUserDataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateStrings()
        }
    }

    private fun updateStrings() {

        val email = megaApi.myEmail
        val warningsTs = megaApi.overquotaWarningsTs
        val files = megaApi.numNodes
        val size = app.myAccountInfo.usedFormatted
        val deadlineTs = megaApi.overquotaDeadlineTs

        if (warningsTs.size() == 1) {
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

        var text = String.format(getString(R.string.over_disk_quota_paywall_deletion_warning), getHumanizedTimestamp(deadlineTs))
        try {
            text = text.replace("[B]", "<b>")
            text = text.replace("[/B]", "</b>")
            text = text.replace("[M]", "<font color='" + resources.getColor(R.color.mega) + "'>")
            text = text.replace("[/M]", "</font>")
        } catch (e: Exception) {
            logWarning("Exception formatting string", e)
        }

        val result: Spanned
        result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(text)
        }
        deletionWarningText?.text = result
    }

    private fun getProPlanNeeded(): String {
        val plans = app.myAccountInfo.pricing

        val gb = 1073741824 // 1024(KB) * 1024(MB) * 1024(GB)

        for (i in 0 until plans.numProducts) {
            if (plans.getGBStorage(i) > app.myAccountInfo.usedStorage / gb) {
                proPlanNeeded = plans.getProLevel(i)
                return when(plans.getProLevel(i)) {
                    1 -> "PRO I"
                    2 -> "PRO II"
                    3 -> "PRO III"
                    4 -> "PRO Lite"
                    else -> "PRO"
                }
            }
        }

        return "PRO"
    }
}