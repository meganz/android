package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.appstart.AppStartTask
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.StopSyncWorkerTask

/**
 * Module to provide the tasks to be executed when the app starts
 */
@Module
@InstallIn(SingletonComponent::class)
interface AppStartModule {

    companion object {
        /**
         * Provides the task to stop the sync worker when the app starts
         */
        @Provides
        @IntoSet
        fun provideStopSyncWorkerTask(task: StopSyncWorkerTask): AppStartTask =
            task
    }
}