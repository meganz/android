package mega.privacy.android.app.presentation.chat

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.mapper.ChatRequestMessageMapper
import mega.privacy.android.domain.entity.node.ChatRequestResult
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class ChatRequestMessageMapperTest {
    private val mContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val chatRequestMessageMapper = ChatRequestMessageMapper(mContext)

    @Test
    fun `test that getResultText returns null when there is more than 1 successful and failed share`() {
        val mockShareCount = Random.nextInt(5, 10)
        val mockErrorCount = mockShareCount - 3
        val underTest =
            chatRequestMessageMapper(
                ChatRequestResult.ChatRequestAttachNode(
                    count = mockShareCount,
                    errorCount = mockErrorCount,
                )
            )
        val expected = null
        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that the correct message is returned when there is only 1 failed share request`() {
        val underTest =
            chatRequestMessageMapper(
                ChatRequestResult.ChatRequestAttachNode(
                    count = 1,
                    errorCount = 1,
                )
            )
        val expected =
            mContext.getString(
                R.string.files_send_to_chat_error,
            )

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that the correct message is returned when there is only 1 successful share request`() {
        val underTest =
            chatRequestMessageMapper(
                ChatRequestResult.ChatRequestAttachNode(
                    count = 1,
                    errorCount = 0,
                )
            )
        val expected =
            mContext.resources.getQuantityString(
                R.plurals.files_send_to_chat_success,
                1
            )
        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that the correct message is returned when there are multiple successful share request`() {
        val underTest =
            chatRequestMessageMapper(
                ChatRequestResult.ChatRequestAttachNode(
                    count = 2,
                    errorCount = 0,
                )
            )
        val expected =
            mContext.resources.getQuantityString(
                R.plurals.files_send_to_chat_success,
                2
            )
        assertThat(underTest).isEqualTo(expected)
    }
}