package mega.privacy.android.feature.payment.presentation.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorAdsClosingTimestampUseCase
import mega.privacy.android.feature.payment.model.AccountStorageUIState
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * ViewModel for account storage
 *
 */
@HiltViewModel
class AccountStorageViewModel @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getAccountAchievements: GetAccountAchievements,
    private val monitorAdsClosingTimestampUseCase: MonitorAdsClosingTimestampUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountStorageUIState())
    val state = _state.asStateFlow()

    private var monitorStorageJob: Job? = null

    init {
        getBaseStorage()
        monitorAccountDetail()
        monitorAdsClosingTimestamp()
    }

    private fun monitorAdsClosingTimestamp() {
        viewModelScope.launch {
            monitorAdsClosingTimestampUseCase()
                .catch {
                    Timber.Forest.e(it)
                }
                .collect { timestamp ->
                    _state.update { state ->
                        state.copy(lastAdsClosingTimestamp = timestamp ?: 0)
                    }
                }
        }
    }

    /**
     * Get base storage
     */
    private fun getBaseStorage() {
        viewModelScope.launch {
            runCatching {
                getAccountAchievements(
                    achievementType = AchievementType.MEGA_ACHIEVEMENT_WELCOME,
                    awardIndex = 0L
                )
            }.onSuccess { megaAchievement ->
                megaAchievement?.apply {
                    baseStorage?.let {
                        _state.update { state ->
                            state.copy(
                                baseStorage = it,
                            )
                        }
                    }

                }
            }.onFailure { exception ->
                Timber.Forest.e(exception)
            }
        }
    }

    /**
     * Monitor account detail
     */
    fun monitorAccountDetail() {
        monitorStorageJob?.cancel()
        monitorStorageJob = viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch {
                    Timber.Forest.e(it)
                }
                .collect { accountDetail ->
                    _state.update { state ->
                        state.copy(
                            totalStorage = accountDetail.storageDetail?.totalStorage,
                            storageUsedPercentage = accountDetail.storageDetail?.usedPercentage
                                ?: 0,
                        )
                    }
                }
        }
    }

    /**
     * Check if the account should be upgraded due to ads.
     */
    fun isUpgradeAccountDueToAds(): Boolean {
        val state = _state.value
        Timber.Forest.d("Storage: ${state.storageUsedPercentage}")
        val within2Days =
            System.currentTimeMillis() - state.lastAdsClosingTimestamp <= 2.days.inWholeMicroseconds
        return state.storageUsedPercentage < 50 && within2Days
    }

    private companion object {
        const val KB: Long = 1024
        const val MB: Long = KB * 1024
        const val GB: Long = MB * 1024
        const val TB: Long = GB * 1024
        const val PB: Long = TB * 1024
        const val EB: Long = PB * 1024
    }
}