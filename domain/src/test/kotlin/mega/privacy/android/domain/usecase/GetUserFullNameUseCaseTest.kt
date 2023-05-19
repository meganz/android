package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetUserFullNameUseCaseTest {

    private lateinit var underTest: GetUserFullNameUseCase

    private val contactsRepository: ContactsRepository = mock()
    private val accountRepository: AccountRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = GetUserFullNameUseCase(
            contactsRepository = contactsRepository,
            accountRepository = accountRepository,
        )
    }

    @Test
    fun `test that last name is used when first name is empty and last name is not`() = runTest {
        val expectedLastName = "LastName"
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(contactsRepository.getCurrentUserLastName(any())).thenReturn(expectedLastName)

        assertThat(underTest(true)).isEqualTo(expectedLastName)
    }

    @Test
    fun `test that first name and last name are concatenated when both are not blank`() = runTest {
        val expectedLastName = "LastName"
        val expectedFirstName = "FirstName"
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn(expectedFirstName)
        whenever(contactsRepository.getCurrentUserLastName(any())).thenReturn(expectedLastName)

        assertThat(underTest(true))
            .isEqualTo("$expectedFirstName $expectedLastName")
    }

    @Test
    fun `test that email id is used when both first name and last name are blank`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(contactsRepository.getCurrentUserLastName(any())).thenReturn("")
        whenever(accountRepository.getAccountEmail()).thenReturn("abc@mega.co.nz")

        assertThat(
            underTest(true)
        ).isEqualTo("abc")
    }

    @Test
    fun `test that full name is null when first name, last name and email are blank`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(contactsRepository.getCurrentUserLastName(any())).thenReturn("")
        whenever(accountRepository.getAccountEmail()).thenReturn("")

        assertThat(
            underTest(true)
        ).isEqualTo(null)
    }

    @Test
    internal fun `test that last name is returned if first name throw an exception`() = runTest {
        val expected = "LastName"
        contactsRepository.stub {
            onBlocking { getCurrentUserFirstName(any()) }.thenAnswer {
                throw MegaException(
                    1,
                    "First Name threw exception"
                )
            }
            onBlocking { getCurrentUserLastName(any()) }.thenReturn(expected)
        }

        assertThat(underTest(true)).isEqualTo(expected)
    }

    @Test
    internal fun `test that first name is returned if last name throws an exception`() = runTest {
        val expected = "FirstName"
        contactsRepository.stub {
            onBlocking { getCurrentUserFirstName(any()) }.thenReturn(expected)
            onBlocking { getCurrentUserLastName(any()) }.thenAnswer {
                throw MegaException(
                    1,
                    "Last Name threw exception"
                )
            }
        }

        assertThat(underTest(true)).isEqualTo(expected)
    }

}