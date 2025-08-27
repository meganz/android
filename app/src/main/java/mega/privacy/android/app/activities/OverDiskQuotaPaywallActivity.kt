package mega.privacy.android.app.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.IntentConstants.Companion.EXTRA_ASK_PERMISSIONS
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.overdisk.OverDiskQuotaPaywallViewModel
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.TimeUtils.DATE_LONG_FORMAT
import mega.privacy.android.app.utils.TimeUtils.formatDate
import mega.privacy.android.app.utils.TimeUtils.getHumanizedTimeMs
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import mega.privacy.android.navigation.ExtraConstant
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class OverDiskQuotaPaywallActivity : PasscodeActivity(), View.OnClickListener {

    @Inject
    @ApplicationScope
    lateinit var globalScope: CoroutineScope

    @Inject
    lateinit var isFirstLaunchUseCase: IsFirstLaunchUseCase

    private val viewModel: OverDiskQuotaPaywallViewModel by viewModels()

    private var timer: CountDownTimer? = null

    private var overDiskQuotaPaywallText: TextView? = null
    private var deletionWarningText: TextView? = null
    private var dismissButton: Button? = null
    private var upgradeButton: Button? = null
    private var proPlanNeeded: Int? = 0

    private var deadlineTs: Long = -1

    override fun shouldSetStatusBarTextColor() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets(WindowInsetsCompat.Type.navigationBars())
        super.onCreate(savedInstanceState)

        viewModel.requestStorageDetailIfNeeded()

        viewModel.getUserData()

        setContentView(R.layout.activity_over_disk_quota_paywall)

        overDiskQuotaPaywallText = findViewById(R.id.over_disk_quota_paywall_text)
        deletionWarningText = findViewById(R.id.over_disk_quota_paywall_deletion_warning)

        updateStrings()

        dismissButton = findViewById(R.id.dismiss_button)
        dismissButton?.setOnClickListener(this)

        upgradeButton = findViewById(R.id.upgrade_button)
        upgradeButton?.setOnClickListener(this)

        collectFlows()
    }

    private fun collectFlows() {
        collectFlow(viewModel.pricing) {
            getProPlanNeeded()
        }

        collectFlow(viewModel.monitorUpdateUserData) {
            updateStrings()
        }

        collectFlow(viewModel.monitorMyAccountUpdate) {
            updateStrings()
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.dismiss_button -> {
                Timber.i("Over Disk Quota Paywall warning dismissed")
                if (isTaskRoot) {
                    launchManagerActivity()
                }
                finish()
            }

            R.id.upgrade_button -> {
                Timber.i("Starting upgrade process after Over Disk Quota Paywall")

                val intent = Intent(applicationContext, ManagerActivity::class.java)
                    .putExtra(ExtraConstant.EXTRA_UPGRADE_ACCOUNT, true)
                    .putExtra(ExtraConstant.EXTRA_ACCOUNT_TYPE, proPlanNeeded)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun launchManagerActivity() {
        globalScope.launch {
            val askPermissions = isFirstLaunchUseCase()
            val intent = Intent(applicationContext, ManagerActivity::class.java)
                .putExtra(EXTRA_ASK_PERMISSIONS, askPermissions)
            startActivity(intent)
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
        val size = myAccountInfo.usedFormatted
        deadlineTs = megaApi.overquotaDeadlineTs

        if (warningsTs == null || warningsTs.size() == 0) {
            overDiskQuotaPaywallText?.text =
                getString(
                    R.string.over_disk_quota_paywall_text_no_warning_dates_info,
                    email, files.toString(), size, getProPlanNeeded()
                )
        } else if (warningsTs.size() == 1) {
            overDiskQuotaPaywallText?.text =
                resources.getQuantityString(
                    R.plurals.over_disk_quota_paywall_text,
                    1,
                    email,
                    formatDate(warningsTs.get(0), DATE_LONG_FORMAT, false, this),
                    files,
                    size,
                    getProPlanNeeded()
                )
        } else {
            var dates = String()
            val lastWarningIndex: Int = warningsTs.size() - 1
            for (i in 0 until lastWarningIndex) {
                if (dates.isEmpty()) {
                    dates += formatDate(
                        warningsTs.get(i),
                        DATE_LONG_FORMAT,
                        false,
                        this
                    )
                } else if (i != lastWarningIndex) {
                    dates =
                        dates + ", " + formatDate(warningsTs.get(i), DATE_LONG_FORMAT, false, this)
                }
            }

            overDiskQuotaPaywallText?.text =
                resources.getQuantityString(
                    R.plurals.over_disk_quota_paywall_text,
                    warningsTs.size(),
                    email,
                    dates,
                    formatDate(warningsTs.get(lastWarningIndex), DATE_LONG_FORMAT, false, this),
                    files,
                    size,
                    getProPlanNeeded()
                )
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
                text =
                    String.format(getString(R.string.over_disk_quota_paywall_deletion_warning_no_data))
            }

            TimeUnit.MILLISECONDS.toSeconds(time) <= 0 -> {
                text =
                    String.format(getString(R.string.over_disk_quota_paywall_deletion_warning_no_time_left))
            }

            else -> {
                text = String.format(
                    getString(R.string.over_disk_quota_paywall_deletion_warning),
                    getHumanizedTimeMs(time)
                )

                if (timer == null) {
                    timer = object : CountDownTimer(time, 1000) {
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
            text = text.replace(
                "[M]",
                "<font color='" + ColorUtils.getThemeColorHexString(
                    applicationContext,
                    com.google.android.material.R.attr.colorError
                ) + "'>"
            )
            text = text.replace("[/M]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting string")
        }

        deletionWarningText?.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Gets the PRO plan needed to be displayed in the ODQ Paywall warning depending on the storage
     * space used by the user.
     */
    private fun getProPlanNeeded(): String {
        val gb = 1073741824 // 1024(KB) * 1024(MB) * 1024(GB)
        val products = viewModel.pricing.value.products
        products.forEach {
            if (it.storage > myAccountInfo.usedStorage / gb) {
                proPlanNeeded = it.level
                return when (it.level) {
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