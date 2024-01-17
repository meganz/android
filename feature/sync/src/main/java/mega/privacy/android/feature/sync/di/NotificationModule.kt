package mega.privacy.android.feature.sync.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.notifications.GetFeatureNotificationCountUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.GetStalledNotificationCountUseCase

@Module
@InstallIn(SingletonComponent::class)
internal interface NotificationModule {

    @Binds
    @IntoSet
    fun bindGetSyncNotificationUseCase(impl: GetStalledNotificationCountUseCase): GetFeatureNotificationCountUseCase
}