package mega.privacy.android.app

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityBusinessExpiredAlertBinding
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.isScreenInPortrait
import timber.log.Timber

/**
 * The class for showing the business expired alert
 */
@AndroidEntryPoint
class BusinessExpiredAlertActivity : PasscodeActivity() {

    private val binding: ActivityBusinessExpiredAlertBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityBusinessExpiredAlertBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        if (megaApi.isMasterBusinessAccount) {
            binding.expiredImageLayout.background =
                ContextCompat.getDrawable(this, R.drawable.gradient_business_admin_expired_bg)
            expiredImageParams.addRule(RelativeLayout.CENTER_IN_PARENT)
            binding.expiredImage.setImageResource(
                if (isScreenInPortrait(this)) {
                    R.drawable.ic_account_expired_admin_portrait
                } else {
                    R.drawable.ic_account_expired_admin_portrait
                }
            )
            binding.expiredText.text = getString(R.string.expired_admin_business_text)
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
            var expiredString = getString(R.string.expired_user_business_text)
            runCatching {
                expiredString = expiredString.replace(
                    "[B]",
                    "<b><font color=\'${
                        getColorHexString(
                            this, R.color.grey_900_grey_100
                        )
                    }\'>"
                ).replace("[/B]", "</font></b>")
            }.onFailure {
                Timber.w(it, "Exception formatting string")
            }
            binding.expiredText.text =
                HtmlCompat.fromHtml(expiredString, HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.expiredSubtext.isVisible = true
        }
        binding.expiredImage.layoutParams = expiredImageParams

        binding.expiredDismissButton.setOnClickListener {
            finish()
        }
    }

    override fun finish() {
        myAccountInfo.isBusinessAlertShown = false
        super.finish()
    }

    companion object {
        private const val IMAGE_HEIGHT_PORTRAIT = 284f
        private const val IMAGE_HEIGHT_LANDSCAPE = 136f
    }
}