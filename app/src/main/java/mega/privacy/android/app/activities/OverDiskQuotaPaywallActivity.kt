package mega.privacy.android.app.activities

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.LogUtil.logInfo
import mega.privacy.android.app.utils.LogUtil.logWarning

class OverDiskQuotaPaywallActivity : PinActivityLollipop(), View.OnClickListener{

    private var scrollContentLayout: ScrollView? = null
    private var overDiskQuotaPaywallText: TextView? = null
    private var deletionWarningText: TextView? = null
    private var dismissButton: Button? = null
    private var upgradeButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_over_disk_quota_paywall)
        window.statusBarColor = resources.getColor(R.color.status_bar_red_alert)

        scrollContentLayout = findViewById(R.id.scroll_content_layout)

        overDiskQuotaPaywallText = findViewById(R.id.over_disk_quota_paywall_text)
        overDiskQuotaPaywallText?.text = String.format(getString(R.string.over_disk_quota_paywall_text),
                "EMAIL", "DATES", "FILES", "SIZE")

        deletionWarningText = findViewById(R.id.over_disk_quota_paywall_deletion_warning)
        var text = String.format(getString(R.string.over_disk_quota_paywall_deletion_warning), "DAYS")
        try {
            text = text.replace("[B]", "<b>")
            text = text.replace("[/B]", "</b>")
            text = text.replace("[M]", "<font color='"+ resources.getColor(R.color.mega) +"'>")
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
                finish()
            }
        }
    }
}