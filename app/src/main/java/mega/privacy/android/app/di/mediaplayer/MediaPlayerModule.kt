package mega.privacy.android.app.di.mediaplayer

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.mediaplayer.facade.MediaPlayerFacade
import mega.privacy.android.app.mediaplayer.mapper.ExoPlayerRepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.app.mediaplayer.usecase.DefaultStopAudioService
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.StopAudioService
import javax.inject.Singleton

/**
 * Module class regarding media player
 */
@Module
@InstallIn(SingletonComponent::class)
class MediaPlayerModule {

    /**
     * Provide the MediaPlayerFacade implementation for video player
     */
    @VideoPlayer
    @Singleton
    @Provides
    fun provideVideoPlayerFacade(
        @ApplicationContext context: Context,
        crashReporter: CrashReporter,
        repeatToggleModeMapper: RepeatToggleModeByExoPlayerMapper,
        exoPlayerRepeatModeMapper: ExoPlayerRepeatModeMapper,
    ): MediaPlayerFacade =
        MediaPlayerFacade(context, crashReporter, repeatToggleModeMapper, exoPlayerRepeatModeMapper)

    /**
     * Provide the MediaPlayerFacade implementation for audio player
     */
    @AudioPlayer
    @Singleton
    @Provides
    fun provideAudioPlayerFacade(
        @ApplicationContext context: Context,
        crashReporter: CrashReporter,
        repeatToggleModeMapper: RepeatToggleModeByExoPlayerMapper,
        exoPlayerRepeatModeMapper: ExoPlayerRepeatModeMapper,
    ): MediaPlayerFacade =
        MediaPlayerFacade(context, crashReporter, repeatToggleModeMapper, exoPlayerRepeatModeMapper)

    /**
     * Provide the implementation for [StopAudioService]
     */
    @Provides
    fun provideStopAudioPlayerService(@ApplicationContext context: Context): StopAudioService =
        DefaultStopAudioService(context)
}
