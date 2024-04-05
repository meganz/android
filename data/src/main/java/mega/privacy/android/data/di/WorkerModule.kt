package mega.privacy.android.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.worker.ForegroundSetter

/**
 * Module for workers
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    /**
     * Provides [ForegroundSetter] to its default null value
     */
    @Provides
    fun provideForegroundSetter(): ForegroundSetter? = null
}