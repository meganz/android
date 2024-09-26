package mega.privacy.android.feature.sync.di

import android.app.NotificationManager
import androidx.core.app.NotificationChannelCompat
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.notifications.GetFeatureNotificationCountUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.GetStalledNotificationCountUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager

@Module
@InstallIn(SingletonComponent::class)
internal interface NotificationModule {

    @Binds
    @IntoSet
    fun bindGetSyncNotificationUseCase(impl: GetStalledNotificationCountUseCase): GetFeatureNotificationCountUseCase

    companion object {
        @Provides
        @IntoSet
        fun provideSyncNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                SyncNotificationManager.CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .setName(SyncNotificationManager.CHANNEL_NAME)
                .setShowBadge(false)
                .setSound(null, null)
                .build()
    }
}