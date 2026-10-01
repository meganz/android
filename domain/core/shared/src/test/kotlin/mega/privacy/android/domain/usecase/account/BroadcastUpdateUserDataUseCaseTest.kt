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
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BroadcastUpdateUserDataUseCaseTest {

    @InjectMocks
    private lateinit var underTest: BroadcastUpdateUserDataUseCase

    @Mock
    private lateinit var accountRepository: AccountRepository

    private lateinit var closeable: AutoCloseable

    @BeforeEach
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `test that the repository is broadcasting the update user data event when execute the use case`() =
        runTest {
            // When
            underTest()

            // Then
            verify(accountRepository).broadcastUpdateUserData()
        }

    @AfterEach
    fun resetMocks() {
        closeable.close()
    }
}
