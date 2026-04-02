package mega.privacy.android.app.di.mediaplayer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
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
import timber.log.Timber
import java.io.File
import javax.inject.Singleton

/**
 * Module class regarding media player
 */
@Module
@OptIn(UnstableApi::class)
@InstallIn(SingletonComponent::class)
class MediaPlayerModule {

    @Singleton
    @Provides
    fun provideMediaPlayerSimpleCache(@ApplicationContext context: Context): SimpleCache? {
        repeat(2) { attempt ->
            runCatching {
                return SimpleCache(
                    File(context.cacheDir, CACHE_DIR_NAME),
                    LeastRecentlyUsedCacheEvictor(CACHE_MAX_SIZE),
                    StandaloneDatabaseProvider(context),
                )
            }.onFailure { e ->
                Timber.e(e, "SimpleCache init failed (attempt ${attempt + 1})")
                File(context.cacheDir, CACHE_DIR_NAME).deleteRecursively()
            }
        }
        return null
    }

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
        simpleCache: SimpleCache?,
    ): MediaPlayerFacade =
        MediaPlayerFacade(
            context,
            crashReporter,
            repeatToggleModeMapper,
            exoPlayerRepeatModeMapper,
            simpleCache
        )

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
        simpleCache: SimpleCache?,
    ): MediaPlayerFacade =
        MediaPlayerFacade(
            context,
            crashReporter,
            repeatToggleModeMapper,
            exoPlayerRepeatModeMapper,
            simpleCache
        )

    /**
     * Provide the implementation for [StopAudioService]
     */
    @Provides
    fun provideStopAudioPlayerService(@ApplicationContext context: Context): StopAudioService =
        DefaultStopAudioService(context)

    companion object {
        private const val CACHE_MAX_SIZE = 500 * 1024 * 1024L
        private const val CACHE_DIR_NAME = "media_player_cache"
    }
}
