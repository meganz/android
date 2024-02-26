package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUserDataUseCaseTest {

    @InjectMocks
    private lateinit var underTest: GetUserDataUseCase

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var broadcastUpdateUserDataUseCase: BroadcastUpdateUserDataUseCase

    private lateinit var closeable: AutoCloseable

    @BeforeEach
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `test that should get the user's data from the account repository and broadcast update user data when execute the use case`() =
        runTest {
            // When
            underTest()

            // Then
            verify(accountRepository).getUserData()
            verify(broadcastUpdateUserDataUseCase).invoke()
        }

    @Test
    fun `test that update user data shouldn't be broadcast when failed to get the user data from repository`() =
        runTest {
            // Given
            whenever(accountRepository.getUserData()).thenThrow(RuntimeException())

            try {
                // When
                underTest()
            } catch (e: RuntimeException) {
                // Then
                verify(accountRepository).getUserData()
                verify(broadcastUpdateUserDataUseCase, never()).invoke()
            }
        }

    @AfterEach
    fun resetMocks() {
        closeable.close()
    }
}
