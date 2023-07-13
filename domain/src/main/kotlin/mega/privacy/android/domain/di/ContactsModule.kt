package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.AddNewContacts
import mega.privacy.android.domain.usecase.ApplyContactUpdates
import mega.privacy.android.domain.usecase.GetContactCredentials
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.RequestLastGreen
import mega.privacy.android.domain.usecase.ResetCredentials
import mega.privacy.android.domain.usecase.VerifyCredentials
import mega.privacy.android.domain.usecase.account.DefaultUpdateCurrentUserName
import mega.privacy.android.domain.usecase.account.UpdateCurrentUserName
import mega.privacy.android.domain.usecase.contact.DefaultReloadContactDatabase
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetContactItem
import mega.privacy.android.domain.usecase.contact.GetCurrentUserAliases
import mega.privacy.android.domain.usecase.contact.GetCurrentUserFirstName
import mega.privacy.android.domain.usecase.contact.GetCurrentUserLastName
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import mega.privacy.android.domain.usecase.contact.GetUserLastName
import mega.privacy.android.domain.usecase.contact.ReloadContactDatabase

/**
 * Contacts module.
 *
 * Provides all contacts implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ContactsModule {

    @Binds
    abstract fun bindUpdateCurrentUserName(implementation: DefaultUpdateCurrentUserName): UpdateCurrentUserName

    @Binds
    abstract fun bindResetContactDatabase(implementation: DefaultReloadContactDatabase): ReloadContactDatabase

    companion object {
        @Provides
        fun provideMonitorContactRequestUpdates(contactsRepository: ContactsRepository): MonitorContactRequestUpdates =
            MonitorContactRequestUpdates(contactsRepository::monitorContactRequestUpdates)

        @Provides
        fun provideRequestLastGreen(contactsRepository: ContactsRepository): RequestLastGreen =
            RequestLastGreen(contactsRepository::requestLastGreen)

        @Provides
        fun provideMonitorContactUpdates(contactsRepository: ContactsRepository): MonitorContactUpdates =
            MonitorContactUpdates(contactsRepository::monitorContactUpdates)

        @Provides
        fun provideApplyContactUpdates(contactsRepository: ContactsRepository): ApplyContactUpdates =
            ApplyContactUpdates(contactsRepository::applyContactUpdates)

        @Provides
        fun provideAddNewContacts(contactsRepository: ContactsRepository): AddNewContacts =
            AddNewContacts(contactsRepository::addNewContacts)

        @Provides
        fun provideGetContactCredentials(contactsRepository: ContactsRepository): GetContactCredentials =
            GetContactCredentials(contactsRepository::getContactCredentials)

        @Provides
        fun provideVerifyCredentials(contactsRepository: ContactsRepository): VerifyCredentials =
            VerifyCredentials(contactsRepository::verifyCredentials)

        @Provides
        fun provideResetCredentials(contactsRepository: ContactsRepository): ResetCredentials =
            ResetCredentials(contactsRepository::resetCredentials)

        @Provides
        fun provideGetCurrentUserFirstName(contactsRepository: ContactsRepository): GetCurrentUserFirstName =
            GetCurrentUserFirstName(contactsRepository::getCurrentUserFirstName)

        @Provides
        fun provideGetCurrentUserLastName(contactsRepository: ContactsRepository): GetCurrentUserLastName =
            GetCurrentUserLastName(contactsRepository::getCurrentUserLastName)

        @Provides
        fun provideGetUserFirstName(contactsRepository: ContactsRepository): GetUserFirstName =
            GetUserFirstName(contactsRepository::getUserFirstName)

        @Provides
        fun provideGetUserLastName(contactsRepository: ContactsRepository): GetUserLastName =
            GetUserLastName(contactsRepository::getUserLastName)

        @Provides
        fun provideGetCurrentUserAliases(contactsRepository: ContactsRepository): GetCurrentUserAliases =
            GetCurrentUserAliases(contactsRepository::getCurrentUserAliases)

        @Provides
        fun provideGetContactEmail(contactsRepository: ContactsRepository): GetContactEmail =
            GetContactEmail(contactsRepository::getContactEmail)

        @Provides
        fun provideContactItem(contactsRepository: ContactsRepository): GetContactItem =
            GetContactItem(contactsRepository::getContactItem)
    }
}
