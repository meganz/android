package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.RemoveFavouriteMenuAction
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.UpdateNodeFavoriteUseCase
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveFavouriteBottomSheetMenuItemTest {
    private val updateNodeFavoriteUseCase = mock<UpdateNodeFavoriteUseCase>()
    private val removeFavouriteBottomSheetMenuItem = RemoveFavouriteBottomSheetMenuItem(
        menuAction = RemoveFavouriteMenuAction(),
        updateNodeFavoriteUseCase = updateNodeFavoriteUseCase
    )

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParameters")
    fun `test that remove favourite bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) = runTest {
        val result = removeFavouriteBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish = isNodeInRubbish,
            accessPermission = accessPermission,
            isInBackups = isInBackups,
            node = node,
            isConnected = isConnected,
        )
        assertThat(result).isEqualTo(expected)
    }

    private fun provideTestParameters() = Stream.of(
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn true
                on { isFavourite } doReturn true
            },
            false,
            false,
        ),
        Arguments.of(
            true,
            AccessPermission.FULL,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isFavourite } doReturn true
            },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isFavourite } doReturn false
            },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFolderNode> {
                on { isTakenDown } doReturn false
                on { isFavourite } doReturn true
            },
            false,
            true,
        )
    )
}