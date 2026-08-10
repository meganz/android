package mega.privacy.android.app.mediaplayer.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.mediaplayer.service.LegacyAudioPlayerService
import mega.privacy.android.domain.usecase.StopAudioService
import javax.inject.Inject

/**
 * The implementation for stop audio service
 */
class DefaultStopAudioService @Inject constructor(@ApplicationContext private val context: Context) :
    StopAudioService {

    override suspend fun invoke() {
        LegacyAudioPlayerService.stopAudioPlayer(context)
    }
}