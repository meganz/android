package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.node.model.menuaction.VerifyMenuAction
import mega.privacy.android.app.presentation.search.navigation.cannotVerifyUserRoute
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerifyBottomSheetMenuItemTest {

    private val menuAction = VerifyMenuAction()
    private val nodeId = mock<NodeId> {
        on { longValue } doReturn 123L
    }
    private val sharedInfo = mock<ShareData> {
        on { nodeHandle } doReturn 123L
        on { user } doReturn "user"
        on { isPending } doReturn true
        on { isVerified } doReturn false
        on { timeStamp } doReturn 123L
        on { access } doReturn AccessPermission.OWNER
    }
    private val getUnverifiedIncomingShares = mock<GetUnverifiedIncomingShares>()
    private val getUnverifiedOutgoingShares = mock<GetUnverifiedOutgoingShares>()
    private val underTest: VerifyBottomSheetMenuItem = VerifyBottomSheetMenuItem(
        menuAction = menuAction,
        getUnverifiedIncomingShares = getUnverifiedIncomingShares,
        getUnverifiedOutgoingShares = getUnverifiedOutgoingShares
    )

    @Test
    fun `shouldDisplay returns true when shared info is null`() = runTest {
        val node = mock<TypedFolderNode>()
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
        )
        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `shouldDisplay returns true when node is unverified incoming share`() = runTest {
        val node = mock<TypedFolderNode> {
            on { isIncomingShare } doReturn true
            on { id } doReturn nodeId
        }
        whenever(getUnverifiedIncomingShares(SortOrder.ORDER_NONE)).thenReturn(listOf(sharedInfo))
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
        )
        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `shouldDisplay returns true when node is unverified outgoing share`() = runTest {
        val node = mock<TypedFolderNode> {
            on { isIncomingShare } doReturn false
            on { id } doReturn nodeId
            on { isOutShare() } doReturn true
        }
        whenever(getUnverifiedOutgoingShares(SortOrder.ORDER_NONE)).thenReturn(listOf(sharedInfo))
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
        )
        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that verify bottom sheet menu item onClick function opens dialog`() = runTest {
        val node = mock<TypedFolderNode> {
            on { id } doReturn nodeId
            on { isOutShare() } doReturn true
            on { isPendingShare } doReturn true
        }
        whenever(getUnverifiedOutgoingShares(SortOrder.ORDER_NONE)).thenReturn(listOf(sharedInfo))
        val onDismiss = mock<() -> Unit>()
        val actionHandler = mock<(menuAction: MenuAction, node: TypedNode) -> Unit>()
        val navController = mock<NavHostController>()
        underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
        )
        val onClickFunction = underTest.getOnClickFunction(
            node,
            onDismiss,
            actionHandler,
            navController
        )
        onClickFunction()
        verify(onDismiss).invoke()
        verify(navController).navigate("$cannotVerifyUserRoute/${sharedInfo.user}")
    }
}