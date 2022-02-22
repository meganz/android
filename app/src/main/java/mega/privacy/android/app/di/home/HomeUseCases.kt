package mega.privacy.android.app.di.home

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class HomeUseCases {

    @Binds
    abstract fun bindMonitorChatNotificationCount(implementation: DefaultMonitorChatNotificationCount): MonitorChatNotificationCount

    @Binds
    abstract fun bindHasIncomingCall(implementation: DefaultHasIncomingCall): HasIncomingCall

    @Binds
    abstract fun bindIsOnCall(implementation: DefaultIsOnCall): IsOnCall

}