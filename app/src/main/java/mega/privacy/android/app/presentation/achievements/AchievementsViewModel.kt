package mega.privacy.android.app.presentation.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.usecase.GetAccountAchievements
import javax.inject.Inject

/**
 * Achievements view model
 *
 * @param getAccountAchievements : [GetAccountAchievements] use case
 * @param uiMegaAchievementMapper : UIMegaAchievementMapper
 */
class AchievementsViewModel @Inject constructor(
    private val getAccountAchievements: GetAccountAchievements,
    private val uiMegaAchievementMapper: UIMegaAchievementMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(AchievementInfoUIState())

    /**
     * State to be observed for UI
     */
    val state = _state.asStateFlow()

    init {
        getAccountAchievements()
    }

    /**
     * set award count
     * @param count
     */
    fun setAwardCount(count: Long) {
        _state.update {
            it.copy(awardCount = count)
        }
    }

    /**
     * set achievement type
     * @param [AchievementType]
     */
    fun setAchievementType(achievementType: AchievementType) {
        _state.update {
            it.copy(achievementType = achievementType)
        }
    }

    /**
     * Set toolbar title
     */
    fun setToolbarTitle(title: String) {
        _state.update {
            it.copy(toolbarTitle = title)
        }
    }

    /**
     * get achievements
     */
    private fun getAccountAchievements() {
        viewModelScope.launch {
            getAccountAchievements(achievementType = state.value.achievementType,
                awardIndex = state.value.awardCount)?.let { achievement ->
                _state.update {
                    it.copy(uiMegaAchievement = uiMegaAchievementMapper(achievement))
                }
            }
        }
    }
}