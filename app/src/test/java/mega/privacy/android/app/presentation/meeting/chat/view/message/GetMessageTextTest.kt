package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth
import mega.privacy.android.core.formatter.stripChatFormatTags
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.toFormattedText
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMessageTextTest {

    @ParameterizedTest(name = " if input text is {0} and links is {1}: {2} ")
    @ArgumentsSource(FormatTextArgumentsProvider::class)
    fun `test that text is shown correctly`(
        inputText: String,
        links: List<String>,
        expectedText: String,
    ) {
        Truth.assertThat(inputText.toFormattedText(links).toString()).isEqualTo(expectedText)
    }

    /**
     * The notification path renders the same message as plain text, so both must agree.
     *
     * They only part ways when nested tags are unbalanced ([UNBALANCED_NESTED_TAGS]):
     * [toFormattedText] abandons the whole match and shows every tag, while
     * [stripChatFormatTags] pairs each tag on its own and removes the ones that do match.
     * Both are acceptable renderings of malformed input.
     */
    @ParameterizedTest(name = " if input text is {0} and links is {1}: {2} ")
    @ArgumentsSource(FormatTextArgumentsProvider::class)
    fun `test that the plain text used for notifications matches the formatted text`(
        inputText: String,
        links: List<String>,
        expectedText: String,
    ) {
        Assumptions.assumeTrue(inputText !in UNBALANCED_NESTED_TAGS)

        Truth.assertThat(inputText.stripChatFormatTags()).isEqualTo(expectedText)
    }

    @ParameterizedTest(name = " if input text is {0} and links is {1}: {2} ")
    @ArgumentsSource(LinkAndFormatArgumentsProvider::class)
    fun `test that toFormattedText shows each part once in order when the message mixes links and formats`(
        inputText: String,
        links: List<String>,
        expectedText: String,
    ) {
        Truth.assertThat(inputText.toFormattedText(links).toString()).isEqualTo(expectedText)
    }

    @ParameterizedTest(name = " if input text is {0} and links is {1}: {2} ")
    @ArgumentsSource(LinkAndFormatArgumentsProvider::class)
    fun `test that stripChatFormatTags matches toFormattedText when the message mixes links and formats`(
        inputText: String,
        links: List<String>,
        expectedText: String,
    ) {
        Truth.assertThat(inputText.stripChatFormatTags()).isEqualTo(expectedText)
    }

    @ParameterizedTest(name = " if input text is {0} and links is {1}: {2} ")
    @ArgumentsSource(LinkAndFormatArgumentsProvider::class)
    fun `test that toFormattedText adds a link annotation for each link when the message mixes links and formats`(
        inputText: String,
        links: List<String>,
        expectedText: String,
    ) {
        val formattedText = inputText.toFormattedText(links)
        val linkAnnotations = formattedText
            .getStringAnnotations(URL_TAG, 0, formattedText.length)
            .map { formattedText.substring(it.start, it.end) to it.item }

        Truth.assertThat(linkAnnotations).containsExactlyElementsIn(links.map { it to it }).inOrder()
    }

    @Test
    fun `test that toFormattedText keeps the bold style and the link when the link fills only a part of the bold sentence`() {
        val formattedText = "*Go to www.mega.io now* _italic_".toFormattedText(listOf("www.mega.io"))

        Truth.assertThat(formattedText.toString()).isEqualTo("Go to www.mega.io now italic")
        (0 until 21).forEach { index ->
            Truth.assertThat(formattedText.stylesAt(index).map { it.fontWeight })
                .contains(FontWeight.Bold)
        }
        (22 until 28).forEach { index ->
            Truth.assertThat(formattedText.stylesAt(index).map { it.fontStyle })
                .contains(FontStyle.Italic)
        }
        Truth.assertThat(formattedText.linkAnnotations())
            .containsExactly(IntRange(6, 16) to "www.mega.io")
    }

    @Test
    fun `test that toFormattedText keeps all the wrapping styles and the link when several tags wrap a link`() {
        val formattedText = "_`*www.mega.io*`_ *bold*".toFormattedText(listOf("www.mega.io"))

        Truth.assertThat(formattedText.toString()).isEqualTo("www.mega.io bold")
        (0 until 11).forEach { index ->
            val styles = formattedText.stylesAt(index)
            Truth.assertThat(styles.map { it.fontWeight }).contains(FontWeight.Bold)
            Truth.assertThat(styles.map { it.fontStyle }).contains(FontStyle.Italic)
            Truth.assertThat(styles.any { it.fontFamily != null }).isTrue()
        }
        (12 until 16).forEach { index ->
            Truth.assertThat(formattedText.stylesAt(index).map { it.fontWeight })
                .contains(FontWeight.Bold)
        }
        Truth.assertThat(formattedText.linkAnnotations())
            .containsExactly(IntRange(0, 10) to "www.mega.io")
    }

    @ParameterizedTest(name = " if link is {0} ")
    @ValueSource(strings = ["", "***", "*_~`"])
    fun `test that toFormattedText keeps the formats and adds no link when the link has no text other than tags`(
        link: String,
    ) {
        val formattedText = EMPTY_LINK_MESSAGE.toFormattedText(listOf(link))

        Truth.assertThat(formattedText.toString()).isEqualTo(EMPTY_LINK_EXPECTED_TEXT)
        Truth.assertThat(formattedText.linkAnnotations()).isEmpty()
        (0 until 4).forEach { index ->
            Truth.assertThat(formattedText.stylesAt(index).map { it.fontWeight })
                .contains(FontWeight.Bold)
        }
    }

    @ParameterizedTest(name = " if link is {0} ")
    @ValueSource(strings = ["", "***", "*_~`"])
    fun `test that stripChatFormatTags matches toFormattedText when the link has no text other than tags`(
        link: String,
    ) {
        Truth.assertThat(EMPTY_LINK_MESSAGE.stripChatFormatTags())
            .isEqualTo(EMPTY_LINK_MESSAGE.toFormattedText(listOf(link)).toString())
    }

    private fun AnnotatedString.stylesAt(index: Int) =
        spanStyles.filter { index in it.start until it.end }.map { it.item }

    private fun AnnotatedString.linkAnnotations() =
        getStringAnnotations(URL_TAG, 0, length).map { IntRange(it.start, it.end - 1) to it.item }

    @Test
    fun `test that toFormattedText shows the tags as typed when two formats start at the same position without other formats`() {
        Truth.assertThat("_~text_~".toFormattedText().toString()).isEqualTo("_~text_~")
        Truth.assertThat("_*bold* text_".toFormattedText().toString()).isEqualTo("_*bold* text_")
    }

    internal class LinkAndFormatArgumentsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext) = Stream.of(
            Arguments.of(
                "www.mega.io *bold*",
                listOf("www.mega.io"),
                "www.mega.io bold"
            ),
            Arguments.of(
                "www.mega.io *bold* text",
                listOf("www.mega.io"),
                "www.mega.io bold text"
            ),
            Arguments.of(
                "www.mega.io _italic_ and ~strike~",
                listOf("www.mega.io"),
                "www.mega.io italic and strike"
            ),
            Arguments.of(
                "*bold* www.mega.io",
                listOf("www.mega.io"),
                "bold www.mega.io"
            ),
            Arguments.of(
                "*bold* www.mega.io _italic_",
                listOf("www.mega.io"),
                "bold www.mega.io italic"
            ),
            Arguments.of(
                "*www.mega.io*",
                listOf("www.mega.io"),
                "www.mega.io"
            ),
            Arguments.of(
                "*www.mega.io* and _italic_ and ~strike~",
                listOf("www.mega.io"),
                "www.mega.io and italic and strike"
            ),
            Arguments.of(
                "_italic_ and *www.mega.io* and ~strike~",
                listOf("www.mega.io"),
                "italic and www.mega.io and strike"
            ),
            Arguments.of(
                "*www.mega.io* www.example.com",
                listOf("www.mega.io", "www.example.com"),
                "www.mega.io www.example.com"
            ),
            Arguments.of(
                "www.mega.io *bold _italic_ text*",
                listOf("www.mega.io"),
                "www.mega.io bold italic text"
            ),
            Arguments.of(
                "www.mega.io *a _b_ c* *d _e_ f*",
                listOf("www.mega.io"),
                "www.mega.io a b c d e f"
            ),
            Arguments.of(
                "www.example.com *www.mega.io* _italic_",
                listOf("www.example.com", "www.mega.io"),
                "www.example.com www.mega.io italic"
            ),
            Arguments.of(
                "_`*www.mega.io*`_ *bold*",
                listOf("www.mega.io"),
                "www.mega.io bold"
            ),
            Arguments.of(
                "*Go to www.mega.io now* _italic_",
                listOf("www.mega.io"),
                "Go to www.mega.io now italic"
            ),
            Arguments.of(
                "*www.mega.io is the site* text",
                listOf("www.mega.io"),
                "www.mega.io is the site text"
            ),
            Arguments.of(
                "*Go to www.mega.io now* text",
                listOf("www.mega.io"),
                "Go to www.mega.io now text"
            ),
            Arguments.of(
                "*Go to www.mega.io* text",
                listOf("www.mega.io"),
                "Go to www.mega.io text"
            ),
            Arguments.of(
                "_`*Go to www.mega.io now*`_ *bold*",
                listOf("www.mega.io"),
                "Go to www.mega.io now bold"
            ),
            Arguments.of(
                "```Go to www.mega.io now``` text",
                listOf("www.mega.io"),
                "Go to www.mega.io now text"
            ),
            Arguments.of(
                "*Go _www.mega.io_ now* text",
                listOf("www.mega.io"),
                "Go www.mega.io now text"
            ),
            Arguments.of(
                "*www.mega.io* _www.example.com_ *a _b_ c*",
                listOf("www.mega.io", "www.example.com"),
                "www.mega.io www.example.com a b c"
            ),
            Arguments.of(
                "*a www.mega.io b www.example.com c* _italic_",
                listOf("www.mega.io", "www.example.com"),
                "a www.mega.io b www.example.com c italic"
            ),
        )
    }

    internal class FormatTextArgumentsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext) = Stream.of(
            Arguments.of(
                "Text without format",
                emptyList<String>(),
                "Text without format"
            ),
            Arguments.of(
                "*Text with bold format*",
                emptyList<String>(),
                "Text with bold format"
            ),
            Arguments.of(
                "* Text with bold format*",
                emptyList<String>(),
                " Text with bold format"
            ),
            Arguments.of(
                "* Text with bold format *",
                emptyList<String>(),
                " Text with bold format "
            ),
            Arguments.of(
                "*Text with bold format *",
                emptyList<String>(),
                "Text with bold format "
            ),
            Arguments.of(
                "*\nText with bold format*",
                emptyList<String>(),
                "*\nText with bold format*",
            ),
            Arguments.of(
                "*\nText with bold format\n*",
                emptyList<String>(),
                "*\nText with bold format\n*",
            ),
            Arguments.of(
                "*Text with bold format\n*",
                emptyList<String>(),
                "*Text with bold format\n*",
            ),
            Arguments.of(
                "*Text with bold format* new bold*",
                emptyList<String>(),
                "Text with bold format new bold*"
            ),
            Arguments.of(
                "*Text with bold format* no format *new bold*",
                emptyList<String>(),
                "Text with bold format no format new bold",
            ),
            Arguments.of(
                "*Text with bold format* no format _new italic_",
                emptyList<String>(),
                "Text with bold format no format new italic",
            ),
            Arguments.of(
                "_Text with italic format_",
                emptyList<String>(),
                "Text with italic format"
            ),
            Arguments.of(
                "_ Text with italic format_",
                emptyList<String>(),
                " Text with italic format"
            ),
            Arguments.of(
                "_ Text with italic format _",
                emptyList<String>(),
                " Text with italic format "
            ),
            Arguments.of(
                "_Text with italic format _",
                emptyList<String>(),
                "Text with italic format "
            ),
            Arguments.of(
                "_\nText with italic format_",
                emptyList<String>(),
                "_\nText with italic format_"
            ),
            Arguments.of(
                "_\nText with italic format\n_",
                emptyList<String>(),
                "_\nText with italic format\n_",
            ),
            Arguments.of(
                "_Text with italic format\n_",
                emptyList<String>(),
                "_Text with italic format\n_"
            ),
            Arguments.of(
                "_Text with italic format_ new italic_",
                emptyList<String>(),
                "Text with italic format new italic_"
            ),
            Arguments.of(
                "_Text with italic format_ no format _new italic_",
                emptyList<String>(),
                "Text with italic format no format new italic"
            ),
            Arguments.of(
                "_Text with italic format_ no format ~new strikethrough~",
                emptyList<String>(),
                "Text with italic format no format new strikethrough"
            ),
            Arguments.of(
                "~Text with strikethrough format~",
                emptyList<String>(),
                "Text with strikethrough format"
            ),
            Arguments.of(
                "~ Text with strikethrough format~",
                emptyList<String>(),
                " Text with strikethrough format"
            ),
            Arguments.of(
                "~ Text with strikethrough format ~",
                emptyList<String>(),
                " Text with strikethrough format "
            ),
            Arguments.of(
                "~Text with strikethrough format ~",
                emptyList<String>(),
                "Text with strikethrough format "
            ),
            Arguments.of(
                "~\nText with italic format~",
                emptyList<String>(),
                "~\nText with italic format~"
            ),
            Arguments.of(
                "~\nText with italic format\n~",
                emptyList<String>(),
                "~\nText with italic format\n~",
            ),
            Arguments.of(
                "~Text with italic format\n~",
                emptyList<String>(),
                "~Text with italic format\n~"
            ),
            Arguments.of(
                "~Text with strikethrough format~ new strikethrough~",
                emptyList<String>(),
                "Text with strikethrough format new strikethrough~"
            ),
            Arguments.of(
                "~Text with strikethrough format~ no format ~new strikethrough~",
                emptyList<String>(),
                "Text with strikethrough format no format new strikethrough"
            ),
            Arguments.of(
                "~Text with strikethrough format~ no format `new quote`",
                emptyList<String>(),
                "Text with strikethrough format no format new quote"
            ),
            Arguments.of(
                "~Text with strikethrough format~",
                emptyList<String>(),
                "Text with strikethrough format"
            ),
            Arguments.of(
                "`Text with quote format`",
                emptyList<String>(),
                "Text with quote format"
            ),
            Arguments.of(
                "` Text with quote format`",
                emptyList<String>(),
                " Text with quote format"
            ),
            Arguments.of(
                "` Text with quote format `",
                emptyList<String>(),
                " Text with quote format "
            ),
            Arguments.of(
                "` Text with quote format`",
                emptyList<String>(),
                " Text with quote format"
            ),
            Arguments.of(
                "`\nText with italic format`",
                emptyList<String>(),
                "`\nText with italic format`"
            ),
            Arguments.of(
                "`\nText with italic format\n`",
                emptyList<String>(),
                "`\nText with italic format\n`",
            ),
            Arguments.of(
                "`Text with italic format\n`",
                emptyList<String>(),
                "`Text with italic format\n`"
            ),
            Arguments.of(
                "`Text with quote format` new quote`",
                emptyList<String>(),
                "Text with quote format new quote`"
            ),
            Arguments.of(
                "`Text with quote format` no format `new quote`",
                emptyList<String>(),
                "Text with quote format no format new quote"
            ),
            Arguments.of(
                "`Text with quote format` no format ```new multi-quote```",
                emptyList<String>(),
                "Text with quote format no format new multi-quote"
            ),
            Arguments.of(
                "```Text with multi-quote\nformat```",
                emptyList<String>(),
                "Text with multi-quote\nformat"
            ),
            Arguments.of(
                "``` Text with multi-quote\nformat```",
                emptyList<String>(),
                " Text with multi-quote\nformat"
            ),
            Arguments.of(
                "```Text with multi-quote\nformat ```",
                emptyList<String>(),
                "Text with multi-quote\nformat "
            ),
            Arguments.of(
                "```Text with multi-quote\nformat``` new multi-quote```",
                emptyList<String>(),
                "Text with multi-quote\nformat new multi-quote```"
            ),
            Arguments.of(
                "```Text with multi-quote\nformat``` no format ```new multi-quote```",
                emptyList<String>(),
                "Text with multi-quote\nformat no format new multi-quote"
            ),
            Arguments.of(
                "```Text with multi-quote\nformat``` no format *new bold*",
                emptyList<String>(),
                "Text with multi-quote\nformat no format new bold"
            ),
            Arguments.of(
                "```Text with *bold format* multi-quote\nformat``` no format",
                emptyList<String>(),
                "Text with *bold format* multi-quote\nformat no format",
            ),
            Arguments.of(
                "```Text with *bold* _italic_ multi-quote\nformat``` no format",
                emptyList<String>(),
                "Text with *bold* _italic_ multi-quote\nformat no format",
            ),
            Arguments.of(
                "```Text with *_bolditalic_* multi-quote\nformat``` no format",
                emptyList<String>(),
                "Text with *_bolditalic_* multi-quote\nformat no format",
            ),
            Arguments.of(
                "```Text `with` ~*_bolditalic_*~ multi-quote\nformat``` no format",
                emptyList<String>(),
                "Text `with` ~*_bolditalic_*~ multi-quote\nformat no format",
            ),
            Arguments.of(
                "*Text with bold _italic_ format*",
                emptyList<String>(),
                "Text with bold italic format"
            ),
            Arguments.of(
                "*Text with bold `_italicquote_` format*",
                emptyList<String>(),
                "Text with bold italicquote format"
            ),
            Arguments.of(
                "*Text with bold _`italicquote`_ format*",
                emptyList<String>(),
                "Text with bold italicquote format"
            ),
            Arguments.of(
                "*Text with bold ~`_italicquotestrikethrough_`~ format*",
                emptyList<String>(),
                "Text with bold italicquotestrikethrough format"
            ),
            Arguments.of(
                "*Text with bold `~_italicquotestrikethrough_~` format*",
                emptyList<String>(),
                "Text with bold italicquotestrikethrough format"
            ),
            Arguments.of(
                "*Text with bold `_~italicquotestrikethrough~_` format*",
                emptyList<String>(),
                "Text with bold italicquotestrikethrough format"
            ),
            Arguments.of(
                "*Text with bold _italic_ `quote` format*",
                emptyList<String>(),
                "Text with bold italic quote format"
            ),
            Arguments.of(
                "*Text with bold _italic_ `quote`format*",
                emptyList<String>(),
                "Text with bold italic quoteformat"
            ),
            Arguments.of(
                "*Text with bold _~italicstrikethrough~_ `quote` format*",
                emptyList<String>(),
                "Text with bold italicstrikethrough quote format"
            ),
            Arguments.of(
                "*Text with bold _~italicstrikethrough_~ `quote` format*",
                emptyList<String>(),
                "Text with bold _~italicstrikethrough_~ quote format"
            ),
            Arguments.of(
                "*Text with bold _`~italicstrikethroughquote`_~ `quote` format*",
                emptyList<String>(),
                "Text with bold _`~italicstrikethroughquote`_~ quote format"
            ),
            Arguments.of(
                "*Text with *bold* `quote` format*",
                emptyList<String>(),
                "Text with bold quote format"
            ),
            Arguments.of(
                "*Text* www.mega.io text",
                listOf("www.mega.io"),
                "Text www.mega.io text"
            ),
            Arguments.of(
                "*www.mega.io*",
                listOf("www.mega.io"),
                "www.mega.io"
            ),
            Arguments.of(
                "`*www.mega.io*`",
                listOf("www.mega.io"),
                "www.mega.io"
            ),
            Arguments.of(
                "_`*www.mega.io*`_",
                listOf("www.mega.io"),
                "www.mega.io"
            ),
            Arguments.of(
                "```www.mega.io```",
                listOf("www.mega.io"),
                "www.mega.io"
            ),
            Arguments.of(
                "Hello *world*",
                listOf("www.notpresent.io"),
                "Hello world"
            ),
            Arguments.of(
                "*_*",
                emptyList<String>(),
                "_"
            ),
            Arguments.of(
                "_*_",
                emptyList<String>(),
                "*"
            ),
            Arguments.of(
                "*~*",
                emptyList<String>(),
                "~"
            ),
            Arguments.of(
                "~_~",
                emptyList<String>(),
                "_"
            ),
        )
    }

    private companion object {
        const val URL_TAG = "URL"
        const val EMPTY_LINK_MESSAGE = "*bold* _italic_ text"
        const val EMPTY_LINK_EXPECTED_TEXT = "bold italic text"

        val UNBALANCED_NESTED_TAGS = listOf(
            "*Text with bold _~italicstrikethrough_~ `quote` format*",
            "*Text with bold _`~italicstrikethroughquote`_~ `quote` format*",
        )
    }
}