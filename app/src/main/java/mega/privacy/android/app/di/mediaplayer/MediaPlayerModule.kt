package mega.privacy.android.app.di.mediaplayer

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.mediaplayer.facade.MediaPlayerFacade
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
    fun provideVideoPlayerFacade(@ApplicationContext context: Context): MediaPlayerFacade =
        MediaPlayerFacade(context)

    /**
     * Provide the MediaPlayerFacade implementation for audio player
     */
    @AudioPlayer
    @Singleton
    @Provides
    fun provideAudioPlayerFacade(@ApplicationContext context: Context): MediaPlayerFacade =
        MediaPlayerFacade(context)
}