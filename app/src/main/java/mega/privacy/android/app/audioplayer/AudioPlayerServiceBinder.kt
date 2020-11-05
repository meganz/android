package mega.privacy.android.app.audioplayer

import android.os.Binder
import androidx.lifecycle.LiveData
import com.google.android.exoplayer2.SimpleExoPlayer

/**
 * This class will be what is returned when an activity binds to this service.
 * The activity will also use this to know what it can get from our service to know
 * about the video playback.
 */
class AudioPlayerServiceBinder(
    val exoPlayer: SimpleExoPlayer,
    val metadata: LiveData<Metadata>,
) : Binder()
