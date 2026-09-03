package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.qualifier.AudioPlaybackModeMutex
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AudioPlaybackModeModule {

    @AudioPlaybackModeMutex
    @Singleton
    @Provides
    fun provideAudioPlaybackModeMutex(): Mutex = Mutex()
}
