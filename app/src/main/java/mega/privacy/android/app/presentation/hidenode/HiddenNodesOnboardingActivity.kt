package mega.privacy.android.app.presentation.hidenode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.HiddenNodeOnboardingCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.HiddenNodeOnboardingContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.HiddenNodeUpgradeCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.HiddenNodeUpgradeUpgradeButtonPressedEvent
import javax.inject.Inject

@AndroidEntryPoint
class HiddenNodesOnboardingActivity : AppCompatActivity() {
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val isOnboarding: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        intent.getBooleanExtra(IS_ONBOARDING, false)
    }

    private val viewModel: HiddenNodesOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (isOnboarding) viewModel.setHiddenNodesOnboarded()

        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsState(initial = ThemeMode.System)
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                HiddenNodesOnboardingScreen(
                    viewModel = viewModel,
                    isOnboarding = isOnboarding,
                    onClickBack = ::finish,
                    onClickContinue = ::handleContinue,
                )
            }
        }
    }

    private fun handleContinue() {
        Analytics.tracker.trackEvent(
            if (isOnboarding) HiddenNodeOnboardingContinueButtonPressedEvent
            else HiddenNodeUpgradeUpgradeButtonPressedEvent
        )
        if (isOnboarding) {
            setResult(RESULT_OK)
        } else {
            megaNavigator.openUpgradeAccount(context = this)
        }

        finish()
    }

    override fun finish() {
        Analytics.tracker.trackEvent(
            if (isOnboarding) HiddenNodeOnboardingCloseButtonPressedEvent
            else HiddenNodeUpgradeCloseButtonPressedEvent
        )
        super.finish()
    }

    companion object {
        private const val IS_ONBOARDING: String = "is_onboarding"

        fun createScreen(
            context: Context,
            isOnboarding: Boolean,
        ) = Intent(context, HiddenNodesOnboardingActivity::class.java).apply {
            putExtra(IS_ONBOARDING, isOnboarding)
        }
    }
}
