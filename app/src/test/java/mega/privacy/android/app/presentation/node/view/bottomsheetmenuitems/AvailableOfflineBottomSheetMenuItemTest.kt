package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.node.model.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvailableOfflineBottomSheetMenuItemTest {

    private val removeOfflineNodeUseCase = mock<RemoveOfflineNodeUseCase>()
    private val isFolderEmptyUseCase = mock<IsFolderEmptyUseCase>()
    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private val underTest = AvailableOfflineBottomSheetMenuItem(
        menuAction = AvailableOfflineMenuAction(),
        removeOfflineNodeUseCase = removeOfflineNodeUseCase,
        isFolderEmptyUseCase = isFolderEmptyUseCase,
        scope = scope
    )

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParametersWithEmptyFolder")
    fun `test that available offline bottom sheet menu item visibility is correct when folder is not empty`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) = runTest {
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            isConnected,
        )
        assertEquals(expected, result)
    }

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - isConnected {4} - expected {5}")
    @MethodSource("provideTestParametersWithNonEmptyFolder")
    fun `test that available offline bottom sheet menu item visibility is correct when folder is empty`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
        expected: Boolean,
    ) = runTest {
        whenever(isFolderEmptyUseCase(node)).thenReturn(true)
        val result = underTest.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            isConnected,
        )
        assertEquals(expected, result)
    }

    private fun provideTestParametersWithEmptyFolder() = Stream.of(
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn true },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false,
            true,
        ),
    )

    private fun provideTestParametersWithNonEmptyFolder() = Stream.of(
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn true },
            false,
            false,
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false,
            false,
        ),
    )

    @Test
    fun `test that available offline bottom sheet menu item has the correct menu action`() {
        assertThat(underTest.menuAction).isInstanceOf(AvailableOfflineMenuAction::class.java)
    }

    @Test
    fun `test that remove offline is called when node is available offline`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(123456L)
            on { isAvailableOffline } doReturn true
        }
        val onDismiss = mock<() -> Unit>()
        val actionHandler = mock<(MenuAction, TypedNode) -> Unit>()
        val navController = mock<NavHostController>()
        val onClick = underTest.getOnClickFunction(node, onDismiss, actionHandler, navController)

        onClick()
        verify(removeOfflineNodeUseCase).invoke(node.id)
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that remove offline is not called when node is not available offline`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(123456L)
            on { isAvailableOffline } doReturn false
        }
        val onDismiss = mock<() -> Unit>()
        val actionHandler = mock<(MenuAction, TypedNode) -> Unit>()
        val navController = mock<NavHostController>()
        val onClick = underTest.getOnClickFunction(node, onDismiss, actionHandler, navController)

        onClick()
        verifyNoInteractions(removeOfflineNodeUseCase)
        verify(onDismiss).invoke()
        verify(actionHandler).invoke(any(), any())
    }


}