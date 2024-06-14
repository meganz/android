package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.environment.IsConnectivityInRoamingStateUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLocalContactsUseCaseTest {

    private lateinit var underTest: GetLocalContactsUseCase

    private val contactsRepository: ContactsRepository = mock()
    private val isConnectivityInRoamingStateUseCase: IsConnectivityInRoamingStateUseCase = mock()
    private val getNormalizedPhoneNumberByNetworkUseCase: GetNormalizedPhoneNumberByNetworkUseCase =
        mock()
    private val isEmailValidUseCase: IsEmailValidUseCase = mock()
    private val defaultDispatcher = UnconfinedTestDispatcher()

    private val phoneNumber = "1234567890"
    private val normalizedPhoneNumber = "+621234567890"
    private val photoUri = UriPath("photoUri")

    @BeforeEach
    fun setup() {
        underTest = GetLocalContactsUseCase(
            defaultDispatcher = defaultDispatcher,
            contactsRepository = contactsRepository,
            isConnectivityInRoamingStateUseCase = isConnectivityInRoamingStateUseCase,
            getNormalizedPhoneNumberByNetworkUseCase = getNormalizedPhoneNumberByNetworkUseCase,
            isEmailValidUseCase = isEmailValidUseCase
        )
    }

    @Test
    fun `test that a list of sorted local contacts with non empty numbers and emails is returned`() =
        runTest {
            val firstContactName = "name1"
            val secondContactName = "name2"
            val availableLocalContacts = listOf(
                LocalContact(
                    id = 1L,
                    name = secondContactName
                ),
                LocalContact(
                    id = 2L,
                    name = firstContactName,
                    photoUri = photoUri
                )
            )
            whenever(contactsRepository.getLocalContacts()).thenReturn(availableLocalContacts)
            val availableLocalContactNumbers = listOf(
                LocalContact(
                    id = 1L,
                    phoneNumbers = listOf(phoneNumber),
                    normalizedPhoneNumbers = listOf(phoneNumber)
                ),
                LocalContact(id = 2L)
            )
            whenever(contactsRepository.getLocalContactNumbers()).thenReturn(
                availableLocalContactNumbers
            )
            val firstEmail = "test@test.com"
            val secondEmail = "test2@test.com"
            val availableLocalContactEmails = listOf(
                LocalContact(
                    id = 1L,
                    emails = listOf(firstEmail)
                ),
                LocalContact(
                    id = 2L,
                    emails = listOf(secondEmail)
                )
            )
            whenever(contactsRepository.getLocalContactEmailAddresses()).thenReturn(
                availableLocalContactEmails
            )
            whenever(isConnectivityInRoamingStateUseCase()).thenReturn(false)
            whenever(getNormalizedPhoneNumberByNetworkUseCase(phoneNumber)).thenReturn(
                normalizedPhoneNumber
            )
            whenever(isEmailValidUseCase(firstEmail)).thenReturn(true)
            whenever(isEmailValidUseCase(secondEmail)).thenReturn(true)

            val actual = underTest()

            val expected = listOf(
                LocalContact(
                    id = 2L,
                    name = firstContactName,
                    emails = listOf(secondEmail),
                    photoUri = photoUri
                ),
                LocalContact(
                    id = 1L,
                    name = secondContactName,
                    phoneNumbers = listOf(phoneNumber),
                    normalizedPhoneNumbers = listOf(normalizedPhoneNumber),
                    emails = listOf(firstEmail)
                ),
            )
            assertThat(actual).isEqualTo(expected)
        }

    // Declare this variable here because it is only used by the below test case
    // ref:
    // - https://wiki.c2.com/?DeclareVariablesAtFirstUse
    // - stated in Steve McConnell's "Code Complete" book too (Ideally, declare and define each variable close to where it’s first used)
    private val email = "t"

    @ParameterizedTest
    @MethodSource("provideLocalContactWithInvalidEmails")
    fun `test that a list of local contacts with empty emails is returned given invalid emails`(
        availableLocalContactEmails: List<LocalContact>,
    ) = runTest {
        val availableLocalContacts = listOf(LocalContact(id = 1L, photoUri = photoUri))
        whenever(contactsRepository.getLocalContacts()).thenReturn(availableLocalContacts)
        whenever(contactsRepository.getLocalContactNumbers()).thenReturn(emptyList())
        whenever(contactsRepository.getLocalContactEmailAddresses()).thenReturn(
            availableLocalContactEmails
        )
        whenever(isEmailValidUseCase(email)).thenReturn(false)

        val actual = underTest()

        val expected = listOf(LocalContact(id = 1L, photoUri = photoUri))
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideLocalContactWithInvalidEmails() = Stream.of(
        Arguments.of(listOf(LocalContact(id = 1L))),
        Arguments.of(
            listOf(
                LocalContact(
                    id = 2L,
                    emails = listOf(email)
                )
            )
        )
    )

    @Test
    fun `test that a list of local contacts with empty phone numbers is returned when there are no contacts with phone numbers`() =
        runTest {
            val contactID = 1L
            val availableLocalContacts =
                listOf(LocalContact(id = contactID, photoUri = photoUri))
            whenever(contactsRepository.getLocalContacts()).thenReturn(availableLocalContacts)
            whenever(contactsRepository.getLocalContactNumbers()).thenReturn(emptyList())
            whenever(contactsRepository.getLocalContactEmailAddresses()).thenReturn(emptyList())

            val actual = underTest()

            val expected = listOf(LocalContact(id = contactID, photoUri = photoUri))
            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest(name = "roaming status is {0}, is phone number valid: {1}, and normalized number is {2}")
    @MethodSource("provideEmptyAndNonCountryCodeNormalizedPhoneNumberCase")
    fun `test that a list of local contacts with empty normalized phone numbers is returned when`(
        roamingStatus: Boolean,
        isPhoneNumberValid: Boolean,
        nonCountryCodeNormalizedNumber: String,
    ) = runTest {
        val availableLocalContacts = listOf(LocalContact(id = 1L, photoUri = photoUri))
        whenever(contactsRepository.getLocalContacts()).thenReturn(availableLocalContacts)
        val availableLocalContactNumbers = listOf(
            LocalContact(
                id = 1L,
                phoneNumbers = listOf(phoneNumber),
                normalizedPhoneNumbers = listOf(nonCountryCodeNormalizedNumber)
            )
        )
        whenever(contactsRepository.getLocalContactNumbers()).thenReturn(
            availableLocalContactNumbers
        )
        whenever(contactsRepository.getLocalContactEmailAddresses()).thenReturn(emptyList())
        whenever(isConnectivityInRoamingStateUseCase()).thenReturn(roamingStatus)
        whenever(getNormalizedPhoneNumberByNetworkUseCase(phoneNumber)).thenReturn(
            if (isPhoneNumberValid) nonCountryCodeNormalizedNumber else ""
        )

        val actual = underTest()

        val expected = listOf(
            LocalContact(
                id = 1L,
                phoneNumbers = listOf(phoneNumber),
                photoUri = photoUri
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideEmptyAndNonCountryCodeNormalizedPhoneNumberCase() = Stream.of(
        Arguments.of(true, true, " "),
        Arguments.of(true, true, phoneNumber),
        Arguments.of(false, false, "  "),
        Arguments.of(false, false, phoneNumber)
    )

    @AfterEach
    fun tearDown() {
        reset(
            contactsRepository,
            isConnectivityInRoamingStateUseCase,
            getNormalizedPhoneNumberByNetworkUseCase,
            isEmailValidUseCase
        )
    }
}
