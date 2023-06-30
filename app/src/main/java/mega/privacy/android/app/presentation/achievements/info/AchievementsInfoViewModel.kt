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
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for AchievementsInfoScreen
 */
@HiltViewModel
class AchievementsInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase,
    private val numberOfDaysMapper: NumberOfDaysMapper,
) : ViewModel() {
    private val achievementInfoArgs = AchievementInfoArgs(savedStateHandle)
    private val achievementType =
        AchievementType.values()
            .firstOrNull {
                it.classValue == achievementInfoArgs.achievementTypeId
            }
    private val _uiState = MutableStateFlow(AchievementsInfoUIState())

    /**
     * Flow of [AchievementsInfoUIState] UI State
     * @see AchievementsInfoUIState
     */
    val uiState = _uiState.asStateFlow()

    init {
        setInitialAchievementsType()
        fetchAchievementsOverview()
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
     * Fetch the current user's achievements overview
     */
    private fun fetchAchievementsOverview() = viewModelScope.launch {
        runCatching {
            getAccountAchievementsOverviewUseCase().also {
                updateAchievementsRemainingDays(it)
                updateAwardedStorage(it)
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Update achievements remaining days if any
     * this may not be updated if the user didn't have any achievements awarded to them
     */
    private fun updateAchievementsRemainingDays(achievementsOverview: AchievementsOverview) =
        viewModelScope.launch {
            achievementsOverview
                .awardedAchievements
                .firstOrNull { it.type == achievementType }
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
    private fun updateAwardedStorage(achievementsOverview: AchievementsOverview) =
        viewModelScope.launch {
            val awardedStorage =
                if (uiState.value.isAchievementAwarded.not() && achievementType != AchievementType.MEGA_ACHIEVEMENT_WELCOME) {
                    achievementsOverview.allAchievements.firstOrNull {
                        it.type == achievementType
                    }?.grantStorageInBytes
                } else {
                    achievementsOverview.awardedAchievements.firstOrNull {
                        it.awardId == uiState.value.awardId
                    }?.rewardedStorageInBytes
                }

            _uiState.update {
                it.copy(awardStorageInBytes = awardedStorage ?: 0)
            }
        }
}