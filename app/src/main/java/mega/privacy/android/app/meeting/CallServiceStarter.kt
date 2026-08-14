package mega.privacy.android.app.meeting

import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.navigation.destination.ChatNavKey
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts the foreground [CallService] for an ongoing or incoming call.
 *
 * Injectable replacement for the legacy `MegaApplication.openCallService` facade.
 */
@Singleton
class CallServiceStarter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Start the foreground [CallService] for the given chat.
     *
     * @param chatId Chat ID of the call. Ignored when [MEGACHAT_INVALID_HANDLE].
     */
    operator fun invoke(chatId: Long) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            Timber.d("Start call Service. Chat ID = $chatId")
            Intent(context, CallService::class.java).apply {
                putExtra(ChatNavKey.LEGACY_CHAT_ID, chatId)
                context.startForegroundService(this)
            }
        }
    }
}

/**
 * Entry point to resolve [CallServiceStarter] in classes that cannot use constructor or field
 * injection (Compose UI and hand-instantiated static objects).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CallServiceStarterEntryPoint {

    /**
     * The [CallServiceStarter] instance.
     */
    fun callServiceStarter(): CallServiceStarter
}
