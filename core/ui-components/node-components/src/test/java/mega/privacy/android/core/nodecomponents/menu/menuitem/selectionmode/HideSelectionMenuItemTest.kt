package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HideSelectionMenuItemTest {

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isTakenDown } doReturn false
        on { isMarkedSensitive } doReturn false
        on { isSensitiveInherited } doReturn false
    }

    private val mockSensitiveFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(456L)
        on { isTakenDown } doReturn false
        on { isMarkedSensitive } doReturn true
        on { isSensitiveInherited } doReturn false
    }

    private val mockInheritedSensitiveFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(789L)
        on { isTakenDown } doReturn false
        on { isMarkedSensitive } doReturn false
        on { isSensitiveInherited } doReturn true
    }

    @Test
    fun `test shouldDisplay returns false when no access permission`() = runTest {
        val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase>()
        val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
        val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()

        val hideMenuItem = HideSelectionMenuItem(
            mock<HideMenuAction>(),
            isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase
        )

        val result = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when node is taken down`() = runTest {
        val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase>()
        val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
        val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()

        val hideMenuItem = HideSelectionMenuItem(
            mock<HideMenuAction>(),
            isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase
        )

        val result = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = false
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when node is in backups`() = runTest {
        val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase>()
        val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
        val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()

        val hideMenuItem = HideSelectionMenuItem(
            mock<HideMenuAction>(),
            isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase
        )

        val result = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = false,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when hiding action is not allowed for node`() = runTest {
        val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
            onBlocking { invoke(any()) } doReturn false
        }
        val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
        val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()

        val hideMenuItem = HideSelectionMenuItem(
            mock<HideMenuAction>(),
            isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase
        )

        val result = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns true for free account with non-sensitive nodes`() = runTest {
        val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
            onBlocking { invoke(any()) } doReturn true
        }
        val accountType = mock<AccountType> {
            on { isPaid } doReturn false
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
            onBlocking { invoke() } doReturn flowOf(accountDetail)
        }
        val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase> {
            onBlocking { invoke() } doReturn BusinessAccountStatus.Active
        }

        val hideMenuItem = HideSelectionMenuItem(
            mock<HideMenuAction>(),
            isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase
        )

        val result = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test shouldDisplay returns true for paid account with non-sensitive nodes`() = runTest {
        val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
            onBlocking { invoke(any()) } doReturn true
        }
        val accountType = mock<AccountType> {
            on { isPaid } doReturn true
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
            onBlocking { invoke() } doReturn flowOf(accountDetail)
        }
        val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase> {
            onBlocking { invoke() } doReturn BusinessAccountStatus.Active
        }

        val hideMenuItem = HideSelectionMenuItem(
            mock<HideMenuAction>(),
            isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase
        )

        val result = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test shouldDisplay returns true for expired business account`() = runTest {
        val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
            onBlocking { invoke(any()) } doReturn true
        }
        val accountType = mock<AccountType> {
            on { isPaid } doReturn true
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
            onBlocking { invoke() } doReturn flowOf(accountDetail)
        }
        val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase> {
            onBlocking { invoke() } doReturn BusinessAccountStatus.Expired
        }

        val hideMenuItem = HideSelectionMenuItem(
            mock<HideMenuAction>(),
            isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase
        )

        val result = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test shouldDisplay returns false when all nodes are sensitive for paid account`() =
        runTest {
            val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
                onBlocking { invoke(any()) } doReturn true
            }
            val accountType = mock<AccountType> {
                on { isPaid } doReturn true
            }
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { this.accountType } doReturn accountType
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
                onBlocking { invoke() } doReturn flowOf(accountDetail)
            }
            val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase> {
                onBlocking { invoke() } doReturn BusinessAccountStatus.Active
            }

            val hideMenuItem = HideSelectionMenuItem(
                mock<HideMenuAction>(),
                isHidingActionAllowedUseCase,
                monitorAccountDetailUseCase,
                getBusinessStatusUseCase
            )

            val result = hideMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockSensitiveFileNode),
                canBeMovedToTarget = true,
                noNodeInBackups = true,
                noNodeTakenDown = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay returns false when any node has inherited sensitivity for paid account`() =
        runTest {
            val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
                onBlocking { invoke(any()) } doReturn true
            }
            val accountType = mock<AccountType> {
                on { isPaid } doReturn true
            }
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { this.accountType } doReturn accountType
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
                onBlocking { invoke() } doReturn flowOf(accountDetail)
            }
            val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase> {
                onBlocking { invoke() } doReturn BusinessAccountStatus.Active
            }

            val hideMenuItem = HideSelectionMenuItem(
                mock<HideMenuAction>(),
                isHidingActionAllowedUseCase,
                monitorAccountDetailUseCase,
                getBusinessStatusUseCase
            )

            val result = hideMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockFileNode, mockInheritedSensitiveFileNode),
                canBeMovedToTarget = true,
                noNodeInBackups = true,
                noNodeTakenDown = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay ignores canBeMovedToTarget parameter`() = runTest {
        val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
            onBlocking { invoke(any()) } doReturn true
        }
        val accountType = mock<AccountType> {
            on { isPaid } doReturn true
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
            onBlocking { invoke() } doReturn flowOf(accountDetail)
        }
        val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase> {
            onBlocking { invoke() } doReturn BusinessAccountStatus.Active
        }

        val hideMenuItem = HideSelectionMenuItem(
            mock<HideMenuAction>(),
            isHidingActionAllowedUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase
        )

        // Test that it ignores canBeMovedToTarget parameter
        val result1 = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = false,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        val result2 = hideMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result1).isTrue()
        assertThat(result2).isTrue() // Should be the same regardless of canBeMovedToTarget
    }
}
