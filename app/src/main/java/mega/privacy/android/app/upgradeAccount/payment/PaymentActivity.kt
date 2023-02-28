package mega.privacy.android.app.upgradeAccount.payment

import android.os.Bundle
import android.view.MenuItem
import android.view.View.INVISIBLE
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityPaymentBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.middlelayer.iab.BillingConstant
import mega.privacy.android.app.upgradeAccount.payment.PaymentViewModel.Companion.MONTHLY_SUBSCRIBED
import mega.privacy.android.app.upgradeAccount.payment.PaymentViewModel.Companion.YEARLY_SUBSCRIBED
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.account.Skus
import timber.log.Timber

/**
 * Activity for managing upgrade account payments.
 */
class PaymentActivity : PasscodeActivity(), Scrollable {

    companion object {
        /**
         * Const defining upgrade type.
         */
        const val UPGRADE_TYPE = "UPGRADE_TYPE"
        private const val NOT_SUBSCRIBED_ALPHA = 0.36f
    }

    private lateinit var binding: ActivityPaymentBinding
    private val viewModel by viewModels<PaymentViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = StringResourcesUtils.getString(R.string.payment)
        }

        binding.scrollview.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        val upgradeType = intent.getIntExtra(UPGRADE_TYPE, INVALID_VALUE)

        setupUpgradeType(upgradeType)
        collectFlow(viewModel.state) { state ->
            setupPaymentMethods(upgradeType, state.isPaymentMethodAvailable, state.product)
        }

        binding.monthlyButton.setOnClickListener { binding.yearlyButton.isChecked = false }
        binding.monthlyText.setOnClickListener {
            binding.monthlyButton.isChecked = true
            binding.yearlyButton.isChecked = false
        }

        binding.yearlyButton.setOnClickListener { binding.monthlyButton.isChecked = false }
        binding.yearlyText.setOnClickListener {
            binding.monthlyButton.isChecked = false
            binding.yearlyButton.isChecked = true
        }
    }

    private fun setupUpgradeType(upgradeType: Int) {
        var color = 0
        var title = 0

        when (upgradeType) {
            PRO_LITE -> {
                color = R.color.orange_400_orange_300
                title = R.string.prolite_account
            }
            PRO_I -> {
                color = R.color.red_600_red_300
                title = R.string.pro1_account
            }
            PRO_II -> {
                color = R.color.red_600_red_300
                title = R.string.pro2_account
            }
            PRO_III -> {
                color = R.color.red_600_red_300
                title = R.string.pro3_account
            }
        }

        binding.paymentType.apply {
            setTextColor(ContextCompat.getColor(this@PaymentActivity, color))
            text = StringResourcesUtils.getString(title)
        }

        binding.walletIcon.setImageResource(BillingConstant.PAY_METHOD_ICON_RES_ID)

        var textWallet = StringResourcesUtils.getString(BillingConstant.PAY_METHOD_RES_ID)

        try {
            textWallet = textWallet.replace(
                "[A]", "<font color='"
                        + ColorUtils.getColorHexString(this, R.color.grey_087_white_087)
                        + "'>"
            ).replace("[/A]", "</font>")
        } catch (e: java.lang.Exception) {
            Timber.e(e, "Exception formatting string")
        }

        binding.walletText.text = textWallet.toSpannedHtmlText()

        binding.proceedButton.setOnClickListener {
            when (upgradeType) {
                PRO_I -> if (binding.monthlyButton.isChecked) Skus.SKU_PRO_I_MONTH
                else Skus.SKU_PRO_I_YEAR
                PRO_II -> if (binding.monthlyButton.isChecked) Skus.SKU_PRO_II_MONTH
                else Skus.SKU_PRO_II_YEAR

                PRO_III ->
                    if (binding.monthlyButton.isChecked) Skus.SKU_PRO_III_MONTH
                    else Skus.SKU_PRO_III_YEAR

                PRO_LITE ->
                    if (binding.monthlyButton.isChecked) Skus.SKU_PRO_LITE_MONTH
                    else Skus.SKU_PRO_LITE_YEAR
                else -> null
            }?.let { productId ->
                billingViewModel.startPurchase(this, productId)
            }
        }
    }

    private fun setupPaymentMethods(
        upgradeType: Int,
        isPaymentMethodAvailable: Boolean,
        products: List<Product>,
    ) {
        if (isPaymentMethodAvailable.not() || products.isEmpty()) {
            hideBilling()
            return
        }

        products.forEach { product ->
            val textToShow = viewModel.getPriceString(this, product)

            if (product.isMonthly) {
                binding.monthlyText.text = textToShow
            } else if (product.isYearly) {
                binding.yearlyText.text = textToShow
            }
        }


        when (viewModel.getSubscription(upgradeType)) {
            MONTHLY_SUBSCRIBED -> {
                disableBilling()
                binding.yearlyText.alpha = NOT_SUBSCRIBED_ALPHA
            }
            YEARLY_SUBSCRIBED -> {
                disableBilling()
                binding.monthlyText.alpha = NOT_SUBSCRIBED_ALPHA
            }
        }
    }

    private fun disableBilling() {
        binding.monthlyButton.visibility = INVISIBLE
        binding.yearlyButton.visibility = INVISIBLE
        binding.proceedButton.isVisible = false

        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.contentView)
        constraintSet.connect(
            R.id.monthly_text,
            ConstraintSet.START,
            R.id.month_separator,
            ConstraintSet.START,
            0
        )
        constraintSet.connect(
            R.id.yearly_text,
            ConstraintSet.START,
            R.id.year_separator,
            ConstraintSet.START,
            0
        )
        constraintSet.applyTo(binding.contentView)
    }

    private fun hideBilling() {
        binding.billingPeriod.text =
            StringResourcesUtils.getString(R.string.no_available_payment_method)

        binding.monthlyButton.isVisible = false
        binding.monthlyText.isVisible = false
        binding.monthSeparator.isVisible = false
        binding.yearlyButton.isVisible = false
        binding.yearlyText.isVisible = false
        binding.yearSeparator.isVisible = false
        binding.proceedButton.isVisible = false
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollview.canScrollVertically(SCROLLING_UP_DIRECTION)
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        ColorUtils.changeStatusBarColorForElevation(this, withElevation)

        binding.toolbar.apply {
            setBackgroundColor(
                if (Util.isDarkMode(this@PaymentActivity) && withElevation) ColorUtils.getColorForElevation(
                    this@PaymentActivity,
                    elevation
                )
                else ContextCompat.getColor(this@PaymentActivity, R.color.white_dark_grey)
            )

            setElevation(if (withElevation) elevation else 0f)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }
}