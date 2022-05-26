package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.PushesRepository
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder
import mega.privacy.android.app.fcm.NewTokenWorker.Companion.NEW_TOKEN
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.middlelayer.push.PushMessageHandler.Companion.PUSH_TOKEN
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default [PushesRepository] implementation.
 *
 * @property context        Required for getting shared preferences.
 * @property megaApi        Required for registering push notifications.
 * @property ioDispatcher   Required for launching coroutines.
 */
class DefaultPushesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApi: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaChatApi: MegaChatApiGateway
) : PushesRepository {

    private val token = "token"

    override fun getPushToken(): String =
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE)
            .getString(token, "") ?: ""

    override suspend fun registerPushNotifications(deviceType: Int, newToken: String): String =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApi.registerPushNotifications(
                    deviceType, newToken, OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestRegisterPushNotificationsCompleted(continuation)
                    )
                )
            }
        }

    override fun setPushToken(newToken: String) {
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE)
            .edit()
            .putString(NEW_TOKEN, newToken)
            .apply()
    }

    override suspend fun pushReceived(beep: Boolean): Unit =
        withContext(ioDispatcher) {
            suspendCoroutine {
                megaChatApi.pushReceived(
                    beep, OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestPushReceivedCompleted()
                    )
                )
            }
        }

    private fun onRequestRegisterPushNotificationsCompleted(continuation: Continuation<String>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.text))
            } else {
                continuation.failWithError(error)
            }
        }

    private fun onRequestPushReceivedCompleted() =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                if (!megaApi.isEphemeralPlusPlus) {
                    ChatAdvancedNotificationBuilder.newInstance(context)
                        .generateChatNotification(request)
                }
            } else {
                Timber.e("Error TYPE_PUSH_RECEIVED: ${error.errorString}")
            }
        }
}