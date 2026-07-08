package mega.privacy.android.app.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.overdisk.OverDiskQuotaPaywallViewModel
import mega.privacy.android.app.presentation.overdisk.view.OverDiskQuotaPaywallScreen
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import timber.log.Timber

/**
 * Over Disk Quota Paywall screen, shown when the account is in over disk quota and the user
 * must upgrade or risk data deletion.
 */
@AndroidEntryPoint
class OverDiskQuotaPaywallActivity : FragmentActivity() {

    private val viewModel: OverDiskQuotaPaywallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MegaAppContainer(themeMode = themeMode) {
                OverDiskQuotaPaywallScreen(
                    uiState = uiState,
                    onDismiss = ::onDismiss,
                    onUpgrade = ::onUpgrade,
                )
            }
        }
    }

    private fun onDismiss() {
        Timber.i("Over Disk Quota Paywall warning dismissed")
        if (isTaskRoot) {
            launchMegaActivity()
        }
        finish()
    }

    private fun onUpgrade() {
        Timber.i("Starting upgrade process after Over Disk Quota Paywall")
        runCatching {
            startActivity(
                MegaActivity.getIntentWithExtraDestinations(
                    context = this,
                    navKeys = listOf(
                        UpgradeAccountNavKey(
                            source = UpgradeAccountSource.MY_ACCOUNT_SCREEN,
                        )
                    ),
                )
            )
        }.onFailure {
            Timber.e(it)
        }
        finish()
    }

    private fun launchMegaActivity() {
        runCatching {
            // MegaActivity already handles ask permission logic internally
            startActivity(
                MegaActivity.getIntent(this@OverDiskQuotaPaywallActivity)
            )
        }.onFailure {
            Timber.e(it)
        }
    }
}
