package mega.privacy.android.domain.usecase.thumbnailpreview

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatThumbnailUseCaseTest {
    private lateinit var underTest: GetChatThumbnailUseCase

    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val getPreviewUseCase = mock<GetPreviewUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetChatThumbnailUseCase(
            getChatFileUseCase = getChatFileUseCase,
            getPreviewUseCase = getPreviewUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getChatFileUseCase, getPreviewUseCase)
    }

    @Test
    fun `test that preview is returned if chat file is present`() = runTest {
        val chatFile = mock<ChatImageFile>()
        val previewFile = File("previewFile")
        whenever(getChatFileUseCase(123L, 456L)).thenReturn(chatFile)
        whenever(getPreviewUseCase(chatFile)).thenReturn(previewFile)
        assertThat(underTest(123L, 456L)).isSameInstanceAs(previewFile)
    }

    @Test
    fun `test that null is returned if chat file is not present`() = runTest {
        whenever(getChatFileUseCase(123L, 456L)).thenReturn(null)
        assertThat(underTest(123L, 456L)).isNull()
    }
}