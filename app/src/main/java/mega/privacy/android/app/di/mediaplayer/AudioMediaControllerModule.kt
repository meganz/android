package mega.privacy.android.app.di.mediaplayer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.mediaplayer.gateway.AudioMediaControllerFacade
import mega.privacy.android.app.mediaplayer.gateway.AudioMediaControllerGateway

/**
 * Hilt module that binds [AudioMediaControllerGateway] to [AudioMediaControllerFacade].
 *
 * No scope annotation — each injection site (i.e. each [mega.privacy.android.app.mediaplayer.AudioPlayerViewModel])
 * receives its own instance, which is tied to the ViewModel's lifetime via [AudioMediaControllerGateway.release].
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AudioMediaControllerModule {

    @Binds
    abstract fun bindAudioMediaControllerGateway(
        impl: AudioMediaControllerFacade,
    ): AudioMediaControllerGateway
}
