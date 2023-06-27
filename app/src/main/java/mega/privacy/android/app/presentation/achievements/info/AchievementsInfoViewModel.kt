package mega.privacy.android.app.presentation.achievements.info

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.data.extensions.toMillis
import mega.privacy.android.app.presentation.achievements.info.model.AchievementsInfoUIState
import mega.privacy.android.data.mapper.NumberOfDaysMapper
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import java.io.Serializable
import javax.inject.Inject

/**
 * View Model for AchievementsInfoFragment
 * @see AchievementsInfoFragment
 */
@HiltViewModel
class AchievementsInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val numberOfDaysMapper: NumberOfDaysMapper,
) : ViewModel() {
    private val achievementsOverview =
        savedStateHandle.get<Serializable>(ACHIEVEMENTS_OVERVIEW) as? AchievementsOverview
    private val achievementType =
        savedStateHandle.get<Serializable>(ACHIEVEMENTS_TYPE) as? AchievementType
    private val _uiState = MutableStateFlow(AchievementsInfoUIState())

    /**
     * Flow of [AchievementsInfoFragment] UI State
     * @see AchievementsInfoUIState
     */
    val uiState = _uiState.asStateFlow()

    init {
        setInitialAchievementsType()
        updateAchievementsRemainingDays()
        updateAwardedStorage()
    }

    /**
     * Sets the achievements type from fragment arguments
     */
    private fun setInitialAchievementsType() = viewModelScope.launch {
        _uiState.update {
            it.copy(achievementType = achievementType)
        }
    }

    /**
     * Update achievements remaining days if any
     * this may not be updated if the user didn't have any achievements awarded to them
     */
    private fun updateAchievementsRemainingDays() = viewModelScope.launch {
        achievementsOverview
            ?.awardedAchievements
            ?.firstOrNull { it.type == achievementType }
            ?.let { award ->
                val remainingDays =
                    numberOfDaysMapper(award.expirationTimestampInSeconds.toMillis())

                _uiState.update {
                    it.copy(
                        awardId = award.awardId,
                        achievementRemainingDays = remainingDays,
                        isAchievementExpired = remainingDays < 1,
                        isAchievementAlmostExpired = remainingDays <= 15,
                        isAchievementAwarded = award.awardId != -1,
                    )
                }
            }
    }

    /**
     * Update the amount of storage that can be awarded or already has been awarded
     * depends on if the user has been awarded or not
     * MEGA_ACHIEVEMENT_WELCOME is by default should have already been awarded to the user
     */
    private fun updateAwardedStorage() = viewModelScope.launch {
        val awardedStorage =
            if (uiState.value.isAchievementAwarded.not() && achievementType != AchievementType.MEGA_ACHIEVEMENT_WELCOME) {
                achievementsOverview?.allAchievements?.first {
                    it.type == achievementType
                }?.grantStorageInBytes
            } else {
                achievementsOverview?.awardedAchievements?.first {
                    it.awardId == uiState.value.awardId
                }?.rewardedStorageInBytes
            }

        _uiState.update {
            it.copy(awardStorageInBytes = awardedStorage ?: 0)
        }
    }

    companion object {
        /**
         * Achievements overview tag to be passed as fragment arguments
         */
        const val ACHIEVEMENTS_OVERVIEW = "achievements_overview"

        /**
         * Achievements type tag to be passed as fragment arguments
         */
        const val ACHIEVEMENTS_TYPE = "achievements_type"
    }
}