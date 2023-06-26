package test.mega.privacy.android.app.presentation.recentactions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.recentactions.RecentActionsViewModel
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsSharesType
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

@ExperimentalCoroutinesApi
class RecentActionsViewModelTest {
    private lateinit var underTest: RecentActionsViewModel

    private val getRecentActionsUseCase = mock<GetRecentActionsUseCase> {
        onBlocking { invoke() }.thenReturn(emptyList())
    }
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase> {
        onBlocking { invoke(NodeId(anyLong())) }.thenReturn(null)
    }

    private val getVisibleContactsUseCase = mock<GetVisibleContactsUseCase> {
        onBlocking { invoke() }.thenReturn(emptyList())
    }
    private val getNodeByHandle = mock<GetNodeByHandle> {
        onBlocking { invoke(any()) }.thenReturn(null)
    }
    private val getAccountDetailsUseCase = mock<GetAccountDetailsUseCase> {
        onBlocking { invoke(any()) }.thenReturn(mock())
    }
    private val setHideRecentActivityUseCase = mock<SetHideRecentActivityUseCase>()

    private val areCredentialsVerifiedUseCase = mock<AreCredentialsVerifiedUseCase> {
        onBlocking { invoke(any()) }.thenReturn(true)
    }

    private val monitorHideRecentActivityUseCase = mock<MonitorHideRecentActivityUseCase> {
        flow {
            emit(false)
        }
    }
    private val monitorNodeUpdates = FakeMonitorUpdates()

    private val node: TypedFileNode = mock {
        on { id }.thenReturn(NodeId(123))
        on { isNodeKeyDecrypted }.thenReturn(false)
    }

    private val megaRecentActionBucket = mock<RecentActionBucket> {
        on { this.nodes }.thenReturn(listOf(node))
        on { this.parentHandle }.thenReturn(321)
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(0L)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
    }

    private val megaRecentActionBucket2 = mock<RecentActionBucket> {
        on { this.nodes }.thenReturn(listOf(node))
        on { this.parentHandle }.thenReturn(111)
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(0L)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
    }

    private val megaRecentActionBucket3 = mock<RecentActionBucket> {
        on { this.nodes }.thenReturn(listOf(node))
        on { this.parentHandle }.thenReturn(111)
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(1L)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = RecentActionsViewModel(
            getRecentActionsUseCase = getRecentActionsUseCase,
            getVisibleContactsUseCase = getVisibleContactsUseCase,
            setHideRecentActivityUseCase = setHideRecentActivityUseCase,
            getNodeByHandle = getNodeByHandle,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            monitorHideRecentActivityUseCase = monitorHideRecentActivityUseCase,
            monitorNodeUpdates = monitorNodeUpdates,
            areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.recentActionItems).isEqualTo(emptyList<RecentActionItemType>())
            assertThat(initial.hideRecentActivity).isEqualTo(false)
        }
    }

    @Test
    fun `test that recent action items is updated at initialization`() =
        runTest {
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    // initialization
                    assertThat(awaitItem().size).isEqualTo(0)
                    // 1 item + 1 header
                    val item = awaitItem()
                    assertThat(item.filterIsInstance<RecentActionItemType.Item>().size).isEqualTo(1)
                    assertThat(item.filterIsInstance<RecentActionItemType.Header>().size).isEqualTo(
                        1
                    )
                }
        }

    @Test
    fun `test that 2 recent action items are grouped under same header if timestamp is same`() =
        runTest {
            whenever(getRecentActionsUseCase()).thenReturn(
                listOf(
                    megaRecentActionBucket,
                    megaRecentActionBucket2,
                )
            )

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    // initialization
                    assertThat(awaitItem().size).isEqualTo(0)
                    // 1 header + 2 items
                    val item = awaitItem()
                    assertThat(item.filterIsInstance<RecentActionItemType.Item>().size)
                        .isEqualTo(2)
                    assertThat(item.filterIsInstance<RecentActionItemType.Header>().size)
                        .isEqualTo(1)
                }
        }

    @Test
    fun `test that 2 recent action items are under two different headers if timestamp is different`() =
        runTest {
            whenever(getRecentActionsUseCase()).thenReturn(
                listOf(
                    megaRecentActionBucket,
                    megaRecentActionBucket3,
                )
            )

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem().size).isEqualTo(0)
                    // 2 header + 2 items
                    val item = awaitItem()
                    assertThat(item.filterIsInstance<RecentActionItemType.Item>().size)
                        .isEqualTo(2)
                    assertThat(item.filterIsInstance<RecentActionItemType.Header>().size)
                        .isEqualTo(2)
                }
        }

    @Test
    fun `test that the recent action user name item is populated with the fullName if retrieved from email`() =
        runTest {
            val expected = "FirstName LastName"
            val contact = mock<ContactData> {
                on { fullName }.thenReturn(expected)
            }
            val contactItem = mock<ContactItem> {
                on { email }.thenReturn("aaa@aaa.com")
                on { contactData }.thenReturn(contact)
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getVisibleContactsUseCase()).thenReturn(listOf(contactItem))

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).userName)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that the recent action user name item is populated with empty string if not retrieved from email`() =
        runTest {
            val expected = ""
            val contactItem = mock<ContactItem> {
                on { email }.thenReturn("aaa@aaa.com")
                on { contactData }.thenReturn(mock())
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getVisibleContactsUseCase()).thenReturn(listOf(contactItem))

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).userName)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that recent action current user is owner item is set to true if the user email corresponds to the current user`() =
        runTest {
            val userAccount = UserAccount(
                userId = null,
                email = "aaa@aaa.com",
                fullName = "name",
                isBusinessAccount = false,
                isMasterBusinessAccount = false,
                accountTypeIdentifier = null,
                accountTypeString = ""
            )
            whenever(getAccountDetailsUseCase(false)).thenReturn(userAccount)
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).currentUserIsOwner)
                        .isEqualTo(true)
                }
        }

    @Test
    fun `test that recent action current user is owner item is set to false if the user email does not corresponds to the current user`() =
        runTest {
            val userAccount = UserAccount(
                userId = null,
                email = "bbb@aaa.com",
                fullName = "name",
                isBusinessAccount = false,
                isMasterBusinessAccount = false,
                accountTypeIdentifier = null,
                accountTypeString = ""
            )
            whenever(getAccountDetailsUseCase(false)).thenReturn(userAccount)
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).currentUserIsOwner)
                        .isEqualTo(false)
                }
        }

    @Test
    fun `test that the recent action parent folder name item is set to empty string if not retrieved from the parent node`() =
        runTest {
            val expected = ""
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).parentFolderName)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that the recent action parent folder name item is set if retrieved from the parent node`() =
        runTest {
            val expected = "Cloud drive"
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { name }.thenReturn(expected)
                on { isNodeKeyDecrypted }.thenReturn(false)
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).parentFolderName)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that the recent action shares type item is set to INCOMING_SHARES if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.INCOMING_SHARES
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isIncomingShare }.thenReturn(true)
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).parentFolderSharesType)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that the recent action shares type item is set to OUTGOING_SHARES if parent root node is in outgoing shares`() =
        runTest {
            val expected = RecentActionsSharesType.OUTGOING_SHARES
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isShared }.thenReturn(true)
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).parentFolderSharesType)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that the recent action shares type item is set to PENDING_OUTGOING_SHARES if parent root node is in pending shares`() =
        runTest {
            val expected = RecentActionsSharesType.PENDING_OUTGOING_SHARES
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isPendingShare }.thenReturn(true)
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).parentFolderSharesType)
                        .isEqualTo(expected)
                }
        }

    @Test
    fun `test that the recent action shares type item is set to NONE if parent root node is not shared`() =
        runTest {
            val expected = RecentActionsSharesType.NONE
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isIncomingShare }.thenReturn(false)
                on { isShared }.thenReturn(false)
                on { isPendingShare }.thenReturn(false)
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).parentFolderSharesType)
                        .isEqualTo(expected)
                }
        }


    @Test
    fun `test that recent action items is updated when receiving a node update`() =
        runTest {
            whenever(getRecentActionsUseCase()).thenReturn(emptyList())

            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem().size).isEqualTo(0)
                    advanceUntilIdle()
                    monitorNodeUpdates.emit(NodeUpdate(emptyMap()))
                    whenever(getRecentActionsUseCase()).thenReturn(
                        listOf(megaRecentActionBucket)
                    )
                    assertThat(awaitItem().size).isEqualTo(2)
                }
            verify(getRecentActionsUseCase, times(2)).invoke()
        }

    @Test
    fun `test that hide recent activity is set with value of monitor hide recent activity`() =
        runTest {
            whenever(monitorHideRecentActivityUseCase()).thenReturn(
                flow {
                    emit(true)
                    emit(false)
                }
            )
            underTest.state.map { it.hideRecentActivity }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(false)
                    assertThat(awaitItem()).isEqualTo(true)
                    assertThat(awaitItem()).isEqualTo(false)
                }
        }

    @Test
    fun `test that disable hide recent activity calls setHideRecentActivity use case with value true `() =
        runTest {
            underTest.disableHideRecentActivitySetting()
            advanceUntilIdle()
            verify(setHideRecentActivityUseCase).invoke(false)
        }

    @Test
    fun `test that calling select will set selected and snapShotActionList properties`() =
        runTest {
            val expectedSelected = RecentActionItemType.Item(
                bucket = megaRecentActionBucket
            )
            val expectedSnapshotActionList = listOf(megaRecentActionBucket)
            whenever(getRecentActionsUseCase()).thenReturn(expectedSnapshotActionList)
            assertThat(underTest.selected).isEqualTo(null)
            assertThat(underTest.snapshotActionList).isEqualTo(null)
            advanceUntilIdle()
            underTest.select(expectedSelected)
            assertThat(underTest.selected).isEqualTo(expectedSelected.bucket)
            assertThat(underTest.snapshotActionList).isEqualTo(expectedSnapshotActionList)
        }

    @Test
    fun `test that isKeyVerified gets updated in recent action items`() = runTest {
        whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
        underTest.state.map { it.recentActionItems }.distinctUntilChanged().test {
            awaitItem()
            assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).isKeyVerified)
                .isEqualTo(true)
        }
    }

    @Test
    fun `test that recent action current user is owner item is set to false if get user account throws an exception`() =
        runTest {
            whenever(getAccountDetailsUseCase(false)).thenAnswer {
                throw MegaException(
                    1,
                    "Account call failed"
                )
            }
            whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket))
            underTest.state.map { it.recentActionItems }.distinctUntilChanged()
                .test {
                    awaitItem()
                    assertThat((awaitItem().filterIsInstance<RecentActionItemType.Item>()[0]).currentUserIsOwner)
                        .isEqualTo(false)
                }
        }
}
