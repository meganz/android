package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetCurrentUserFullNameTest {

    private lateinit var underTest: DefaultGetCurrentUserFullName

    private val contactsRepository: ContactsRepository = mock()
    private val accountRepository: AccountRepository = mock()

    private val defaultFirstName = "defaultFirstName"
    private val defaultLastName = "defaultLastName"


    @Before
    fun setUp() {
        underTest = DefaultGetCurrentUserFullName(
            ioDispatcher = UnconfinedTestDispatcher(),
            contactsRepository = contactsRepository,
            accountRepository = accountRepository
        )
    }

    @Test
    fun `test that last name is used when first name is empty and last name is not`() = runTest {
        val expectedLastName = "LastName"
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(contactsRepository.getCurrentUserLastName(any())).thenReturn(expectedLastName)

        assertThat(
            underTest(true, defaultFirstName, defaultLastName)
        ).isEqualTo(expectedLastName)
    }

    @Test
    fun `test that first name and last name are concatenated when both are not blank`() = runTest {
        val expectedLastName = "LastName"
        val expectedFirstName = "FirstName"
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn(expectedFirstName)
        whenever(contactsRepository.getCurrentUserLastName(any())).thenReturn(expectedLastName)

        assertThat(
            underTest(true, defaultFirstName, defaultLastName)
        ).isEqualTo("$expectedFirstName $expectedLastName")
    }

    @Test
    fun `test that email id is used when both first name and last name are blank`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(contactsRepository.getCurrentUserLastName(any())).thenReturn("")
        whenever(accountRepository.accountEmail).thenReturn("abc@mega.co.nz")

        assertThat(
            underTest(true, defaultFirstName, defaultLastName)
        ).isEqualTo("abc")
    }

    @Test
    fun `test that default names are used when both first name, last name and email are null or blank`() =
        runTest {
            whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
            whenever(contactsRepository.getCurrentUserLastName(any())).thenReturn("")
            whenever(accountRepository.accountEmail).thenReturn(null)

            assertThat(
                underTest(true, defaultFirstName, defaultLastName)
            ).isEqualTo("$defaultFirstName $defaultLastName")
        }
}