package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetPresencePersistUseCaseTest {
    private val chatRepository: ChatRepository = mock()

    private lateinit var underTest: SetPresencePersistUseCase

    @BeforeAll
    fun setup() {
        underTest = SetPresencePersistUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = "enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke calls setPresencePersist on the repository`(enabled: Boolean) =
        runTest {
            underTest(enabled)

            verify(chatRepository).setPresencePersist(enabled)
        }
}
