package mega.privacy.android.app.di.pushes

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.PushReceived
import mega.privacy.android.domain.usecase.GetPushToken
import mega.privacy.android.domain.usecase.RegisterPushNotifications
import mega.privacy.android.domain.usecase.SetPushToken

/**
 * Pushes use cases module.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushesModule {

    companion object {
        @Provides
        fun provideGetPushToken(pushesRepository: PushesRepository): GetPushToken =
            GetPushToken(pushesRepository::getPushToken)

        @Provides
        fun provideRegisterPushNotification(pushesRepository: PushesRepository): RegisterPushNotifications =
            RegisterPushNotifications(pushesRepository::registerPushNotifications)

        @Provides
        fun provideSetPushToken(pushesRepository: PushesRepository): SetPushToken =
            SetPushToken(pushesRepository::setPushToken)

        @Provides
        fun providePushReceived(pushesRepository: PushesRepository): PushReceived =
            PushReceived(pushesRepository::pushReceived)

        @Provides
        fun provideMonitorContactRequestUpdates(contactsRepository: ContactsRepository): MonitorContactRequestUpdates =
            MonitorContactRequestUpdates(contactsRepository::monitorContactRequestUpdates)
    }
}