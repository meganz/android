package mega.privacy.android.app.di.mediaplayer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.mediaplayer.gateway.AudioMediaControllerFacade
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway

/**
 * Hilt module that binds [AudioMediaControllerGateway] to [AudioMediaControllerFacade].
 *
 * No scope annotation — a new [AudioMediaControllerFacade] is created per injection site,
 * and every owner must call [AudioMediaControllerGateway.release] in its own cleanup:
 * [mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerViewModel] (activity-scoped,
 * released when the Activity is destroyed) and
 * [mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerQueueViewModel]
 * (entry-scoped, released when the queue's ViewModel is cleared).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AudioMediaControllerModule {

    @Binds
    abstract fun bindAudioMediaControllerGateway(
        impl: AudioMediaControllerFacade,
    ): AudioMediaControllerGateway
}
