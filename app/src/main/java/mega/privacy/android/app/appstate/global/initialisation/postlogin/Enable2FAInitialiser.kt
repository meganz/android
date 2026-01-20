package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.take
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.Enable2FANavKey
import timber.log.Timber
import javax.inject.Inject


class Enable2FAInitialiser @Inject constructor(
    requireTwoFactorAuthenticationUseCase: RequireTwoFactorAuthenticationUseCase,
    getCurrentUserEmail: GetCurrentUserEmail,
    getLastRegisteredEmailUseCase: GetLastRegisteredEmailUseCase,
    navigationEventQueue: NavigationEventQueue,
    monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase,
) : PostLoginInitialiser(
    action = { _, isFastLogin ->
        monitorUpdateUserDataUseCase() // ensure current user email loaded
            .catch {
                Timber.e(it, "Error monitoring user data updates for 2FA requirement")
            }.take(1).collect {
                Timber.d("isFastLogin: $isFastLogin")
                if (!isFastLogin) {
                    // logged in by account credentials
                    runCatching {
                        val currentUserEmail = getCurrentUserEmail()
                        val lastRegisteredEmail = getLastRegisteredEmailUseCase()
                        Timber.d("Current user email: $currentUserEmail Last registered email: $lastRegisteredEmail")
                        requireTwoFactorAuthenticationUseCase(
                            newAccount = currentUserEmail == lastRegisteredEmail,
                            firstLogin = true
                        )
                    }.onFailure { e ->
                        Timber.e(e, "Error checking 2FA requirements")
                    }
                } else {
                    // fast login it means the user has already logged in once
                    runCatching {
                        requireTwoFactorAuthenticationUseCase(
                            newAccount = false,
                            firstLogin = false
                        )
                    }.onSuccess { show2FA ->
                        if (show2FA) {
                            navigationEventQueue.emit(Enable2FANavKey)
                        }
                    }.onFailure { e ->
                        Timber.e(e, "Error checking 2FA requirements on fast login")
                    }
                }
            }
    }
)