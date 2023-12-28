package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.RestoreMenuAction
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestoreBottomSheetMenuItemTest {

    private val restoreBottomSheetMenuItem = RestoreBottomSheetMenuItem(RestoreMenuAction())

    @ParameterizedTest(name = "isNodeInRubbish {0} - expected {1}")
    @MethodSource("provideTestParameters")
    fun `test that restore bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        expected: Boolean,
    ) {
        val result = restoreBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish,
            null,
            false,
            mock<TypedFolderNode>(),
            true
        )
        assertEquals(expected, result)
    }

    private fun provideTestParameters() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false),
    )

}