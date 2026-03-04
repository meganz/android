package mega.privacy.android.app.textEditor

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.DefaultTextEditorTopBarSlots
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorConditionalTopBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarSlot
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class TextEditorTopBarSlotsResolverTest {

    companion object {
        private val lineNumbersAndMore = listOf(
            TextEditorTopBarSlot.LineNumbers,
            TextEditorTopBarSlot.More,
        )

        @JvmStatic
        fun provideEditCreateModeCases(): Stream<Arguments> = Stream.of(
            Arguments.of(TextEditorMode.Edit, Constants.OFFLINE_ADAPTER),
            Arguments.of(TextEditorMode.Create, Constants.FILE_LINK_ADAPTER),
        )

        @JvmStatic
        fun provideViewModeCases(): Stream<Arguments> = Stream.of(
            Arguments.of(
                Constants.OFFLINE_ADAPTER,
                listOf(
                    TextEditorTopBarSlot.LineNumbers,
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Share),
                    TextEditorTopBarSlot.More,
                ),
            ),
            Arguments.of(NodeSourceTypeInt.RUBBISH_BIN_ADAPTER, lineNumbersAndMore),
            Arguments.of(
                Constants.FILE_LINK_ADAPTER,
                listOf(
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                    TextEditorTopBarSlot.LineNumbers,
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.SendToChat),
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Share),
                    TextEditorTopBarSlot.More,
                ),
            ),
            Arguments.of(
                Constants.ZIP_ADAPTER,
                listOf(
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                    TextEditorTopBarSlot.LineNumbers,
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.SendToChat),
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Share),
                    TextEditorTopBarSlot.More,
                ),
            ),
            Arguments.of(
                Constants.FOLDER_LINK_ADAPTER,
                listOf(
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                    TextEditorTopBarSlot.LineNumbers,
                    TextEditorTopBarSlot.More,
                ),
            ),
            Arguments.of(
                Constants.VERSIONS_ADAPTER,
                listOf(
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                    TextEditorTopBarSlot.LineNumbers,
                    TextEditorTopBarSlot.More,
                ),
            ),
            Arguments.of(
                Constants.FROM_CHAT,
                listOf(
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                    TextEditorTopBarSlot.LineNumbers,
                    TextEditorTopBarSlot.More,
                ),
            ),
            Arguments.of(
                NodeSourceTypeInt.INCOMING_SHARES_ADAPTER,
                listOf(
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                    TextEditorTopBarSlot.LineNumbers,
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.SendToChat),
                    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Share),
                    TextEditorTopBarSlot.More,
                ),
            ),
            Arguments.of(NodeSourceTypeInt.FILE_BROWSER_ADAPTER, DefaultTextEditorTopBarSlots),
            Arguments.of(null, DefaultTextEditorTopBarSlots),
            Arguments.of(9999, DefaultTextEditorTopBarSlots),
        )
    }

    @ParameterizedTest(
        name = "mode={0} returns LineNumbers and More only"
    )
    @MethodSource("provideEditCreateModeCases")
    fun `test that Edit and Create mode return LineNumbers and More only`(
        mode: TextEditorMode,
        nodeSourceType: Int?,
    ) {
        val result = computeTextEditorTopBarSlots(nodeSourceType, mode)
        assertThat(result).containsExactly(
            TextEditorTopBarSlot.LineNumbers,
            TextEditorTopBarSlot.More,
        )
    }

    @ParameterizedTest(
        name = "View mode nodeSourceType={0} -> slots list"
    )
    @MethodSource("provideViewModeCases")
    fun `test that View mode returns correct ordered slots for nodeSourceType`(
        nodeSourceType: Int?,
        expectedSlots: List<TextEditorTopBarSlot>,
    ) {
        val result = computeTextEditorTopBarSlots(nodeSourceType, TextEditorMode.View)
        assertThat(result).isEqualTo(expectedSlots)
    }
}
