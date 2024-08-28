package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.CopyMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.CopyToolbarMenuItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CopyToolbarMenuItemTest {

    private val underTest = CopyToolbarMenuItem(CopyMenuAction())

    @ParameterizedTest(name = "when are selected nodes taken down is {0}, then is copy item visible is {1}")
    @MethodSource("provideArguments")
    fun `test that the copy item visibility is adjusted`(
        noNodeTakenDown: Boolean,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = listOf(mock<TypedFolderNode>()),
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = noNodeTakenDown,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false),
    )
}