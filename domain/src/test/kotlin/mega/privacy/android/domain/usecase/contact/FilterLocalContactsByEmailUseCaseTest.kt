package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilterLocalContactsByEmailUseCaseTest {

    private lateinit var underTest: FilterLocalContactsByEmailUseCase

    private val contactsRepository: ContactsRepository = mock()
    private val isAMegaContactByEmailUseCase: IsAMegaContactByEmailUseCase = mock()

    @BeforeEach
    fun setup() {
        underTest = FilterLocalContactsByEmailUseCase(
            contactsRepository = contactsRepository,
            isAMegaContactByEmailUseCase = isAMegaContactByEmailUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            contactsRepository,
            isAMegaContactByEmailUseCase
        )
    }

    @Test
    fun `test that a successfully filtered list of local contacts is returned`() = runTest {
        val addedEmail = "test@test.com"
        val unAddedEmail = "test2@test.com"
        val firstUserID = 1L
        val secondUserID = 2L
        val users = listOf(
            User(
                handle = firstUserID,
                email = addedEmail,
                visibility = UserVisibility.Visible,
                timestamp = System.currentTimeMillis(),
                userChanges = listOf()
            )
        )
        whenever(contactsRepository.getAvailableContacts()).thenReturn(users)
        whenever(isAMegaContactByEmailUseCase(any(), eq(addedEmail))).thenReturn(true)
        whenever(isAMegaContactByEmailUseCase(any(), eq(unAddedEmail))).thenReturn(false)

        val firstUserName = "name1"
        val secondUserName = "name2"
        val localContacts = listOf(
            LocalContact(
                id = firstUserID,
                name = firstUserName,
                emails = listOf(addedEmail, unAddedEmail)
            ),
            LocalContact(
                id = secondUserID,
                name = secondUserName,
                emails = listOf(unAddedEmail, unAddedEmail)
            )
        )
        val actual = underTest(localContacts)

        val expected = listOf(
            LocalContact(
                id = firstUserID,
                name = firstUserName,
                emails = listOf(unAddedEmail)
            ),
            LocalContact(
                id = secondUserID,
                name = secondUserName,
                emails = listOf(unAddedEmail, unAddedEmail)
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the initial list of emails is returned when no contacts are available`() =
        runTest {
            val email = "test@test.com"
            whenever(contactsRepository.getAvailableContacts()).thenReturn(listOf())

            val contactID = 1L
            val userName = "name1"
            val localContacts = listOf(
                LocalContact(
                    id = contactID,
                    name = userName,
                    emails = listOf(email)
                )
            )
            val actual = underTest(localContacts)

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    name = userName,
                    emails = listOf(email)
                )
            )
            assertThat(actual).isEqualTo(expected)
        }
}
