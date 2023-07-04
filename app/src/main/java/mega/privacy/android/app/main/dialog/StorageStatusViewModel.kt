package mega.privacy.android.app.main.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabled
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class StorageStatusViewModel @Inject constructor(
    private val getPricing: GetPricing,
    private val isAchievementsEnabled: IsAchievementsEnabled,
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
                isAchievementsEnabled()
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
                _state.update { it.copy(product = products.firstOrNull { product -> product.level == Constants.PRO_III && product.months == 1 }) }
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