package mega.privacy.android.app.main.megaachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverview
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AchievementsOverviewViewModel @Inject constructor(
    private val getAccountAchievementsOverview: GetAccountAchievementsOverview,
) : ViewModel() {

    private val _state =
        MutableStateFlow<AchievementsUI>(AchievementsUI.Progress)

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { getAccountAchievementsOverview() }
                .onSuccess { achievementsOverview ->
                    _state.value = AchievementsUI.Content(
                        achievementsOverview,
                        areAllRewardsExpired(achievementsOverview)
                    )
                }
                .onFailure { _state.value = AchievementsUI.Error }
        }
    }

    private fun areAllRewardsExpired(achievementsDetails: AchievementsOverview): Boolean {
        var expiredAwardCount = 0

        achievementsDetails.awardedAchievements.forEach { award ->
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

        return expiredAwardCount == achievementsDetails.awardedAchievements.size
    }
}
