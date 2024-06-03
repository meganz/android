package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsEmailInContactsUseCaseTest {

    private lateinit var underTest: IsEmailInContactsUseCase

    private val contactsRepository: ContactsRepository = mock()
    private val isAMegaContactByEmailUseCase: IsAMegaContactByEmailUseCase = mock()

    @BeforeEach
    fun setUp() {
        underTest = IsEmailInContactsUseCase(
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
    fun `test that True is returned when the email exists in the visible contacts`() = runTest {
        val email = "email@email.com"
        val user = newUser()
        whenever(contactsRepository.getAvailableContacts()) doReturn listOf(user)
        whenever(isAMegaContactByEmailUseCase(user, email)) doReturn true

        val actual = underTest(email)

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that False is returned when the email doesn't exist in the visible contacts`() =
        runTest {
            val email = "email@email.com"
            val user = newUser()
            whenever(contactsRepository.getAvailableContacts()) doReturn listOf(user)
            whenever(isAMegaContactByEmailUseCase(user, email)) doReturn false

            val actual = underTest(email)

            assertThat(actual).isFalse()
        }

    private fun newUser(
        withHandle: Long = -1L,
        withEmail: String = "",
        withVisibility: UserVisibility = UserVisibility.Unknown,
        withTimestamp: Long = 0L,
        withUserChanges: List<UserChanges> = emptyList(),
    ) = User(
        handle = withHandle,
        email = withEmail,
        visibility = withVisibility,
        timestamp = withTimestamp,
        userChanges = withUserChanges
    )
}
