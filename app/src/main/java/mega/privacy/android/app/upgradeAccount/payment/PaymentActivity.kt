package mega.privacy.android.app.upgradeAccount.payment

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.upgradeAccount.payment.component.PaymentScreen
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.PurchaseType
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

            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                PaymentScreen(billingViewModel = billingViewModel, paymentViewModel = viewModel)
            }
        }
    }

    override fun handlePurchased(purchaseType: PurchaseType): Boolean {
        if (myAccountInfo.isUpgradeFromAccount()) {
            startActivity(Intent(this, MyAccountActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            return true
        }
        return false
    }

    companion object {
        /**
         * Const defining upgrade type.
         */
        const val UPGRADE_TYPE = "UPGRADE_TYPE"
    }
}