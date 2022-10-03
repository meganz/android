package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.AddNewContacts
import mega.privacy.android.domain.usecase.ApplyContactUpdates
import mega.privacy.android.domain.usecase.GetContactData
import mega.privacy.android.domain.usecase.GetVisibleContacts
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

    @Provides
    fun provideGetVisibleContacts(contactsRepository: ContactsRepository): GetVisibleContacts =
        GetVisibleContacts(contactsRepository::getVisibleContacts)

    @Provides
    fun provideGetContactData(contactsRepository: ContactsRepository): GetContactData =
        GetContactData(contactsRepository::getContactData)

    @Provides
    fun provideApplyContactUpdates(contactsRepository: ContactsRepository): ApplyContactUpdates =
        ApplyContactUpdates(contactsRepository::applyContactUpdates)

    @Provides
    fun provideAddNewContacts(contactsRepository: ContactsRepository): AddNewContacts =
        AddNewContacts(contactsRepository::addNewContacts)
}