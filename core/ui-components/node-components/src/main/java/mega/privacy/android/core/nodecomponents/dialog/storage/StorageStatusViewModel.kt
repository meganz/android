package mega.privacy.android.core.nodecomponents.dialog.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StorageStatusViewModel @Inject constructor(
    private val getPricing: GetPricing,
    private val isAchievementsEnabledUseCase: IsAchievementsEnabledUseCase,
    private val getAccountTypeUseCase: GetAccountTypeUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
) : ViewModel() {
    private val _state = MutableStateFlow(StorageStatusUiState())

    /**
     * State
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                isAchievementsEnabledUseCase()
            }.onSuccess { isEnabled ->
                _state.update { it.copy(isAchievementsEnabled = isEnabled) }
            }.onFailure {
                Timber.e(it)
            }
        }
        viewModelScope.launch {
            runCatching {
                getPricing(false).products
            }.onSuccess { products ->
                _state.update {
                    it.copy(
                        product = products
                            .filter { product -> product.months == 1 }
                            .maxBy { product -> product.storage }
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
        viewModelScope.launch {
            runCatching {
                getAccountTypeUseCase()
            }.onSuccess { accountType ->
                _state.update { it.copy(accountType = accountType) }
            }.onFailure {
                Timber.e(it)
            }
        }
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
 */
data class StorageStatusUiState(
    val product: Product? = null,
    val accountType: AccountType = AccountType.FREE,
    val isAchievementsEnabled: Boolean = false,
)