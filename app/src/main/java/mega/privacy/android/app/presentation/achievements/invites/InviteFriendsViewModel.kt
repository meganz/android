package mega.privacy.android.app.presentation.achievements.invites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.achievements.invites.model.InviteFriendsUIState
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for InviteFriendsScreen
 */
@HiltViewModel
class InviteFriendsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase,
) : ViewModel() {
    private val inviteFriendsArgs = InviteFriendsArgs(savedStateHandle)
    private val _uiState = MutableStateFlow(InviteFriendsUIState())

    /**
     * Flow of [InviteFriendsUIState] UI State
     * @see InviteFriendsUIState
     * @see InviteFriendsUIState
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
            if (inviteFriendsArgs.storageBonusInBytes > 0) {
                _uiState.update { it.copy(grantStorageInBytes = inviteFriendsArgs.storageBonusInBytes) }
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
}