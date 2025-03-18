package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Contact
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
class GetContactFromCacheByHandleUseCaseTest {

    private lateinit var underTest: MonitorContactByHandleUseCase

    private val contactsRepository: ContactsRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = MonitorContactByHandleUseCase(
            contactsRepository = contactsRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(contactsRepository)
    }

    @Test
    fun `test that the correct archived chat list items is returned`() = runTest {
        val contactId = 123L
        val contact = Contact(
            userId = 456L,
            email = "test@test.com"
        )
        whenever(contactsRepository.monitorContactByHandle(contactId)) doReturn flow {
            emit(
                contact
            )
        }

        underTest(contactId).test {
            assertThat(awaitItem()).isEqualTo(contact)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
