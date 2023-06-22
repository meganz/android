package mega.privacy.android.app.main.megaachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.achievements.referral.ReferralBonusesFragment
import mega.privacy.android.app.presentation.achievements.referral.model.ReferralBonusesUIState
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * InviteFriendsViewModel
 * ViewModel for [InviteFriendsFragment]
 */
@HiltViewModel
class InviteFriendsViewModel @Inject constructor(
    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InviteFriendsUIState())

    /**
     * Flow of [ReferralBonusesFragment] UI State
     * @see ReferralBonusesUIState
     * @see ReferralBonusesUIState
     */
    val uiState = _uiState.asStateFlow()

    init {
        getInviteReferralStorageValue()
    }

    /**
     * Get Achievements with the type of [AwardedAchievementInvite]
     * then it will be converted to [ReferralBonusAchievements] and returns a list of it
     * This function gets called once when the screen first
     */
    private fun getInviteReferralStorageValue() {
        viewModelScope.launch {
            /**
             * Get all achievements and filter only the one with the type [AwardedAchievementInvite]
             */
            runCatching {
                getAccountAchievementsOverviewUseCase()
                    .allAchievements
                    .first { it.type == AchievementType.MEGA_ACHIEVEMENT_INVITE }
                    .grantStorageInBytes
            }.onSuccess { value ->
                _uiState.update { it.copy(grantStorageInBytes = value) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}