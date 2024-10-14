package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorContactVisibilityUpdateUseCaseTest {

    private lateinit var underTest: MonitorContactVisibilityUpdateUseCase
    private val contactRepository: ContactsRepository = mock()
    private val accountRepository: AccountRepository = mock()


    @BeforeAll
    fun setup() {
        underTest = MonitorContactVisibilityUpdateUseCase(contactRepository, accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(contactRepository)
        reset(accountRepository)
    }

    @Test
    fun `test that visibility changes are emitted correctly`() = runTest {
        val userId1 = UserId(1L)

        val contactItem = mock<ContactItem> {
            on { handle } doReturn userId1.id
            on { visibility } doReturn UserVisibility.Visible
        }

        val userUpdate = mock<UserUpdate> {
            on { changes } doReturn mapOf(
                userId1 to listOf(UserChanges.Visibility(UserVisibility.Hidden))
            )
        }

        whenever(contactRepository.getVisibleContacts()).thenReturn(listOf(contactItem))
        whenever(accountRepository.monitorUserUpdates()).thenReturn(flowOf(userUpdate))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(userId1)
        }
    }

    @Test
    fun `test that multiple visibility changes are emitted properly`() = runTest {
        val userId1 = UserId(1L)

        val contactItem1 = mock<ContactItem> {
            on { handle } doReturn userId1.id
            on { visibility } doReturn UserVisibility.Visible
        }
        val contactItem2 = mock<ContactItem> {
            on { handle } doReturn userId1.id
            on { visibility } doReturn UserVisibility.Hidden
        }

        val userUpdate1 = mock<UserUpdate> {
            on { changes } doReturn mapOf(
                userId1 to listOf(UserChanges.Visibility(UserVisibility.Hidden))
            )
        }
        val userUpdate2 = mock<UserUpdate> {
            on { changes } doReturn mapOf(
                userId1 to listOf(UserChanges.Visibility(UserVisibility.Visible))
            )
        }

        whenever(contactRepository.getVisibleContacts())
            .thenReturn(listOf(contactItem1))
            .thenReturn(listOf(contactItem2))
        whenever(accountRepository.monitorUserUpdates()).thenReturn(
            flowOf(userUpdate1, userUpdate2)
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(userId1)
            assertThat(awaitItem()).isEqualTo(userId1)
        }
    }

    @Test
    fun `test that non-visibility user updates are not emitted`() = runTest {
        val userId1 = UserId(1L)

        val contactItem = mock<ContactItem> {
            on { handle } doReturn userId1.id
            on { visibility } doReturn UserVisibility.Visible
        }

        val userUpdate = mock<UserUpdate> {
            on { changes } doReturn mapOf(
                userId1 to listOf(UserChanges.Firstname)
            )
        }

        whenever(contactRepository.getVisibleContacts()).thenReturn(listOf(contactItem))
        whenever(accountRepository.monitorUserUpdates()).thenReturn(flowOf(userUpdate))

        underTest().test {
            expectNoEvents()
        }
    }
}
