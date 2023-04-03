package mega.privacy.android.app.main.megaachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverview
import mega.privacy.android.domain.usecase.achievements.IsAddPhoneRewardEnabledUseCase
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

/**
 * View Model for achievements related data
 */
@HiltViewModel
class AchievementsOverviewViewModel @Inject constructor(
    private val getAccountAchievementsOverview: GetAccountAchievementsOverview,
    private val areAchievementsEnabled: AreAchievementsEnabledUseCase,
    private val isAddPhoneRewardEnabled: IsAddPhoneRewardEnabledUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AchievementsUIState())

    /**
     * Achievements state
     */
    val state = _state.asStateFlow()

    init {
        logAchievementsEnabled()
        getAchievementsOverview()
        updateAddPhoneReward()
    }

    private fun updateAddPhoneReward() {
        viewModelScope.launch {
            _state.update {
                it.copy(showAddPhoneReward = isAddPhoneRewardEnabled())
            }
        }
    }

    private fun logAchievementsEnabled() {
        viewModelScope.launch {
            Timber.d("Achievements are enabled: ${areAchievementsEnabled()}")
        }
    }

    private fun getAchievementsOverview() {
        viewModelScope.launch {
            runCatching { getAccountAchievementsOverview() }
                .onSuccess { achievementsOverview ->
                    _state.value = AchievementsUIState(
                        achievementsOverview = achievementsOverview,
                        areAllRewardsExpired = areAllRewardsExpired(achievementsOverview)
                    )
                }
                .onFailure { _state.value = AchievementsUIState(showError = true) }
        }
    }

    private fun areAllRewardsExpired(overview: AchievementsOverview): Boolean {
        var expiredAwardCount = 0

        overview.awardedAchievements.forEach { award ->
            if (award is AwardedAchievementInvite) {
                Timber.d("Registration award expires in ${award.expirationTimestampInDays} days")
                val start =
                    Util.calculateDateFromTimestamp(award.expirationTimestampInDays)
                val end = Calendar.getInstance()
                val startTime = start.timeInMillis
                val endTime = end.timeInMillis
                val diffTime = startTime - endTime
                val daysLeft = diffTime / (1000 * 60 * 60 * 24)
                if (daysLeft < 0) {
                    expiredAwardCount++
                }
            } else {
                Timber.d("Type of achievement: ${award.type}")
            }
        }

        return expiredAwardCount == overview.awardedAchievements.size
    }
}
