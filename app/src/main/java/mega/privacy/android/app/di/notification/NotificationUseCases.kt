package mega.privacy.android.app.di.notification

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.AcknowledgeUserAlerts
import mega.privacy.android.domain.usecase.DefaultMonitorUserAlerts
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
    }

}