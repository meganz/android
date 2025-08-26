package mega.privacy.android.app.presentation.account

import android.icu.text.DecimalFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.CallRecordingUIState
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
 * @property state [CallRecordingUIState]
 */
@HiltViewModel
class AccountStorageViewModel @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
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
                    Timber.e(it)
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
                                baseStorageFormatted = getStorageFormatted(
                                    it,
                                    DecimalFormat("#.##")
                                )
                            )
                        }
                    }

                }
            }.onFailure { exception ->
                Timber.e(exception)
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
                    Timber.e(it)
                }
                .collect { accountDetail ->
                    _state.update { state ->
                        state.copy(
                            totalStorage = accountDetail.storageDetail?.totalStorage,
                            totalStorageFormatted = getStorageFormatted(
                                accountDetail.storageDetail?.totalStorage,
                                DecimalFormat("#.##")
                            ),
                            storageUsedPercentage = accountDetail.storageDetail?.usedPercentage
                                ?: 0,
                        )
                    }
                }
        }
    }


    /**
     * Gets a size string.
     *
     * @return The size string.
     */
    private fun getStorageFormatted(totalStorage: Long?, df: DecimalFormat): String =
        when {
            totalStorage == null -> ""
            totalStorage < KB -> {
                getStringFromStringResMapper(
                    R.string.label_file_size_byte, totalStorage
                )
            }

            totalStorage < MB -> {
                getStringFromStringResMapper(
                    R.string.label_file_size_kilo_byte, df.format(totalStorage / KB)
                )
            }

            totalStorage < GB -> {
                getStringFromStringResMapper(
                    R.string.label_file_size_mega_byte, df.format(totalStorage / MB)
                )
            }

            totalStorage < TB -> {
                getStringFromStringResMapper(
                    R.string.label_file_size_giga_byte, df.format(totalStorage / GB)
                )
            }

            totalStorage < PB -> {
                getStringFromStringResMapper(
                    R.string.label_file_size_tera_byte, df.format(totalStorage / TB)
                )
            }

            totalStorage < EB -> {
                getStringFromStringResMapper(
                    R.string.label_file_size_peta_byte, df.format(totalStorage / PB)
                )
            }

            else -> {
                getStringFromStringResMapper(
                    R.string.label_file_size_exa_byte, df.format(totalStorage / EB)

                )
            }
        }

    /**
     * Check if the account should be upgraded due to ads.
     */
    fun isUpgradeAccountDueToAds(): Boolean {
        val state = _state.value
        Timber.d("Storage: ${state.storageUsedPercentage}")
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