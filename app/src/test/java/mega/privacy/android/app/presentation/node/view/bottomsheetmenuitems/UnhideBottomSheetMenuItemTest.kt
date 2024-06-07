package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.node.model.menuaction.UnhideMenuAction
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnhideBottomSheetMenuItemTest {
    private lateinit var unhideBottomSheetMenuItem: UnhideBottomSheetMenuItem

    private val menuAction = UnhideMenuAction()
    private val nodeId = NodeId(123L)

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val accountType = mock<AccountType> {
        on { isPaid } doReturn true
    }
    private val accountLevelDetail = mock<AccountLevelDetail> {
        on { this.accountType } doReturn accountType
    }
    private val accountDetail = mock<AccountDetail> {
        on { levelDetail } doReturn accountLevelDetail
    }

    @BeforeEach
    fun setUp() {
        unhideBottomSheetMenuItem = UnhideBottomSheetMenuItem(
            menuAction = menuAction,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            isHidingActionAllowedUseCase = isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase
        )
    }

    @Test
    fun `test that shouldDisplay doReturn false when HiddenNodes feature flag is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) doReturn false
            whenever(isHidingActionAllowedUseCase(nodeId)) doReturn true

            val result = unhideBottomSheetMenuItem.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = AccessPermission.OWNER,
                isInBackups = false,
                node = mock(),
                isConnected = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay doReturn false when node is in rubbish`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) doReturn true
        whenever(isHidingActionAllowedUseCase(nodeId)) doReturn true
        val result = unhideBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = mock(),
            isConnected = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay doReturn false when access permission is not OWNER`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) doReturn true
            whenever(isHidingActionAllowedUseCase(nodeId)) doReturn true
            val result = unhideBottomSheetMenuItem.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = AccessPermission.READ,
                isInBackups = false,
                node = mock(),
                isConnected = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay doReturn false when node is taken down`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) doReturn true
        whenever(isHidingActionAllowedUseCase(nodeId)) doReturn true
        val node = mock<TypedNode> {
            on { isTakenDown } doReturn true
        }

        val result = unhideBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay doReturn false when not paid`() = runTest {
        val accountType = mock<AccountType> {
            on { isPaid } doReturn false
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) doReturn true
        whenever(isHidingActionAllowedUseCase(nodeId)) doReturn true
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        val node = mock<TypedNode> {
            on { id } doReturn nodeId
            on { isMarkedSensitive } doReturn true
        }

        val result = unhideBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay doReturn true when all conditions are met`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) doReturn true
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        whenever(isHidingActionAllowedUseCase(nodeId)) doReturn true
        val node = mock<TypedNode> {
            on { id } doReturn nodeId
            on { isMarkedSensitive } doReturn true
        }

        val result = unhideBottomSheetMenuItem.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        assertThat(result).isTrue()
    }
}