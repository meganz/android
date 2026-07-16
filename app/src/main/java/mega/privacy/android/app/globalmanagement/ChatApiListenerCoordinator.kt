package mega.privacy.android.app.globalmanagement

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.listeners.GlobalChatListener
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.CallSoundsController
import mega.privacy.android.app.meeting.listeners.MeetingListener
import mega.privacy.android.app.usecase.call.MonitorCallSoundsUseCase
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the global chat SDK listeners and the call-sounds collection.
 *
 * [register] is idempotent: listeners are added at most once per process until [unregister]
 * removes them, so repeated calls (boot, login retries) never duplicate SDK listeners.
 */
@Singleton
class ChatApiListenerCoordinator @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    private val chatRequestHandler: MegaChatRequestHandler,
    private val megaChatNotificationHandler: MegaChatNotificationHandler,
    private val globalChatListener: GlobalChatListener,
    private val monitorCallSoundsUseCase: MonitorCallSoundsUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val meetingListener = MeetingListener()
    private val soundsController = CallSoundsController()
    private var callSoundsJob: Job? = null
    private var registered = false

    /**
     * Registers the global chat listeners and starts the call-sounds collection.
     * No-op when already registered.
     */
    fun register() {
        if (!registered) {
            Timber.d("Add listeners of megaChatApi")
            megaChatApi.apply {
                addChatRequestListener(chatRequestHandler)
                addChatNotificationListener(megaChatNotificationHandler)
                addChatListener(globalChatListener)
                addChatCallListener(meetingListener)
            }
            registered = true
            checkCallSounds()
        }
    }

    /**
     * Removes the global chat listeners and stops the call-sounds collection.
     */
    fun unregister() {
        try {
            megaChatApi.apply {
                removeChatRequestListener(chatRequestHandler)
                removeChatNotificationListener(megaChatNotificationHandler)
                removeChatListener(globalChatListener)
                removeChatCallListener(meetingListener)
            }
            callSoundsJob?.cancel()
            callSoundsJob = null
            registered = false
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Check the changes of the meeting to play the right sound
     */
    private fun checkCallSounds() {
        callSoundsJob?.cancel()
        callSoundsJob = applicationScope.launch {
            monitorCallSoundsUseCase()
                .collectLatest { next: CallSoundType ->
                    soundsController.playSound(next)
                }
        }
    }
}
