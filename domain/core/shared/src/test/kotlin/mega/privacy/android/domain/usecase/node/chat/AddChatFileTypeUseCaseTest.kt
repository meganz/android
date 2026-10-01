package mega.privacy.android.domain.usecase.node.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.TypedImageNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddChatFileTypeUseCaseTest {
    private lateinit var underTest: AddChatFileTypeUseCase

    private val addImageTypeUseCase = mock<AddImageTypeUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = AddChatFileTypeUseCase(addImageTypeUseCase)
    }

    @BeforeEach
    fun resetMocks() = reset(addImageTypeUseCase)

    @Test
    fun `test that ChatImageFile is returned when chat file is an image node`() = runTest {
        val imageNode = mock<ImageNode>()
        val typedImageNode = mock<TypedImageNode>()
        whenever(addImageTypeUseCase(imageNode)).thenReturn(typedImageNode)
        val expected = ChatImageFile(typedImageNode, CHAT_ID, MESSAGE_ID, MESSAGE_INDEX)
        val actual = underTest(imageNode, CHAT_ID, MESSAGE_ID, MESSAGE_INDEX)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that ChatDefaultFile is returned when chat file is not a know typed file node`() =
        runTest {
            val node = mock<FileNode>()
            val expected =
                ChatDefaultFile(DefaultTypedFileNode(node), CHAT_ID, MESSAGE_ID, MESSAGE_INDEX)
            val actual = underTest(node, CHAT_ID, MESSAGE_ID, MESSAGE_INDEX)
            assertThat(actual).isEqualTo(expected)
        }
}

private const val CHAT_ID = 11L
private const val MESSAGE_ID = 11L
private const val MESSAGE_INDEX = 0