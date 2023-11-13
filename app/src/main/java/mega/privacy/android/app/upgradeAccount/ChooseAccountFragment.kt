package mega.privacy.android.app.upgradeAccount

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountView
import mega.privacy.android.app.upgradeAccount.view.VariantAOnboardingDialogView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantAViewProPlansButtonEvent
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

@AndroidEntryPoint
class ChooseAccountFragment : Fragment() {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    private val chooseAccountViewModel by activityViewModels<ChooseAccountViewModel>()

    internal lateinit var chooseAccountActivity: ChooseAccountActivity


    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chooseAccountActivity = activity as ChooseAccountActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent { ChooseAccountBody() }
    }


    @SuppressLint("ProduceStateDoesNotAssignValue")
    @Composable
    fun ChooseAccountBody() {
        val uiState by chooseAccountViewModel.state.collectAsStateWithLifecycle()
        val mode by getThemeMode()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        AndroidTheme(isDark = mode.isDarkMode()) {
            if (uiState.enableVariantAUI) {
                VariantAOnboardingDialogView(
                    state = uiState,
                    onSkipPressed = chooseAccountActivity::onFreeClick,
                    onViewPlansPressed = {
                        Analytics.tracker.trackEvent(
                            OnboardingUpsellingDialogVariantAViewProPlansButtonEvent
                        )
                        chooseAccountActivity.onPlanClicked(AccountType.PRO_I)
                    },
                )
            } else {
                ChooseAccountView(
                    state = uiState,
                    onBackPressed = chooseAccountActivity::onFreeClick,
                    onPlanClicked = chooseAccountActivity::onPlanClicked
                )
            }
        }
    }
}