package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorHasAnyContactUseCaseTest {

    private lateinit var underTest: MonitorHasAnyContactUseCase

    private val contactsRepository = mock<ContactsRepository>()

    @BeforeEach
    fun setup() {
        underTest = MonitorHasAnyContactUseCase(contactsRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(contactsRepository)
    }

    @Test
    fun `test that monitor has any contact updates returns correct flow`() = runTest {
        whenever(contactsRepository.hasAnyContact())
            .thenReturn(true)
            .thenReturn(true)
            .thenReturn(false)
            .thenReturn(true)
        whenever(contactsRepository.monitorContactRemoved())
            .thenReturn(flowOf(listOf(123L), listOf(321L)))
        whenever(contactsRepository.monitorNewContacts()).thenReturn(flowOf(listOf(456L)))

        underTest().test {
            Truth.assertThat(awaitItem()).isTrue()
            Truth.assertThat(awaitItem()).isTrue()
            Truth.assertThat(awaitItem()).isFalse()
            Truth.assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }
}