package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.mapper.NodeLabelResourceMapper
import mega.privacy.android.app.presentation.node.model.menuaction.LabelMenuAction
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LabelBottomSheetMenuItemTest {

    private val labelResourceMapper = NodeLabelResourceMapper()
    private val nodeLabelMapper = NodeLabelMapper()
    private val underTest = LabelBottomSheetMenuItem(
        menuAction = LabelMenuAction(),
        nodeLabelMapper = nodeLabelMapper,
        labelResourceMapper = labelResourceMapper
    )

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - expected {4}")
    @MethodSource("provideTestParameters")
    fun `test that label bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = isNodeInRubbish,
            accessPermission = accessPermission,
            isInBackups = isInBackups,
            node = node,
            isConnected = true
        )
        assertEquals(expected, result)
    }

    private fun provideTestParameters() = Stream.of(
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn true },
            false
        ),
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.READWRITE,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            true,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            true
        ),
    )

}