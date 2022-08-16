package mega.privacy.android.app.mediaplayer.service

import android.os.Binder
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway

/**
 * This class will be what is returned when an activity binds to this service.
 * The activity will also use this to know what it can get from our service to know
 * about the audio playback.
 *
 * @property serviceGateway MediaPlayerServiceGateway
 * @property playerServiceViewModelGateway ServiceViewModelGateway
 */
class MediaPlayerServiceBinder(
    val serviceGateway: MediaPlayerServiceGateway,
    val playerServiceViewModelGateway: PlayerServiceViewModelGateway,
) : Binder()
