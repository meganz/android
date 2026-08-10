package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.appstate.global.event.SnackbarEventQueueImpl
import mega.privacy.android.app.presentation.snackbar.SnackBarHandlerImpl
import mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueueReceiver
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class SnackbarModule {

    @Singleton
    @Provides
    fun provideSnackbarEventQueue(
        impl: SnackbarEventQueueImpl,
    ): SnackbarEventQueue = impl

    @Singleton
    @Provides
    fun provideSnackbarEventQueueReceiver(
        impl: SnackbarEventQueueImpl,
    ): SnackbarEventQueueReceiver = impl

    @Singleton
    @Provides
    fun provideSnackBarHandler(
        impl: SnackBarHandlerImpl,
    ): SnackBarHandler = impl
}

