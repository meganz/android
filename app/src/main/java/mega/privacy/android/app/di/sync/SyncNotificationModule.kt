package mega.privacy.android.app.di.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.sync.SyncPendingIntentProviderImpl
import mega.privacy.android.feature.sync.ui.notification.SyncPendingIntentProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncNotificationModule {

    @Binds
    abstract fun bindSyncPendingIntentProvider(
        impl: SyncPendingIntentProviderImpl,
    ): SyncPendingIntentProvider
}
