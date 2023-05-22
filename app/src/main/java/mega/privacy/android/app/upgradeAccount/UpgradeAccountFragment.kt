package mega.privacy.android.app.upgradeAccount

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.upgradeAccount.payment.PaymentActivity
import mega.privacy.android.app.upgradeAccount.view.UpgradeAccountView
import mega.privacy.android.app.upgradeAccount.view.NewUpgradeAccountView
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UpgradeAccountFragment : Fragment() {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val upgradeAccountViewModel by activityViewModels<UpgradeAccountViewModel>()

    // define activity view model here, we don't need to create new every time fragment recreate
    private val billingViewModel by activityViewModels<BillingViewModel>()

    internal lateinit var upgradeAccountActivity: UpgradeAccountActivity


    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upgradeAccountActivity = activity as UpgradeAccountActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent { UpgradeAccountBody() }
        setupObservers()
    }


    @Composable
    fun UpgradeAccountBody() {
        val uiState by upgradeAccountViewModel.state.collectAsStateWithLifecycle()
        val mode by getThemeMode()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        AndroidTheme(isDark = mode.isDarkMode()) {
            val useNewPlansPageUI by produceState(initialValue = false) {
                value = getFeatureFlagUseCase(AppFeatures.PlansPageUpdate)
            }
            if (useNewPlansPageUI) {
                NewUpgradeAccountView(
                    state = uiState,
                    onBackPressed = { upgradeAccountActivity.onBackPressedDispatcher.onBackPressed() },
                    onButtonClicked = { onUpgradeClick(it) },
                    onTOSClicked = { redirectToTOSPage() },
                )
            } else {
                UpgradeAccountView(
                    state = uiState,
                    onBackPressed = { upgradeAccountActivity.onBackPressedDispatcher.onBackPressed() },
                    onPlanClicked = { onUpgradeClick(it) },
                    onCustomLabelClicked = {
                        uiState.currentSubscriptionPlan?.let {
                            onCustomLabelClick(it)
                        }
                    },
                    hideBillingWarning = { upgradeAccountViewModel.setBillingWarningVisibility(false) },
                    onDialogPositiveButtonClicked = { onDialogPositiveButtonClicked(it) },
                    onDialogDismissButtonClicked = {
                        upgradeAccountViewModel.setShowBuyNewSubscriptionDialog(
                            showBuyNewSubscriptionDialog = false
                        )
                    },
                )
            }
        }
    }

    private fun setupObservers() {
        upgradeAccountViewModel.onUpgradeClick().observe(viewLifecycleOwner) { upgradeType ->
            startActivity(
                Intent(context, PaymentActivity::class.java)
                    .putExtra(PaymentActivity.UPGRADE_TYPE, upgradeType)
            )
        }
    }

    /**
     * Shows the selected payment plan.
     *
     * @param accountType Selected payment plan.
     */
    private fun onUpgradeClick(accountType: AccountType) {
        with(upgradeAccountViewModel) {
            if (!isBillingAvailable()) {
                Timber.w("Billing not available")
                setBillingWarningVisibility(true)
                return
            }
            val upgradeType = convertAccountTypeToInt(accountType)
            currentPaymentCheck(upgradeType)
        }
    }

    private fun onCustomLabelClick(currentSubscriptionPlan: AccountType) {
        val accountTypeInt = convertAccountTypeToInt(currentSubscriptionPlan)
        AlertsAndWarnings.askForCustomizedPlan(
            requireContext(),
            megaApi.myEmail,
            accountTypeInt
        )
    }

    private fun onDialogPositiveButtonClicked(upgradeType: Int) {
        upgradeAccountViewModel.setShowBuyNewSubscriptionDialog(
            showBuyNewSubscriptionDialog = false
        )
        startActivity(
            Intent(context, PaymentActivity::class.java)
                .putExtra(PaymentActivity.UPGRADE_TYPE, upgradeType)
        )
    }

    private fun convertAccountTypeToInt(accountType: AccountType): Int {
        return when (accountType) {
            AccountType.PRO_LITE -> Constants.PRO_LITE
            AccountType.PRO_I -> Constants.PRO_I
            AccountType.PRO_II -> Constants.PRO_II
            AccountType.PRO_III -> Constants.PRO_III
            else -> Constants.INVALID_VALUE
        }
    }

    private fun redirectToTOSPage() {
        startActivity(
            Intent(requireContext(), WebViewActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setData(Uri.parse(Constants.TERMS_OF_SERVICE_URL))
        )
    }
}