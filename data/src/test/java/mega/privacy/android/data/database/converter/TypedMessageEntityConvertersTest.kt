package mega.privacy.android.data.database.converter

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatMessageChange
import org.junit.jupiter.api.Test

class TypedMessageEntityConvertersTest {
    private val underTest = TypedMessageEntityConverters()

    @Test
    internal fun `test that convertFromLongList returns a string with the longs separated by commas`() {
        val list = listOf(1L, 2L, 3L)
        val expected = "1,2,3"

        val actual = underTest.convertFromLongList(list)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that convertToLongList returns a list of longs`() {
        val string = "1,2,3"
        val expected = listOf(1L, 2L, 3L)

        val actual = underTest.convertToLongList(string)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that empty strings return an empty list when calling convertToLongList`() {
        val string = ""

        val actual = underTest.convertToLongList(string)

        assertThat(actual).isEmpty()
    }

    @Test
    internal fun `test that convertFromStringList returns a string with the strings separated by commas`() {
        val list = listOf("1", "2", "3")
        val expected = "1,2,3"

        val actual = underTest.convertFromStringList(list)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that convertToStringList returns a list of strings`() {
        val string = "1,2,3"
        val expected = listOf("1", "2", "3")

        val actual = underTest.convertToStringList(string)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that empty strings return an empty list when calling convertToStringList`() {
        val string = ""

        val actual = underTest.convertToStringList(string)
        println(actual)

        assertThat(actual).isEmpty()
    }

    @Test
    internal fun `test that convertFromChatMessageChangeList returns a string with the chat message changes separated by commas`() {
        val list = listOf(ChatMessageChange.CONTENT, ChatMessageChange.ACCESS)
        val expected = "CONTENT,ACCESS"

        val actual = underTest.convertFromChatMessageChangeList(list)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that convertToChatMessageChangeList returns a list of chat message changes`() {
        val string = "CONTENT,ACCESS"
        val expected = listOf(ChatMessageChange.CONTENT, ChatMessageChange.ACCESS)

        val actual = underTest.convertToChatMessageChangeList(string)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that empty strings return an empty list when calling convertToChatMessageChangeList`() {
        val string = ""

        val actual = underTest.convertToChatMessageChangeList(string)

        assertThat(actual).isEmpty()
    }

}