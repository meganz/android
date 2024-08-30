package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.appstart.AppStopTask
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.StartSyncWorkerTask

/**
 * Module to provide the tasks to be executed when the app stops
 */
@Module
@InstallIn(SingletonComponent::class)
interface AppStopModule {

    companion object {
        /**
         * Provides the task to start the sync worker when the app stops
         */
        @Provides
        @IntoSet
        fun provideStartSyncWorkerTask(task: StartSyncWorkerTask): AppStopTask =
            task
    }
}