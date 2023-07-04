package mega.privacy.android.app.presentation.achievements.referral.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.achievements.referral.model.ReferralBonusesUIState
import mega.privacy.android.data.mapper.ReferralBonusAchievementsMapper
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ReferralBonusesViewModel for ReferralBonusScreen
 */
@HiltViewModel
class ReferralBonusesViewModel @Inject constructor(
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase,
    private val referralBonusAchievementsMapper: ReferralBonusAchievementsMapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReferralBonusesUIState())

    /**
     * Flow of ReferralBonusScreen UI State
     * @see ReferralBonusesUIState
     * @see ReferralBonusesUIState
     */
    val uiState = _uiState.asStateFlow()

    init {
        getAwardedInviteAchievements()
    }

    /**
     * Get Achievements with the type of [AwardedAchievementInvite]
     * then it will be converted to [ReferralBonusAchievements] and returns a list of it
     * This function gets called once when the screen first
     */
    private fun getAwardedInviteAchievements() {
        viewModelScope.launch {
            /**
             * Get all achievements and filter only the one with the type [AwardedAchievementInvite]
             */
            runCatching {
                getAccountAchievementsOverviewUseCase()
            }.onSuccess { achievementsOverview ->
                val achievements = achievementsOverview
                    .awardedAchievements
                    .filterIsInstance<AwardedAchievementInvite>()
                    .map { achievement ->
                        /*Get user contact information from the referred email*/
                        val contact = runCatching {
                            getContactFromEmailUseCase(
                                email = achievement.referredEmails[0],
                                skipCache = false
                            )
                        }.onFailure {
                            Timber.e(it)
                        }.getOrNull()

                        /**
                         * Call a mapper to map the achievement combined with contact
                         * into a new object combined, because the view would required this combined data
                         */
                        referralBonusAchievementsMapper(achievement, contact)
                    }

                _uiState.update { it.copy(awardedInviteAchievements = achievements) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}