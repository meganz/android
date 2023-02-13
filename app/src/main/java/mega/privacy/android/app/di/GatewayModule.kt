package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.facade.AccountInfoFacade
import mega.privacy.android.app.data.facade.AlbumStringResourceFacade
import mega.privacy.android.app.di.mediaplayer.AudioPlayer
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.facade.MediaPlayerFacade
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceViewModel
import mega.privacy.android.app.meeting.facade.CameraFacade
import mega.privacy.android.app.meeting.facade.RTCAudioManagerFacade
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.facade.AlbumStringResourceGateway
import javax.inject.Singleton

/**
 * Gateway module
 *
 * Registers bindings for gateway dependencies used by the repository classes.
 *
 * Facades and wrappers used by the repositories will also be provided here until they are replaced
 * with repository code or gateways.
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GatewayModule {

    @Binds
    abstract fun bindAccountInfoWrapper(implementation: AccountInfoFacade): AccountInfoWrapper

    /**
     * Provides [AlbumStringResourceGateway] implementation
     */
    @Binds
    abstract fun bindAlbumStringResourceGateway(implementation: AlbumStringResourceFacade): AlbumStringResourceGateway

    /**
     * Provide MediaPlayerGateway implementation
     */
    @AudioPlayer
    @Binds
    @Singleton
    abstract fun bindsAudioPlayerGateway(@AudioPlayer mediaPlayerFacade: MediaPlayerFacade): MediaPlayerGateway

    /**
     * Provide MediaPlayerGateway implementation
     */
    @VideoPlayer
    @Binds
    @Singleton
    abstract fun bindsVideoPlayerGateway(@VideoPlayer mediaPlayerFacade: MediaPlayerFacade): MediaPlayerGateway

    /**
     * Provide PlayerServiceViewModelGateway implementation
     */
    @Binds
    abstract fun bindsPlayerServiceViewModelGateway(implementation: MediaPlayerServiceViewModel): PlayerServiceViewModelGateway

    @Binds
    @Singleton
    abstract fun bindRTCAudioManagerGateway(implementation: RTCAudioManagerFacade): RTCAudioManagerGateway

    /**
     * Provides [CameraGateway] implementation
     */
    @Binds
    @Singleton
    abstract fun bindCameraGateway(implementation: CameraFacade): CameraGateway
}