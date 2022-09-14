package mega.privacy.android.app.di.notification

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.presentation.notification.model.mapper.NotificationMapper
import mega.privacy.android.app.presentation.notification.model.mapper.getNotification
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.AcknowledgeUserAlerts
import mega.privacy.android.domain.usecase.DefaultMonitorUserAlerts
import mega.privacy.android.domain.usecase.MonitorEvent
import mega.privacy.android.domain.usecase.MonitorUserAlerts

@Module
@InstallIn(ViewModelComponent::class)
abstract class NotificationUseCases {

    @Binds
    abstract fun bindMonitorUserAlerts(implementation: DefaultMonitorUserAlerts): MonitorUserAlerts

    companion object {
        @Provides
        fun provideAcknowledgeUserAlerts(repository: NotificationsRepository): AcknowledgeUserAlerts =
            AcknowledgeUserAlerts(repository::acknowledgeUserAlerts)

        @Provides
        fun provideNotificationMapper(): NotificationMapper = ::getNotification

        @Provides
        fun provideMonitorEventUpdate(repository: NotificationsRepository): MonitorEvent =
            MonitorEvent(repository::monitorEvent)
    }

}