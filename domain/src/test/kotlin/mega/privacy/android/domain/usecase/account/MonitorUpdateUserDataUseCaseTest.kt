package mega.privacy.android.domain.usecase.account

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorUpdateUserDataUseCaseTest {

    @Mock
    private lateinit var accountRepository: AccountRepository
    private lateinit var closeable: AutoCloseable

    private lateinit var underTest: MonitorUpdateUserDataUseCase

    @BeforeEach
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        underTest = MonitorUpdateUserDataUseCase(accountRepository)
    }

    @Test
    fun `test that the repository is monitoring the update user data event when execute the use case`() {
        // When
        underTest()

        // Then
        verify(accountRepository).monitorUpdateUserData()
    }

    @Test
    fun `test that use case receives events emitted by the repository`() = runTest {
        // Given
        val updateUserDataEvent = MutableSharedFlow<Unit>()
        whenever(accountRepository.monitorUpdateUserData()).thenReturn(updateUserDataEvent)

        underTest().test {
            // When
            updateUserDataEvent.emit(Unit)
            updateUserDataEvent.emit(Unit)

            // Then
            assertThat(awaitItem()).isEqualTo(Unit)
            assertThat(awaitItem()).isEqualTo(Unit)
        }
    }

    @AfterEach
    fun resetMocks() {
        closeable.close()
    }
}
