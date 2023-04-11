package test.mega.privacy.android.app.presentation.folderlink

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.folderlink.FolderLinkViewModel
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.domain.entity.folderlink.FetchFolderNodesResult
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodes
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolder
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class FolderLinkViewModelTest {

    private lateinit var underTest: FolderLinkViewModel
    private val monitorViewType = mock<MonitorViewType>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val loginToFolder = mock<LoginToFolder>()
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase = mock()
    private val copyNodeUseCase: CopyNodeUseCase = mock()
    private val copyRequestMessageMapper: CopyRequestMessageMapper = mock()
    private val hasCredentials: HasCredentials = mock()
    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val setViewType: SetViewType = mock()
    private val fetchFolderNodesUseCase: FetchFolderNodesUseCase = mock()
    private val getFolderParentNodeUseCase: GetFolderParentNodeUseCase = mock()
    private val getFolderLinkChildrenNodes: GetFolderLinkChildrenNodes = mock()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        underTest = FolderLinkViewModel(
            monitorConnectivityUseCase,
            monitorViewType,
            loginToFolder,
            checkNameCollisionUseCase,
            copyNodeUseCase,
            copyRequestMessageMapper,
            hasCredentials,
            rootNodeExistsUseCase,
            setViewType,
            fetchFolderNodesUseCase,
            getFolderParentNodeUseCase,
            getFolderLinkChildrenNodes
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.isInitialState).isEqualTo(true)
            assertThat(initial.isLoginComplete).isEqualTo(false)
            assertThat(initial.isNodesFetched).isEqualTo(false)
            assertThat(initial.askForDecryptionKeyDialog).isEqualTo(false)
            assertThat(initial.collisions).isNull()
            assertThat(initial.copyResultText).isNull()
            assertThat(initial.copyThrowable).isNull()
            assertThat(initial.shouldLogin).isNull()
            assertThat(initial.hasDbCredentials).isFalse()
            assertThat(initial.nodesList).isEmpty()
            assertThat(initial.rootNode).isNull()
            assertThat(initial.parentNode).isNull()
            assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initial.title).isEqualTo("")
            assertThat(initial.isMultipleSelect).isFalse()
            assertThat(initial.finishActivity).isFalse()
            assertThat(initial.errorDialogTitle).isEqualTo(-1)
            assertThat(initial.errorDialogContent).isEqualTo(-1)
            assertThat(initial.snackBarMessage).isEqualTo(-1)
        }
    }

    @Test
    fun `test that isNodesFetched updated correctly`() = runTest {
        underTest.state.map { it.isNodesFetched }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            underTest.updateIsNodesFetched(true)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that on login into folder and on result OK values are updated correctly`() = runTest {
        val folderLink = "abcd"
        whenever(loginToFolder(folderLink)).thenReturn(FolderLoginStatus.SUCCESS)
        underTest.state.test {
            underTest.folderLogin(folderLink)
            val newValue = expectMostRecentItem()
            assertThat(newValue.isLoginComplete).isTrue()
            assertThat(newValue.isInitialState).isFalse()
        }
    }

    @Test
    fun `test that on login into folder and on result API_INCOMPLETE values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            whenever(loginToFolder(folderLink)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            underTest.state.test {
                underTest.folderLogin(folderLink)
                val newValue = expectMostRecentItem()
                assertThat(newValue.isLoginComplete).isFalse()
                assertThat(newValue.isInitialState).isFalse()
                assertThat(newValue.askForDecryptionKeyDialog).isTrue()
            }
        }

    @Test
    fun `test that on login into folder and on result INCORRECT_KEY values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            val decryptionIntroduced = true
            whenever(loginToFolder(folderLink)).thenReturn(FolderLoginStatus.INCORRECT_KEY)
            underTest.state.test {
                underTest.folderLogin(folderLink, decryptionIntroduced)
                val newValue = expectMostRecentItem()
                assertThat(newValue.isLoginComplete).isFalse()
                assertThat(newValue.isInitialState).isFalse()
                assertThat(newValue.askForDecryptionKeyDialog).isEqualTo(decryptionIntroduced)
            }
        }

    @Test
    fun `test that on login into folder and on result ERROR values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            whenever(loginToFolder(folderLink)).thenReturn(FolderLoginStatus.ERROR)
            underTest.state.test {
                underTest.folderLogin(folderLink)
                val newValue = expectMostRecentItem()
                assertThat(newValue.isLoginComplete).isFalse()
                assertThat(newValue.isInitialState).isFalse()
                assertThat(newValue.askForDecryptionKeyDialog).isFalse()
            }
        }

    @Test
    fun `test that on valid credentials and no root node shouldShowLogin is returned true`() =
        runTest {
            whenever(hasCredentials()).thenReturn(true)
            whenever(rootNodeExistsUseCase()).thenReturn(false)
            underTest.state.test {
                underTest.checkLoginRequired()
                val value = expectMostRecentItem()
                assertThat(value.shouldLogin).isTrue()
                assertThat(value.hasDbCredentials).isTrue()
            }
        }

    @Test
    fun `test that on valid credentials and root node shouldShowLogin is returned false`() =
        runTest {
            whenever(hasCredentials()).thenReturn(true)
            whenever(rootNodeExistsUseCase()).thenReturn(true)
            underTest.state.test {
                underTest.checkLoginRequired()
                val value = expectMostRecentItem()
                assertThat(value.shouldLogin).isFalse()
                assertThat(value.hasDbCredentials).isTrue()
            }
        }

    @Test
    fun `test that launchCollisionActivity values are reset `() = runTest {
        underTest.state.test {
            underTest.resetLaunchCollisionActivity()
            val newValue = expectMostRecentItem()
            assertThat(newValue.collisions).isNull()
        }
    }

    @Test
    fun `test that showCopyResult values are reset `() = runTest {
        underTest.state.test {
            underTest.resetShowCopyResult()
            val newValue = expectMostRecentItem()
            assertThat(newValue.copyResultText).isNull()
            assertThat(newValue.copyThrowable).isNull()
        }
    }

    @Test
    fun `test that askForDecryptionKeyDialog values are reset `() = runTest {
        underTest.state.test {
            underTest.resetAskForDecryptionKeyDialog()
            val newValue = expectMostRecentItem()
            assertThat(newValue.askForDecryptionKeyDialog).isFalse()
        }
    }

    @Test
    fun `test that setViewType is called with GRID when current view type is list`() = runTest {
        assertThat(underTest.state.value.currentViewType).isEqualTo(ViewType.LIST)
        underTest.onChangeViewTypeClicked()
        verify(setViewType).invoke(ViewType.GRID)
    }

    @Test
    fun `test that fetch nodes returns correct result`() = runTest {
        val base64Handle = "1234"
        val rootNodeName = "RootNode"
        val rootNode = mock<TypedFolderNode>()
        val parentNode = mock<TypedFolderNode>()
        val node1 = mock<TypedNode>()
        val node2 = mock<TypedNode>()
        val childrenNodes = listOf(node1, node2)
        val fetchFolderNodeResult = FetchFolderNodesResult(rootNode, parentNode, childrenNodes)

        whenever(rootNode.name).thenReturn(rootNodeName)
        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            val newValue = expectMostRecentItem()
            assertThat(newValue.isNodesFetched).isTrue()
            assertThat(newValue.nodesList.size).isEqualTo(childrenNodes.size)
            assertThat(newValue.rootNode).isEqualTo(rootNode)
            assertThat(newValue.parentNode).isEqualTo(parentNode)
            assertThat(newValue.title).isEqualTo(rootNodeName)
        }
    }

    @Test
    fun `test that onItemLongClick selects the clicked nodes`() = runTest {
        val base64Handle = "1234"
        val childNode = mock<TypedNode>()
        val childrenNodes = listOf(childNode)
        val fetchFolderNodeResult = FetchFolderNodesResult(mock(), mock(), childrenNodes)
        val nodeUIItem = NodeUIItem(childNode, isSelected = false, isInvisible = false)

        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            underTest.onItemLongClick(nodeUIItem)
            val newValue = expectMostRecentItem()
            assertThat(newValue.isMultipleSelect).isTrue()
            assertThat(newValue.nodesList[0].isSelected).isTrue()
        }
    }

    @Test
    fun `test that onSelectAllClicked selects all the nodes`() = runTest {
        val base64Handle = "1234"
        val childrenNodes = listOf<TypedNode>(mock(), mock())
        val fetchFolderNodeResult = FetchFolderNodesResult(mock(), mock(), childrenNodes)

        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            underTest.onSelectAllClicked()
            val newValue = expectMostRecentItem()
            assertThat(newValue.isMultipleSelect).isTrue()
            newValue.nodesList.forEach {
                assertThat(it.isSelected).isTrue()
            }
        }
    }

    @Test
    fun `test that onClearAllClicked unselects all the nodes`() = runTest {
        val base64Handle = "1234"
        val childrenNodes = listOf<TypedNode>(mock(), mock())
        val fetchFolderNodeResult = FetchFolderNodesResult(mock(), mock(), childrenNodes)

        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            underTest.onSelectAllClicked()
            var newValue = expectMostRecentItem()
            assertThat(newValue.isMultipleSelect).isTrue()
            newValue.nodesList.forEach {
                assertThat(it.isSelected).isTrue()
            }
            underTest.onClearAllClicked()
            newValue = expectMostRecentItem()
            assertThat(newValue.isMultipleSelect).isFalse()
            newValue.nodesList.forEach {
                assertThat(it.isSelected).isFalse()
            }
        }
    }

    @Test
    fun `test that handleBackPress returns correct result`() = runTest {
        val base64Handle = "1234"
        val newChildHandle = 1234L
        val newParentNodeName = "New Parent"
        val oldParentNode = mock<TypedFolderNode>()
        val newParentNode = mock<TypedFolderNode>()
        val oldChildNode = mock<TypedNode>()
        val newChildNode = mock<TypedNode>()
        val oldChildrenNodes = listOf(oldChildNode)
        val newChildrenNodes = listOf(newChildNode)
        val fetchFolderNodeResult =
            FetchFolderNodesResult(mock(), oldParentNode, oldChildrenNodes)

        whenever(newParentNode.name).thenReturn(newParentNodeName)
        whenever(newChildNode.id.longValue).thenReturn(newChildHandle)
        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)
        whenever(getFolderParentNodeUseCase(oldParentNode.id)).thenReturn(newParentNode)
        whenever(getFolderLinkChildrenNodes(newParentNode.id.longValue, null))
            .thenReturn(newChildrenNodes)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            underTest.handleBackPress()
            val newValue = expectMostRecentItem()
            assertThat(newValue.parentNode).isEqualTo(newParentNode)
            assertThat(newValue.title).isEqualTo(newParentNodeName)
            assertThat(newValue.nodesList[0].id.longValue).isEqualTo(newChildHandle)
        }
    }

}