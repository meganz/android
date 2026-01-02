package mega.privacy.android.app.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.presentation.account.model.QAAccountSwitchEvent
import mega.privacy.android.app.presentation.account.model.QAAccountUiState
import mega.privacy.android.data.gateway.QAAccountCacheGateway
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for account switching functionality
 * This is designed to be easily migrated to production code in the future
 */
@HiltViewModel
class QAAccountViewModel @Inject constructor(
    private val qaAccountCacheGateway: QAAccountCacheGateway,
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase,
    private val fastLoginUseCase: FastLoginUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QAAccountUiState())
    internal val uiState: StateFlow<QAAccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadCachedAccounts()
        }
    }

    /**
     * Save current account credentials to cache
     */
    suspend fun saveCurrentAccount(): UserCredentials? {
        return runCatching {
            val credentials = getAccountCredentialsUseCase()
            if (credentials != null && !credentials.email.isNullOrBlank() && !credentials.session.isNullOrBlank()) {
                qaAccountCacheGateway.saveAccount(credentials)
                // Update last login time to current time when saving account
                qaAccountCacheGateway.updateLastLoginTime(credentials.email, System.currentTimeMillis())
                loadCachedAccounts()
                Timber.d("Account saved: ${credentials.email}")
                credentials
            } else {
                Timber.w("Cannot save account: credentials are invalid")
                null
            }
        }.onFailure { e ->
            Timber.e(e, "Error saving account")
        }.getOrNull()
    }

    /**
     * Switch to a cached account
     * This method performs a chat logout to clean up chat resources before switching accounts.
     * It does not perform a full logout (which would invalidate the session token).
     */
    fun switchToAccount(credentials: UserCredentials) {
        val session = credentials.session
        if (session.isNullOrBlank()) {
            Timber.w("Cannot switch: session is empty")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSwitchingAccount = true) }
            runCatching {
                // First, clean up chat resources to avoid crashes when switching accounts
                // This is similar to what happens in normal logout, but without invalidating the session
                val disableChatApiUseCase = DisableChatApiUseCase {
                    MegaApplication.getInstance().disableMegaChatApi()
                }
                Timber.d("Cleaning up chat resources before switching to account: ${credentials.email}")
                chatLogoutUseCase(disableChatApiUseCase)
                // Now perform fast login with the new account
                fastLoginUseCase(
                    session = session,
                    refreshChatUrl = false,
                    disableChatApiUseCase = disableChatApiUseCase
                ).catch { exception ->
                    Timber.e(exception, "Error switching account: ${credentials.email}")
                    _uiState.update {
                        it.copy(
                            isSwitchingAccount = false,
                            accountSwitchEvent = triggered(QAAccountSwitchEvent.Failure(exception))
                        )
                    }
                }.collect { loginStatus ->
                    when (loginStatus) {
                        LoginStatus.LoginSucceed -> {
                            Timber.d("Successfully switched to account: ${credentials.email}")
                            // Update last login time
                            qaAccountCacheGateway.updateLastLoginTime(credentials.email, System.currentTimeMillis())
                            _uiState.update {
                                it.copy(
                                    isSwitchingAccount = false,
                                    accountSwitchEvent = triggered(QAAccountSwitchEvent.Success(credentials.email))
                                )
                            }
                        }

                        else -> {
                            // Other statuses, continue waiting
                        }
                    }
                }
            }.onFailure { e ->
                Timber.e(e, "Error switching account")
                _uiState.update {
                    it.copy(
                        isSwitchingAccount = false,
                        accountSwitchEvent = triggered(QAAccountSwitchEvent.Failure(e))
                    )
                }
            }
        }
    }

    /**
     * Remove a cached account
     */
    fun removeAccount(credentials: UserCredentials) {
        viewModelScope.launch {
            runCatching {
                qaAccountCacheGateway.removeAccount(credentials.email)
                loadCachedAccounts()
            }.onFailure { e ->
                Timber.e(e, "Error removing account")
            }
        }
    }

    /**
     * Clear all cached accounts
     */
    fun clearAllAccounts() {
        viewModelScope.launch {
            runCatching {
                qaAccountCacheGateway.clearAllAccounts()
                loadCachedAccounts()
            }.onFailure { e ->
                Timber.e(e, "Error clearing accounts")
            }
        }
    }

    /**
     * Load all cached accounts from storage
     */
    private suspend fun loadCachedAccounts() {
        runCatching {
            val accounts = qaAccountCacheGateway.getAllCachedAccounts()
            _uiState.update { it.copy(cachedAccounts = accounts) }
        }.onFailure { e ->
            Timber.e(e, "Error loading cached accounts")
        }
    }

    /**
     * Consume account switch event
     */
    fun consumeAccountSwitchEvent() {
        _uiState.update { it.copy(accountSwitchEvent = consumed()) }
    }

    /**
     * Get last login time for an account
     */
    suspend fun getLastLoginTime(email: String?): Long? {
        return runCatching {
            qaAccountCacheGateway.getLastLoginTime(email)
        }.onFailure { e ->
            Timber.e(e, "Error getting last login time")
        }.getOrNull()
    }

    /**
     * Get current logged in account email
     */
    suspend fun getCurrentAccountEmail(): String? {
        return runCatching {
            val credentials = getAccountCredentialsUseCase()
            credentials?.email
        }.onFailure { e ->
            Timber.e(e, "Error getting current account email")
        }.getOrNull()
    }

    /**
     * Save remark for an account
     */
    suspend fun saveRemark(email: String?, remark: String?) {
        runCatching {
            qaAccountCacheGateway.saveRemark(email, remark)
            loadCachedAccounts()
        }.onFailure { e ->
            Timber.e(e, "Error saving remark")
        }
    }

    /**
     * Get remark for an account
     */
    suspend fun getRemark(email: String?): String? {
        return runCatching {
            qaAccountCacheGateway.getRemark(email)
        }.onFailure { e ->
            Timber.e(e, "Error getting remark")
        }.getOrNull()
    }
}
