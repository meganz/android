package mega.privacy.android.feature.myaccount.presentation.achievements.invites.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.model.InviteFriendsUIState
import timber.log.Timber

/**
 * ViewModel for InviteFriendsScreen
 */
@HiltViewModel(assistedFactory = InviteFriendsViewModel.Factory::class)
class InviteFriendsViewModel @AssistedInject constructor(
    @Assisted private val grantStorageInBytes: Long,
    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase,
) : ViewModel() {

    /**
     * Factory for creating [InviteFriendsViewModel] with the referral storage bonus carried by the
     * navigation key.
     */
    @AssistedFactory
    interface Factory {
        /**
         * Create an [InviteFriendsViewModel] for the given [grantStorageInBytes].
         */
        fun create(grantStorageInBytes: Long): InviteFriendsViewModel
    }

    private val _uiState = MutableStateFlow(
        InviteFriendsUIState(
            grantStorageInBytes = 0,
            durationInDays = 365,
        )
    )

    /**
     * Flow of [InviteFriendsUIState] UI State
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
             * Check if the caller passes the value of Referral Storage Bonus from invites.
             * Will skip getAccountAchievementsOverviewUseCase calls when a value is present.
             * This is added to remove unnecessary and redundant use case calls when not needed.
             */
            if (grantStorageInBytes > 0) {
                _uiState.update { it.copy(grantStorageInBytes = grantStorageInBytes) }
                return@launch
            }

            /**
             * Get all achievements and filter only the one with the type [AwardedAchievementInvite]
             */
            runCatching {
                getAccountAchievementsOverviewUseCase()
                    .allAchievements
                    .first { it.type == AchievementType.MEGA_ACHIEVEMENT_INVITE }
            }.onSuccess { value ->
                _uiState.update {
                    it.copy(
                        grantStorageInBytes = value.grantStorageInBytes,
                        durationInDays = value.durationInDays
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}
