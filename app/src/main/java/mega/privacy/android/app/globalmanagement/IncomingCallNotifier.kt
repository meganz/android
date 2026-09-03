package mega.privacy.android.app.globalmanagement

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder
import mega.privacy.android.app.mediaplayer.service.LegacyAudioPlayerService
import mega.privacy.android.app.meeting.RtcAudioManagerController
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatCall
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the side effects required to present an incoming call notification: it pauses any
 * playing audio, (re)creates the ringing audio manager and builds the incoming call notification.
 *
 * Marking a chat's notification as shown is intentionally left to each caller so this collaborator
 * does not depend on [mega.privacy.android.app.components.ChatManagement], which would otherwise
 * form a dependency cycle.
 */
@Singleton
class IncomingCallNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rtcAudioManagerController: RtcAudioManagerController,
) {

    /**
     * Present the incoming call notification for [incomingCall] and start ringing.
     *
     * @param incomingCall The incoming call.
     */
    operator fun invoke(incomingCall: MegaChatCall) {
        Timber.d("Show incoming call notification and start to sound. Chat ID is ${incomingCall.chatid}")
        LegacyAudioPlayerService.pauseAudioPlayer(context)
        rtcAudioManagerController.createOrUpdateAudioManager(
            false,
            Constants.AUDIO_MANAGER_CALL_RINGING,
        )
        showNotification(incomingCall)
    }

    @VisibleForTesting
    internal fun showNotification(incomingCall: MegaChatCall) {
        ChatAdvancedNotificationBuilder.newInstance(context)
            .showOneCallNotification(incomingCall)
    }
}
