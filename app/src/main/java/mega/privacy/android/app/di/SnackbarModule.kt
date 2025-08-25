package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.appstate.global.event.SnackbarEventQueueImpl
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SnackbarModule {

    @Singleton
    @Provides
    fun provideSnackbarEventQueue(
        impl: SnackbarEventQueueImpl
    ): SnackbarEventQueue = impl
}

