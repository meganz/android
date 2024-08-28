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

    @ParameterizedTest(name = " as {1} if input text is {0}")
    @ArgumentsSource(FormatTextArgumentsProvider::class)
    fun `test that text is shown correctly`(
        inputText: String,
        expectedText: String,
    ) {
        Truth.assertThat(inputText.toFormattedText(emptyList()).toString()).isEqualTo(expectedText)
    }

    internal class FormatTextArgumentsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
            return Stream.of(
                Arguments.of("Text without format", "Text without format"),
                Arguments.of("*Text with bold format*", "Text with bold format"),
                Arguments.of("* Text with bold format*", "* Text with bold format*"),
                Arguments.of("*Text with bold format *", "*Text with bold format *"),
                Arguments.of(
                    "*Text with bold format* new bold*",
                    "Text with bold format new bold*"
                ),
                Arguments.of(
                    "*Text with bold format* no format *new bold*",
                    "Text with bold format no format new bold",
                ),
                Arguments.of(
                    "*Text with bold format* no format _new italic_",
                    "Text with bold format no format new italic",
                ),
                Arguments.of("_Text with italic format_", "Text with italic format"),
                Arguments.of("_ Text with italic format_", "_ Text with italic format_"),
                Arguments.of("_Text with italic format _", "_Text with italic format _"),
                Arguments.of(
                    "_Text with italic format_ new italic_",
                    "Text with italic format new italic_"
                ),
                Arguments.of(
                    "_Text with italic format_ no format _new italic_",
                    "Text with italic format no format new italic"
                ),
                Arguments.of(
                    "_Text with italic format_ no format ~new strikethrough~",
                    "Text with italic format no format new strikethrough"
                ),
                Arguments.of("~Text with strikethrough format~", "Text with strikethrough format"),
                Arguments.of(
                    "~ Text with strikethrough format~",
                    "~ Text with strikethrough format~"
                ),
                Arguments.of(
                    "~Text with strikethrough format ~",
                    "~Text with strikethrough format ~"
                ),
                Arguments.of(
                    "~Text with strikethrough format~ new strikethrough~",
                    "Text with strikethrough format new strikethrough~"
                ),
                Arguments.of(
                    "~Text with strikethrough format~ no format ~new strikethrough~",
                    "Text with strikethrough format no format new strikethrough"
                ),
                Arguments.of(
                    "~Text with strikethrough format~ no format `new quote`",
                    "Text with strikethrough format no format new quote"
                ),
                Arguments.of("~Text with strikethrough format~", "Text with strikethrough format"),
                Arguments.of("`Text with quote format`", "Text with quote format"),
                Arguments.of("` Text with quote format`", "` Text with quote format`"),
                Arguments.of("` Text with quote format`", "` Text with quote format`"),
                Arguments.of(
                    "`Text with quote format` new quote`",
                    "Text with quote format new quote`"
                ),
                Arguments.of(
                    "`Text with quote format` no format `new quote`",
                    "Text with quote format no format new quote"
                ),
                Arguments.of(
                    "`Text with quote format` no format ```new multi-quote```",
                    "Text with quote format no format new multi-quote"
                ),
                Arguments.of(
                    "```Text with multi-quote\nformat```",
                    "Text with multi-quote\nformat"
                ),
                Arguments.of(
                    "``` Text with multi-quote\nformat```",
                    "``` Text with multi-quote\nformat```"
                ),
                Arguments.of(
                    "```Text with multi-quote\nformat ```",
                    "```Text with multi-quote\nformat ```"
                ),
                Arguments.of(
                    "```Text with multi-quote\nformat``` new multi-quote```",
                    "Text with multi-quote\nformat new multi-quote```"
                ),
                Arguments.of(
                    "```Text with multi-quote\nformat``` no format ```new multi-quote```",
                    "Text with multi-quote\nformat no format new multi-quote"
                ),
                Arguments.of(
                    "```Text with multi-quote\nformat``` no format *new bold*",
                    "Text with multi-quote\nformat no format new bold"
                ),
                Arguments.of(
                    "```Text with *bold format* multi-quote\nformat``` no format",
                    "Text with *bold format* multi-quote\nformat no format",
                ),
                Arguments.of(
                    "```Text with *bold* _italic_ multi-quote\nformat``` no format",
                    "Text with *bold* _italic_ multi-quote\nformat no format",
                ),
                Arguments.of(
                    "```Text with *_bolditalic_* multi-quote\nformat``` no format",
                    "Text with *_bolditalic_* multi-quote\nformat no format",
                ),
                Arguments.of(
                    "```Text `with` ~*_bolditalic_*~ multi-quote\nformat``` no format",
                    "Text `with` ~*_bolditalic_*~ multi-quote\nformat no format",
                ),
                Arguments.of("*Text with bold _italic_ format*", "Text with bold italic format"),
                Arguments.of(
                    "*Text with bold `_italicquote_` format*",
                    "Text with bold italicquote format"
                ),
                Arguments.of(
                    "*Text with bold _`italicquote`_ format*",
                    "Text with bold italicquote format"
                ),
                Arguments.of(
                    "*Text with bold ~`_italicquotestrikethrough_`~ format*",
                    "Text with bold italicquotestrikethrough format"
                ),
                Arguments.of(
                    "*Text with bold `~_italicquotestrikethrough_~` format*",
                    "Text with bold italicquotestrikethrough format"
                ),
                Arguments.of(
                    "*Text with bold `_~italicquotestrikethrough~_` format*",
                    "Text with bold italicquotestrikethrough format"
                ),
                Arguments.of(
                    "*Text with bold _italic_ `quote` format*",
                    "Text with bold italic quote format"
                ),
                Arguments.of(
                    "*Text with bold _italic_ `quote`format*",
                    "Text with bold italic `quote`format"
                ),
                Arguments.of(
                    "*Text with bold _~italicstrikethrough~_ `quote` format*",
                    "Text with bold italicstrikethrough quote format"
                ),
                Arguments.of(
                    "*Text with bold _~italicstrikethrough_~ `quote` format*",
                    "Text with bold _~italicstrikethrough_~ quote format"
                ),
                Arguments.of(
                    "*Text with *bold* `quote` format*",
                    "Text with bold quote format"
                ),
                Arguments.of(
                    "*Text* www.mega.io text",
                    "Text www.mega.io text"
                ),
                Arguments.of(
                    "*www.mega.io*",
                    "www.mega.io"
                ),
                Arguments.of(
                    "`*www.mega.io*`",
                    "www.mega.io"
                ),
                Arguments.of(
                    "_`*www.mega.io*`_",
                    "www.mega.io"
                ),
                Arguments.of(
                    "```www.mega.io```",
                    "www.mega.io"
                ),
            )
        }
    }
}