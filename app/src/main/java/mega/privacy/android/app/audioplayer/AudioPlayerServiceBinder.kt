package mega.privacy.android.app.audioplayer

import android.os.Binder

/**
 * This class will be what is returned when an activity binds to this service.
 * The activity will also use this to know what it can get from our service to know
 * about the video playback.
 */
class AudioPlayerServiceBinder(
    val service: AudioPlayerService,
) : Binder()
