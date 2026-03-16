package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatFilesFolderIdUseCaseTest {

    private lateinit var underTest: GetChatFilesFolderIdUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeEach
    fun setup() {
        underTest = GetChatFilesFolderIdUseCase(chatRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that null is returned when repository returns null`() = runTest {
        whenever(chatRepository.getChatFilesFolderId()).thenReturn(null)

        val actual = underTest()

        assertThat(actual).isNull()
    }

    @Test
    fun `test that node id is returned when repository returns a valid node id`() = runTest {
        val nodeId = NodeId(123L)
        whenever(chatRepository.getChatFilesFolderId()).thenReturn(nodeId)

        val actual = underTest()

        assertThat(actual).isEqualTo(nodeId)
    }
}
