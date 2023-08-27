package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.cameraupload.facade.WorkerFacade
import mega.privacy.android.app.data.facade.AccountInfoFacade
import mega.privacy.android.app.data.facade.AlbumStringResourceFacade
import mega.privacy.android.app.data.facade.ContactFacade
import mega.privacy.android.app.di.mediaplayer.AudioPlayer
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.facade.MediaPlayerFacade
import mega.privacy.android.app.mediaplayer.gateway.AudioPlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerServiceViewModel
import mega.privacy.android.app.meeting.facade.RTCAudioManagerFacade
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.facade.AlbumStringResourceGateway
import mega.privacy.android.data.gateway.WorkerGateway
import mega.privacy.android.data.wrapper.ContactWrapper
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

    @Binds
    abstract fun bindContactWrapper(implementation: ContactFacade): ContactWrapper

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
     * Provide AudioPlayerServiceViewModelGateway implementation
     */
    @Binds
    abstract fun bindsAudioPlayerServiceViewModelGateway(implementation: AudioPlayerServiceViewModel): AudioPlayerServiceViewModelGateway

    @Binds
    @Singleton
    abstract fun bindRTCAudioManagerGateway(implementation: RTCAudioManagerFacade): RTCAudioManagerGateway

    @Binds
    @Singleton
    abstract fun bindWorkerGateway(implementation: WorkerFacade): WorkerGateway
}
