package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetSFUIdUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetSFUIdUseCaseTest {

    private lateinit var underTest: SetSFUIdUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetSFUIdUseCase(
            chatRepository = chatRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that sfu id is set when invoked`() = runTest {
        val sfuId = 123456
        underTest(sfuId)

        verify(chatRepository).setSFUid(sfuId)
    }
}
