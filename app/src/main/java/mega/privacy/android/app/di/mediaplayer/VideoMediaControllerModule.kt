package mega.privacy.android.app.di.mediaplayer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.mediaplayer.gateway.VideoMediaControllerFacade
import mega.privacy.android.feature.mediaplayer.data.gateway.VideoMediaControllerGateway

/**
 * Hilt module that binds [VideoMediaControllerGateway] to [VideoMediaControllerFacade].
 *
 * No scope annotation — a new [VideoMediaControllerFacade], and therefore a new player, is created
 * per injection site, and every owner must call [VideoMediaControllerGateway.release] in its own
 * cleanup. Same shape as [AudioMediaControllerModule].
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class VideoMediaControllerModule {

    @Binds
    abstract fun bindVideoMediaControllerGateway(
        impl: VideoMediaControllerFacade,
    ): VideoMediaControllerGateway
}
