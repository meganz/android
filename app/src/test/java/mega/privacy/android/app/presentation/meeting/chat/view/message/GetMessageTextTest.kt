package mega.privacy.android.app.presentation.meeting.chat.view.message

import com.google.common.truth.Truth
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.toFormattedText
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
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

    internal class FormatTextArgumentsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
            return Stream.of(
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
            )
        }
    }
}