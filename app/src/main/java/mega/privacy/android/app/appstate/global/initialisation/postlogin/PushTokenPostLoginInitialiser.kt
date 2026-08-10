package mega.privacy.android.app.appstate.global.initialisation.postlogin

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import mega.privacy.android.app.utils.Constants.DEVICE_ANDROID
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.pushnotifications.GetPushTokenUseCase
import mega.privacy.android.domain.usecase.pushnotifications.RegisterPushNotificationsUseCase
import mega.privacy.android.domain.usecase.pushnotifications.SetPushTokenUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Post-login initialiser that fetches the current FCM token and registers it with the API if needed.
 * Replaces the NewTokenWorker flow for the post-login case.
 */
class PushTokenPostLoginInitialiser @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val getPushTokenUseCase: GetPushTokenUseCase,
    private val registerPushNotificationsUseCase: RegisterPushNotificationsUseCase,
    private val setPushTokenUseCase: SetPushTokenUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PostLoginInitialiserAction(
    action = { _, _ ->
        withContext(ioDispatcher) {
            monitorFetchNodesFinishUseCase()
                .filter { it }
                .take(1)
                .catch { e -> Timber.e(e) }
                .collectLatest {
                    runCatching {
                        val newToken = firebaseMessaging.token.await()
                        when {
                            newToken.isNullOrEmpty() || getPushTokenUseCase() == newToken ->
                                Timber.d("No need to register new token.")

                            else -> {
                                Timber.d("Registering new push token after login.")
                                runCatching {
                                    registerPushNotificationsUseCase(DEVICE_ANDROID, newToken)
                                }.onSuccess { token ->
                                    runCatching { setPushTokenUseCase(token) }
                                        .onFailure { Timber.w("Exception setting push token: $it") }
                                }
                                    .onFailure { Timber.w("Exception registering push notifications: $it") }
                            }
                        }
                    }.onFailure {
                        Timber.e(it, "Error registering push token after login")
                    }
                }
        }
    }
)
