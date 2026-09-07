package mega.privacy.android.app.mediaplayer.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.LegacyAudioPlayerService
import mega.privacy.android.domain.usecase.StopAudioService
import javax.inject.Inject

/**
 * The implementation for stop audio service.
 *
 * Stops both audio player services: only one runs at a time (chosen by the AudioPlayerRevamp
 * flag), but the caller has no way to know which, and stopping the idle one is a no-op.
 */
class DefaultStopAudioService @Inject constructor(@ApplicationContext private val context: Context) :
    StopAudioService {

    override suspend fun invoke() {
        LegacyAudioPlayerService.stopAudioPlayer(context)
        AudioPlayerService.stopAudioPlayer(context)
    }
}
