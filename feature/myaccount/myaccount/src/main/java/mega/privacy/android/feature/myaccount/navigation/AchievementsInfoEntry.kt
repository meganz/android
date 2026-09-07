package mega.privacy.android.feature.myaccount.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.myaccount.presentation.achievements.info.AchievementsInfoViewModel
import mega.privacy.android.feature.myaccount.presentation.achievements.info.view.AchievementsInfoView
import mega.privacy.android.navigation.destination.AchievementsInfoNavKey

/**
 * Wires the [AchievementsInfoView] to its [AchievementsInfoViewModel] for the achievement type
 * carried by [AchievementsInfoNavKey].
 */
internal fun EntryProviderScope<NavKey>.achievementsInfo() {
    entry<AchievementsInfoNavKey> { navKey ->
        val viewModel = hiltViewModel<AchievementsInfoViewModel, AchievementsInfoViewModel.Factory>(
            creationCallback = { factory -> factory.create(navKey.achievementType) }
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        AchievementsInfoView(uiState = uiState)
    }
}
