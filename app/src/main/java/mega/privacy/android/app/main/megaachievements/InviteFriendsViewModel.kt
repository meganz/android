package mega.privacy.android.app.main.megaachievements

import androidx.lifecycle.SavedStateHandle
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
    private val savedStateHandle: SavedStateHandle,
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
             * Check if the previous fragment passes the value of Referral Storage Bonus from invites
             * Will skip getAccountAchievementsOverviewUseCase calls when a value is present
             * This is added to remove unnecessary and redundant use case calls when not needed.
             */
            val savedReferral = savedStateHandle.get<Long?>(REFERRAL_STORAGE_BONUS)
            if (savedReferral != null && savedReferral > 0) {
                _uiState.update { it.copy(grantStorageInBytes = savedReferral) }
                return@launch
            }

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

    companion object {
        /**
         * Arguments flag to check if the previous fragment passes the value of Referral Storage Bonus from invites
         */
        const val REFERRAL_STORAGE_BONUS = "referral_storage_bonus"
    }
}