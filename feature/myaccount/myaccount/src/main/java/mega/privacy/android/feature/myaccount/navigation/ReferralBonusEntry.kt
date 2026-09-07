package mega.privacy.android.feature.myaccount.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.myaccount.presentation.achievements.referral.view.ReferralBonusView
import mega.privacy.android.feature.myaccount.presentation.achievements.referral.view.ReferralBonusesViewModel
import mega.privacy.android.navigation.destination.ReferralBonusNavKey

/**
 * Wires the [ReferralBonusView] to its [ReferralBonusesViewModel].
 */
internal fun EntryProviderScope<NavKey>.referralBonus() {
    entry<ReferralBonusNavKey> {
        val viewModel = hiltViewModel<ReferralBonusesViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ReferralBonusView(uiState = uiState)
    }
}
