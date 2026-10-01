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
internal class SetPresenceAutoAwayUseCaseTest {
    private val chatRepository: ChatRepository = mock()

    private lateinit var underTest: SetPresenceAutoAwayUseCase

    @BeforeAll
    fun setup() {
        underTest = SetPresenceAutoAwayUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = "enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke calls setPresenceAutoAway on the repository`(enabled: Boolean) =
        runTest {
            val timeoutSeconds = 300

            underTest(enabled, timeoutSeconds)

            verify(chatRepository).setPresenceAutoAway(enabled, timeoutSeconds)
        }
}
