package mega.privacy.android.app.mediaplayer.facade

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.domain.usecase.StopAudioServiceGateway
import javax.inject.Inject

/**
 * The implementation for stop audio service
 */
class StopAudioServiceFacade @Inject constructor(@ApplicationContext private val context: Context) :
    StopAudioServiceGateway {

    override suspend fun invoke() {
        MediaPlayerService.stopAudioPlayer(context)
    }
}