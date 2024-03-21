package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.environment.IsConnectivityInRoamingStateUseCase
import javax.inject.Inject

/**
 * Use case to get local contacts
 *
 * @property defaultDispatcher
 * @property contactsRepository [ContactsRepository]
 * @property isConnectivityInRoamingStateUseCase [IsConnectivityInRoamingStateUseCase]
 * @property getNormalizedPhoneNumberByNetworkUseCase [GetNormalizedPhoneNumberByNetworkUseCase]
 * @property isEmailValidUseCase [IsEmailValidUseCase]
 */
class GetLocalContactsUseCase @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val contactsRepository: ContactsRepository,
    private val isConnectivityInRoamingStateUseCase: IsConnectivityInRoamingStateUseCase,
    private val getNormalizedPhoneNumberByNetworkUseCase: GetNormalizedPhoneNumberByNetworkUseCase,
    private val isEmailValidUseCase: IsEmailValidUseCase,
) {

    /**
     * Invocation method to retrieve the user's local contacts
     */
    suspend operator fun invoke(): List<LocalContact> = withContext(defaultDispatcher) {
        val localContactsDeferred = async { contactsRepository.getLocalContacts() }
        val localContactPhoneNumbersDeferred =
            async { contactsRepository.getLocalContactNumbers() }
        val localContactEmailsDeferred =
            async { contactsRepository.getLocalContactEmailAddresses() }

        val localContacts = localContactsDeferred.await()
        val localPhoneNumbers = localContactPhoneNumbersDeferred.await()
        val localEmail = localContactEmailsDeferred.await()

        localContacts.map { contact ->
            // Get phone numbers and the normalized phone numbers
            val phoneNumberList = mutableListOf<String>()
            val normalizedPhoneNumberList = mutableListOf<String>()
            localPhoneNumbers.firstOrNull { it.id == contact.id }?.let {
                it.phoneNumbers.forEach { phone ->
                    phoneNumberList.add(phone)

                    it.normalizedPhoneNumbers.forEach { number ->
                        var normalizedNumber = number
                        // If roaming, don't normalize the phone number.
                        if ((normalizedNumber.isBlank() || !normalizedNumber.startsWith(PLUS)) && !isConnectivityInRoamingStateUseCase()) {
                            // use current country code to normalize the phone number.
                            normalizedNumber =
                                getNormalizedPhoneNumberByNetworkUseCase(phone).orEmpty()
                        }

                        if (normalizedNumber.isNotBlank() && normalizedNumber.startsWith(PLUS)) {
                            normalizedPhoneNumberList.add(normalizedNumber)
                        }
                    }
                }
            }

            // Get emails
            val emailList = mutableListOf<String>()
            localEmail.forEach {
                if (it.id == contact.id) {
                    val emails = it.emails.filter { email -> isEmailValidUseCase(email) }
                    if (emails.isNotEmpty()) emailList.addAll(emails)
                }
            }

            LocalContact(
                id = contact.id,
                name = contact.name,
                phoneNumbers = phoneNumberList,
                normalizedPhoneNumbers = normalizedPhoneNumberList,
                emails = emailList
            )
        }.sortedBy { it.name }
    }

    companion object {
        /**
         * A valid normalized phone number must start with '+'.
         * Invalid phone numbers are not allowed to be sent to server.
         */
        private const val PLUS = "+"
    }
}
