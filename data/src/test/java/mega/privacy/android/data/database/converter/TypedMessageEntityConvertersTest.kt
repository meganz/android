package mega.privacy.android.data.database.converter

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatMessageChange
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
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

    @Test
    internal fun `test that convertToMessageReactionList parses valid legacy gson entries`() {
        val string =
            """{"reaction":"👍","count":2,"userHandles":[123,456],"hasMe":true};""" +
                    """{"reaction":"❤","count":1,"userHandles":[789],"hasMe":false}"""
        val expected = listOf(
            Reaction(reaction = "👍", count = 2, userHandles = listOf(123L, 456L), hasMe = true),
            Reaction(reaction = "❤", count = 1, userHandles = listOf(789L), hasMe = false),
        )

        val actual = underTest.convertToMessageReactionList(string)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that convertToMessageReactionList drops entries when legacy json has obfuscated field names`() {
        val string = """{"a":"👍","b":1,"c":[123],"d":true}"""

        val actual = underTest.convertToMessageReactionList(string)

        assertThat(actual).isEmpty()
    }

    @Test
    internal fun `test that convertToMessageReactionList drops entries when legacy json has no reaction field`() {
        val string = """{"count":0,"hasMe":false}"""

        val actual = underTest.convertToMessageReactionList(string)

        assertThat(actual).isEmpty()
    }

    @Test
    internal fun `test that convertToMessageReactionList returns a hashable list when stored json is corrupt`() {
        val string = """{"count":0,"hasMe":false};{"a":"👍","b":1,"c":[123],"d":true}"""

        val actual = underTest.convertToMessageReactionList(string)

        assertThat(actual.hashCode()).isEqualTo(emptyList<Reaction>().hashCode())
    }

    @Test
    internal fun `test that convertToMessageReactionList keeps valid entries when legacy json mixes valid and corrupt entries`() {
        val string =
            """{"reaction":"👍","count":2,"userHandles":[123,456],"hasMe":true};""" +
                    """{"count":0,"hasMe":false}"""
        val expected = listOf(
            Reaction(reaction = "👍", count = 2, userHandles = listOf(123L, 456L), hasMe = true),
        )

        val actual = underTest.convertToMessageReactionList(string)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun `test that convertToMessageReactionList returns an empty list when string is malformed`() {
        val string = "not json at all"

        val actual = underTest.convertToMessageReactionList(string)

        assertThat(actual).isEmpty()
    }

    @Test
    internal fun `test that empty strings return an empty list when calling convertToMessageReactionList`() {
        val string = ""

        val actual = underTest.convertToMessageReactionList(string)

        assertThat(actual).isEmpty()
    }

    @Test
    internal fun `test that a reaction list round trips through the converters`() {
        val list = listOf(
            Reaction(reaction = "👍", count = 2, userHandles = listOf(123L, 456L), hasMe = true),
            Reaction(reaction = "a;b", count = 1, userHandles = listOf(789L), hasMe = false),
        )

        val actual = underTest.convertToMessageReactionList(
            underTest.convertFromMessageReactionList(list)
        )

        assertThat(actual).isEqualTo(list)
    }

    @Test
    internal fun `test that an empty reaction list round trips through the converters`() {
        val actual = underTest.convertToMessageReactionList(
            underTest.convertFromMessageReactionList(emptyList())
        )

        assertThat(actual).isEmpty()
    }
}