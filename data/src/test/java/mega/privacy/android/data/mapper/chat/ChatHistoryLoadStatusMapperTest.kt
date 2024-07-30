package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [ChatHistoryLoadStatusMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatHistoryLoadStatusMapperTest {
    private lateinit var underTest: ChatHistoryLoadStatusMapper

    @BeforeAll
    fun setUp() {
        underTest = ChatHistoryLoadStatusMapper()
    }

    @TestFactory
    fun `test that the mapping is correct`() = listOf(
        MegaChatApi.SOURCE_INVALID_CHAT to ChatHistoryLoadStatus.INVALID_CHAT,
        MegaChatApi.SOURCE_ERROR to ChatHistoryLoadStatus.ERROR,
        MegaChatApi.SOURCE_NONE to ChatHistoryLoadStatus.NONE,
        MegaChatApi.SOURCE_LOCAL to ChatHistoryLoadStatus.LOCAL,
        MegaChatApi.SOURCE_REMOTE to ChatHistoryLoadStatus.REMOTE,
    ).map { (input, expected) ->
        dynamicTest("test that $input is mapped to $expected") {
            assertThat(underTest(input)).isEqualTo(expected)
        }
    }

    @Test
    fun `test that an unspecified value is mapped to an error chat history load status`() =
        runTest {
            // 100 is not specified in the Mapper
            assertThat(underTest(100)).isEqualTo(ChatHistoryLoadStatus.ERROR)
        }
}