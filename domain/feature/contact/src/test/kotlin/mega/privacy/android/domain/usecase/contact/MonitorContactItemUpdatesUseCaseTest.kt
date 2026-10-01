package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorContactItemUpdatesUseCaseTest {
    private lateinit var underTest: MonitorContactItemUpdatesUseCase

    private val updatesFlow = MutableSharedFlow<UserUpdate>()
    private val contactsRepository = mock<ContactsRepository>()
    private val applyContactUpdatesUseCase = mock<ApplyContactUpdatesUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorContactItemUpdatesUseCase(
            contactsRepository = contactsRepository,
            applyContactUpdatesUseCase = applyContactUpdatesUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(contactsRepository, applyContactUpdatesUseCase)
        whenever(contactsRepository.monitorContactUpdates()).thenReturn(updatesFlow)
    }

    @Test
    fun `test that invoke emits the initial contact first`() = runTest {
        val initial = mock<ContactItem>()

        underTest(initial).test {
            assertThat(awaitItem()).isEqualTo(initial)
        }
    }

    @Test
    fun `test that invoke emits the updated contact when an update is received`() = runTest {
        val initial = mock<ContactItem>()
        val updated = mock<ContactItem>()
        val update = mock<UserUpdate>()
        whenever(applyContactUpdatesUseCase(initial, update)).thenReturn(updated)

        underTest(initial).test {
            assertThat(awaitItem()).isEqualTo(initial)
            updatesFlow.emit(update)
            assertThat(awaitItem()).isEqualTo(updated)
        }
    }

    @Test
    fun `test that invoke folds consecutive updates onto the latest contact`() = runTest {
        val initial = mock<ContactItem>()
        val first = mock<ContactItem>()
        val second = mock<ContactItem>()
        val firstUpdate = mock<UserUpdate>()
        val secondUpdate = mock<UserUpdate>()
        whenever(applyContactUpdatesUseCase(initial, firstUpdate)).thenReturn(first)
        whenever(applyContactUpdatesUseCase(first, secondUpdate)).thenReturn(second)

        underTest(initial).test {
            assertThat(awaitItem()).isEqualTo(initial)
            updatesFlow.emit(firstUpdate)
            assertThat(awaitItem()).isEqualTo(first)
            updatesFlow.emit(secondUpdate)
            assertThat(awaitItem()).isEqualTo(second)
        }
    }
}
