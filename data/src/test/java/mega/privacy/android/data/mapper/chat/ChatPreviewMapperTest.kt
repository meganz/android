package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatPreview
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [ChatPreviewMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatPreviewMapperTest {
    private lateinit var underTest: ChatPreviewMapper

    private val chatPreviewMapper = mock<ChatRequestMapper>()

    @BeforeAll
    fun setUp() {
        underTest = ChatPreviewMapper(chatPreviewMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatPreviewMapper)
    }

    @Test
    fun `test that the mapped chat preview exists`() = runTest {
        val megaChatRequest = mock<MegaChatRequest>()
        val chatRequest = mock<ChatRequest>()
        whenever(chatPreviewMapper(megaChatRequest)).thenReturn(chatRequest)

        val expected = ChatPreview(request = chatRequest, exist = true)
        val actual = underTest(request = megaChatRequest, errorCode = MegaChatError.ERROR_EXIST)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped chat preview does not exist`() = runTest {
        val megaChatRequest = mock<MegaChatRequest>()
        val chatRequest = mock<ChatRequest>()
        whenever(chatPreviewMapper(megaChatRequest)).thenReturn(chatRequest)

        val expected = ChatPreview(request = chatRequest, exist = false)
        val actual = underTest(request = megaChatRequest, errorCode = MegaChatError.ERROR_NOENT)

        assertThat(actual).isEqualTo(expected)
    }
}