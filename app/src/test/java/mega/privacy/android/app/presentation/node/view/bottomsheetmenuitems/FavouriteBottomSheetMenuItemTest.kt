package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.FavouriteMenuAction
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.UpdateNodeFavoriteUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FavouriteBottomSheetMenuItemTest {

    private val updateNodeFavoriteUseCase = mock<UpdateNodeFavoriteUseCase>()
    private val underTest = FavouriteBottomSheetMenuItem(
        menuAction = FavouriteMenuAction(),
        updateNodeFavoriteUseCase = updateNodeFavoriteUseCase
    )

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParameters")
    fun `test that favourite bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = isNodeInRubbish,
            accessPermission = accessPermission,
            isInBackups = isInBackups,
            node = node,
            isConnected = isConnected,
        )
        Truth.assertThat(result).isEqualTo(expected)
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
            true,
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
            false,
        )
    )
}