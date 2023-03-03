package mega.privacy.android.app.upgradeAccount.payment

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.upgradeAccount.payment.component.PaymentScreen
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Activity for managing upgrade account payments.
 */
@AndroidEntryPoint
internal class PaymentActivity : PasscodeActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<PaymentViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)

            AndroidTheme(isDark = themeMode.isDarkMode()) {
                PaymentScreen(billingViewModel = billingViewModel, paymentViewModel = viewModel)
            }
        }
    }

    companion object {
        /**
         * Const defining upgrade type.
         */
        const val UPGRADE_TYPE = "UPGRADE_TYPE"
    }
}