package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorLastGreenUpdates


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
}