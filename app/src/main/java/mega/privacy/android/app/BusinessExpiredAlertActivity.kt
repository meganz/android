package mega.privacy.android.app

import mega.privacy.android.shared.resources.R as sharedR
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityBusinessExpiredAlertBinding
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.isScreenInPortrait

/**
 * The class for showing the business or pro flexi expired alert
 */
@AndroidEntryPoint
class BusinessExpiredAlertActivity : PasscodeActivity() {

    private val binding: ActivityBusinessExpiredAlertBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityBusinessExpiredAlertBinding.inflate(layoutInflater)
    }

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets(WindowInsetsCompat.Type.navigationBars())
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val expiredLayoutParams =
            binding.expiredImageLayout.layoutParams as LinearLayout.LayoutParams
        expiredLayoutParams.height = if (isScreenInPortrait(this)) {
            dp2px(IMAGE_HEIGHT_PORTRAIT, outMetrics)
        } else {
            dp2px(IMAGE_HEIGHT_LANDSCAPE, outMetrics)
        }
        binding.expiredImageLayout.layoutParams = expiredLayoutParams

        val expiredImageParams = binding.expiredImage.layoutParams as RelativeLayout.LayoutParams
        if (megaApi.isBusinessAccount && myAccountInfo.accountType == Constants.PRO_FLEXI) {
            binding.expiredImageLayout.background =
                ContextCompat.getDrawable(this, R.drawable.gradient_business_admin_expired_bg)
            expiredImageParams.addRule(RelativeLayout.CENTER_IN_PARENT)
            binding.expiredImage.setImageResource(
                if (isScreenInPortrait(this)) {
                    R.drawable.ic_account_expired_admin_portrait
                } else {
                    R.drawable.ic_account_expired_admin_landscape
                }
            )
            binding.deactivatedAccountTitle.text =
                getString(sharedR.string.account_pro_flexi_account_deactivated_dialog_title)
            binding.expiredText.text =
                getString(sharedR.string.account_pro_flexi_account_deactivated_dialog_body)
            binding.expiredSubtext.isVisible = false
            binding.expiredDismissButton.text = getString(R.string.general_ok)
        } else if (megaApi.isMasterBusinessAccount) {
            binding.expiredImageLayout.background =
                ContextCompat.getDrawable(this, R.drawable.gradient_business_admin_expired_bg)
            expiredImageParams.addRule(RelativeLayout.CENTER_IN_PARENT)
            binding.expiredImage.setImageResource(
                if (isScreenInPortrait(this)) {
                    R.drawable.ic_account_expired_admin_portrait
                } else {
                    R.drawable.ic_account_expired_admin_landscape
                }
            )
            binding.expiredText.text =
                getString(R.string.account_business_account_deactivated_dialog_admin_body)
            binding.expiredSubtext.isVisible = false
        } else {
            binding.expiredImageLayout.background =
                ContextCompat.getDrawable(this, R.drawable.gradient_business_user_expired_bg)
            expiredImageParams.apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }
            binding.expiredImage.setImageResource(
                if (isScreenInPortrait(this)) {
                    R.drawable.ic_account_expired_user_portrait
                } else {
                    R.drawable.ic_account_expired_user_landscape
                }
            )
            binding.expiredText.text =
                getString(R.string.account_business_account_deactivated_dialog_sub_user_body)
            binding.expiredSubtext.isVisible = true
        }
        binding.expiredImage.layoutParams = expiredImageParams

        binding.expiredDismissButton.setOnClickListener {
            finish()
        }
    }

    /**
     * finish
     */
    override fun finish() {
        myAccountInfo.isBusinessAlertShown = false
        super.finish()
    }

    companion object {
        private const val IMAGE_HEIGHT_PORTRAIT = 284f
        private const val IMAGE_HEIGHT_LANDSCAPE = 136f
    }
}