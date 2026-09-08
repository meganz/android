package mega.privacy.android.app.mediaplayer

import android.content.Context
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.LegacyAudioPlayerService
import mega.privacy.android.app.utils.CallUtil.participatingInACall

/**
 * Dispatches external pause/resume/stop commands to both audio player services.
 *
 * Only one of the two runs at a time (chosen by the AudioPlayerRevamp flag), but callers have
 * no way to know which; each service guards against being spuriously started, so commanding
 * the idle one is a no-op.
 */
object AudioPlayerCommands {

    /**
     * Pause the audio player when playing video, playing/recording an audio clip, or when a
     * call starts. Remembered, so [resumeAudioPlayers] can undo it.
     *
     * @param context Android context
     */
    @JvmStatic
    fun pauseAudioPlayers(context: Context) {
        LegacyAudioPlayerService.pauseAudioPlayer(context)
        AudioPlayerService.pauseAudioPlayer(context)
    }

    /**
     * Resume the audio player when returning from [pauseAudioPlayers].
     *
     * @param context Android context
     */
    @JvmStatic
    fun resumeAudioPlayers(context: Context) {
        LegacyAudioPlayerService.resumeAudioPlayer(context)
        AudioPlayerService.resumeAudioPlayer(context)
    }

    /**
     * Resume the audio player when returning from [pauseAudioPlayers], unless a call is still
     * ongoing.
     *
     * @param context Android context
     */
    @JvmStatic
    fun resumeAudioPlayersIfNotInCall(context: Context) {
        if (!participatingInACall()) {
            resumeAudioPlayers(context)
        }
    }
}
