package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ChatRequestMapper
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.PushesRepository
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
 * @property context           Required for getting shared preferences.
 * @property megaApi           Required for registering push notifications.
 * @property ioDispatcher      Required for launching coroutines.
 * @property megaChatApi       Required for notifying about pushes.
 * @property chatRequestMapper [ChatRequestMapper]
 */
internal class DefaultPushesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApi: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaChatApi: MegaChatApiGateway,
    private val chatRequestMapper: ChatRequestMapper,
) : PushesRepository {

    override fun getPushToken(): String =
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE)
            .getString(NEW_TOKEN, "") ?: ""

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

    private fun onRequestRegisterPushNotificationsCompleted(continuation: Continuation<String>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.text))
            } else {
                continuation.failWithError(error)
            }
        }

    override fun setPushToken(newToken: String) {
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE)
            .edit()
            .putString(NEW_TOKEN, newToken)
            .apply()
    }

    override suspend fun pushReceived(beep: Boolean): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApi.pushReceived(beep,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestPushReceivedCompleted(continuation)
                    )
                )
            }
        }

    override suspend fun clearPushToken() = withContext(ioDispatcher) {
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE).edit()
            .clear().apply()
    }

    private fun onRequestPushReceivedCompleted(continuation: Continuation<ChatRequest>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                Timber.d("PushMessageWorker onRequestPushReceivedCompleted")
                if (!megaApi.isEphemeralPlusPlus) {
                    continuation.resumeWith(Result.success(chatRequestMapper(request)))
                } else {
                    continuation.failWithError(error)
                }
            } else {
                continuation.failWithError(error)
            }
        }

    companion object {
        private const val PUSH_TOKEN = "PUSH_TOKEN"
        private const val NEW_TOKEN = "NEW_TOKEN"
    }
}