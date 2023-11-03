package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorAllContactParticipantsInChatUseCaseTest {
    private val repository: ContactsRepository = mock()
    private val areAllContactParticipantsInChatUseCase: AreAllContactParticipantsInChatUseCase =
        mock()
    private lateinit var underTest: MonitorAllContactParticipantsInChatUseCase

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    private fun initTestClass() {
        underTest = MonitorAllContactParticipantsInChatUseCase(
            repository,
            areAllContactParticipantsInChatUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository, areAllContactParticipantsInChatUseCase)
    }

    @ParameterizedTest(name = "returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that returns correctly when areAllContactParticipantsInChatUseCase`(
        result: Boolean,
    ) = runTest {
        whenever(repository.monitorNewContacts()).thenReturn(flow { emit(listOf(1L, 2L, 3L)) })
        whenever(repository.monitorContactRemoved()).thenReturn(emptyFlow())
        whenever(areAllContactParticipantsInChatUseCase(any())).thenReturn(result)
        initTestClass()
        underTest(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)).test {
            Truth.assertThat(awaitItem()).isEqualTo(result)
            Truth.assertThat(awaitItem()).isEqualTo(false)
            awaitComplete()
        }
    }

    @Test
    fun `test that returns correctly when monitorNewContacts emits handle not in the peer list`() =
        runTest {
            whenever(repository.monitorNewContacts()).thenReturn(flow { emit(listOf(9L)) })
            whenever(repository.monitorContactRemoved()).thenReturn(emptyFlow())
            whenever(areAllContactParticipantsInChatUseCase(any())).thenReturn(true)
            initTestClass()
            underTest(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)).test {
                Truth.assertThat(awaitItem()).isEqualTo(true) // onStart triggers
                Truth.assertThat(awaitItem()).isEqualTo(false)
                awaitComplete()
            }
        }

    @Test
    fun `test that returns correctly when monitorContactRemoved emits handle not in the peer list`() =
        runTest {
            whenever(repository.monitorNewContacts()).thenReturn(emptyFlow())
            whenever(repository.monitorContactRemoved()).thenReturn(flow { emit(listOf(9L)) })
            whenever(areAllContactParticipantsInChatUseCase(any()))
                .thenReturn(true)
                .thenReturn(false)
            initTestClass()
            underTest(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)).test {
                Truth.assertThat(awaitItem()).isEqualTo(true) // onStart triggers
                Truth.assertThat(awaitItem()).isEqualTo(false)
                awaitComplete()
            }
        }
}