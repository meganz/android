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
class GetUserFirstNameUseCaseTest {

    private lateinit var underTest: GetUserFirstNameUseCase

    private val contactsRepository: ContactsRepository = mock()
    private val accountRepository: AccountRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = GetUserFirstNameUseCase(
            contactsRepository = contactsRepository,
            accountRepository = accountRepository,
        )
    }

    @Test
    fun `test that first name is returned when it is not blank`() = runTest {
        val expectedFirstName = "FirstName"
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn(expectedFirstName)

        assertThat(underTest(true)).isEqualTo(expectedFirstName)
    }

    @Test
    fun `test that first name is trimmed when it has whitespace`() = runTest {
        val expectedFirstName = "FirstName"
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("  $expectedFirstName  ")

        assertThat(underTest(true)).isEqualTo(expectedFirstName)
    }

    @Test
    fun `test that email prefix is returned when first name is empty`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(accountRepository.getAccountEmail()).thenReturn("john.doe@mega.co.nz")

        assertThat(underTest(true)).isEqualTo("john")
    }

    @Test
    fun `test that email prefix is returned when first name is blank with spaces`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("   ")
        whenever(accountRepository.getAccountEmail()).thenReturn("jane_smith@example.com")

        assertThat(underTest(true)).isEqualTo("jane")
    }

    @Test
    fun `test that email prefix handles dots in email`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(accountRepository.getAccountEmail()).thenReturn("first.last@domain.com")

        assertThat(underTest(true)).isEqualTo("first")
    }

    @Test
    fun `test that email prefix handles underscores in email`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(accountRepository.getAccountEmail()).thenReturn("test_user@domain.com")

        assertThat(underTest(true)).isEqualTo("test")
    }

    @Test
    fun `test that null is returned when first name is blank and email is null`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(accountRepository.getAccountEmail()).thenReturn(null)

        assertThat(underTest(true)).isNull()
    }

    @Test
    fun `test that null is returned when first name is blank and email is empty`() = runTest {
        whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
        whenever(accountRepository.getAccountEmail()).thenReturn("")

        assertThat(underTest(true)).isNull()
    }

    @Test
    fun `test that null is returned when first name is blank and email prefix is empty`() =
        runTest {
            whenever(contactsRepository.getCurrentUserFirstName(any())).thenReturn("")
            whenever(accountRepository.getAccountEmail()).thenReturn("@domain.com")

            assertThat(underTest(true)).isNull()
        }

    @Test
    fun `test that email prefix is returned when first name throws exception`() = runTest {
        contactsRepository.stub {
            onBlocking { getCurrentUserFirstName(any()) }.thenAnswer {
                throw MegaException(1, "First Name threw exception")
            }
        }
        whenever(accountRepository.getAccountEmail()).thenReturn("fallback@mega.co.nz")

        assertThat(underTest(true)).isEqualTo("fallback")
    }

    @Test
    fun `test that null is returned when first name throws exception and email is null`() =
        runTest {
            contactsRepository.stub {
                onBlocking { getCurrentUserFirstName(any()) }.thenAnswer {
                    throw MegaException(1, "First Name threw exception")
                }
            }
            whenever(accountRepository.getAccountEmail()).thenReturn(null)

            assertThat(underTest(true)).isNull()
        }

    @Test
    fun `test that forceRefresh parameter is passed correctly when true`() = runTest {
        val expectedFirstName = "FirstName"
        whenever(contactsRepository.getCurrentUserFirstName(forceRefresh = true)).thenReturn(
            expectedFirstName
        )

        assertThat(underTest(true)).isEqualTo(expectedFirstName)
    }

    @Test
    fun `test that forceRefresh parameter is passed correctly when false`() = runTest {
        val expectedFirstName = "FirstName"
        whenever(contactsRepository.getCurrentUserFirstName(forceRefresh = false)).thenReturn(
            expectedFirstName
        )

        assertThat(underTest(false)).isEqualTo(expectedFirstName)
    }
}
