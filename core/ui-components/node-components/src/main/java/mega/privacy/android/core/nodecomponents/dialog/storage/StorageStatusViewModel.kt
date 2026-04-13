package mega.privacy.android.core.nodecomponents.dialog.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StorageStatusViewModel @Inject constructor(
    private val getPricing: GetPricing,
    private val isAchievementsEnabledUseCase: IsAchievementsEnabledUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) : ViewModel() {

    val state: StateFlow<StorageStatusUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorAccountDetailUseCase()
                .map { it.levelDetail?.accountType ?: AccountType.FREE }
                .catch {
                    Timber.e(it, "Error monitoring account detail")
                    emit(AccountType.FREE)
                },
            flow { emit(isAchievementsEnabledUseCase()) }
                .catch {
                    Timber.e(it, "Error getting achievements enabled")
                    emit(false)
                },
            flow {
                emit(
                    getPricing(false).products
                        .filter { it.months == 1 }
                        .maxByOrNull { it.storage }
                )
            }.catch {
                Timber.e(it, "Error getting pricing")
                emit(null)
            },
        ) { accountType, isAchievementsEnabled, product ->
            StorageStatusUiState(
                accountType = accountType,
                isAchievementsEnabled = isAchievementsEnabled,
                product = product,
                isLoading = false,
            )
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = StorageStatusUiState(isLoading = true),
        )
    }

    /**
     * Get user email
     *
     */
    suspend fun getUserEmail() = runCatching { getCurrentUserEmail(false) }.getOrNull().orEmpty()
}

/**
 * Storage status ui state
 *
 * @property product
 * @property accountType
 * @property isAchievementsEnabled
 * @property isLoading
 */
data class StorageStatusUiState(
    val product: Product? = null,
    val accountType: AccountType = AccountType.FREE,
    val isAchievementsEnabled: Boolean = false,
    val isLoading: Boolean = true,
)