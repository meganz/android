package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorLastGreenUpdates
import mega.privacy.android.domain.usecase.MonitorOnlineStatusUpdates
import mega.privacy.android.domain.usecase.RequestLastGreen
import mega.privacy.android.domain.usecase.StartConversation

/**
 * Contacts module.
 *
 * Provides all contacts implementation.
 */
@Module
@InstallIn(ViewModelComponent::class)
class ContactsModule {

    @Provides
    fun provideMonitorContactRequestUpdates(contactsRepository: ContactsRepository): MonitorContactRequestUpdates =
        MonitorContactRequestUpdates(contactsRepository::monitorContactRequestUpdates)

    @Provides
    fun provideMonitorLastSeenUpdates(contactsRepository: ContactsRepository): MonitorLastGreenUpdates =
        MonitorLastGreenUpdates(contactsRepository::monitorChatPresenceLastGreenUpdates)

    @Provides
    fun provideRequestLastGreen(contactsRepository: ContactsRepository): RequestLastGreen =
        RequestLastGreen(contactsRepository::requestLastGreen)

    @Provides
    fun provideMonitorContactUpdates(contactsRepository: ContactsRepository): MonitorContactUpdates =
        MonitorContactUpdates(contactsRepository::monitorContactUpdates)

    @Provides
    fun provideStartConversation(contactsRepository: ContactsRepository): StartConversation =
        StartConversation(contactsRepository::startConversation)

    @Provides
    fun provideMonitorChatOnlineStatusUpdates(contactsRepository: ContactsRepository): MonitorOnlineStatusUpdates =
        MonitorOnlineStatusUpdates(contactsRepository::monitorChatOnlineStatusUpdates)
}