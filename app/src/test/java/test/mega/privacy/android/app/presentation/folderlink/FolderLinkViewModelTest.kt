package test.mega.privacy.android.app.presentation.folderlink

import android.content.Intent
import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetPublicNodeListByIds
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.folderlink.FolderLinkViewModel
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.folderlink.FetchFolderNodesResult
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetLocalFileForNode
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderLinkViewModelTest {

    private lateinit var underTest: FolderLinkViewModel
    private val monitorViewType: MonitorViewType = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val loginToFolderUseCase: LoginToFolderUseCase = mock()
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase = mock()
    private val legacyCopyNodeUseCase: LegacyCopyNodeUseCase = mock()
    private val copyRequestMessageMapper: CopyRequestMessageMapper = mock()
    private val hasCredentials: HasCredentials = mock()
    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val setViewType: SetViewType = mock()
    private val fetchFolderNodesUseCase: FetchFolderNodesUseCase = mock()
    private val getFolderParentNodeUseCase: GetFolderParentNodeUseCase = mock()
    private val getFolderLinkChildrenNodesUseCase: GetFolderLinkChildrenNodesUseCase = mock()
    private val addNodeType: AddNodeType = mock()
    private val getPublicNodeListByIds: GetPublicNodeListByIds = mock()
    private val getNodeUseCase: GetNodeUseCase = mock()
    private val getStringFromStringResMapper: GetStringFromStringResMapper = mock()
    private val areAchievementsEnabledUseCase: AreAchievementsEnabledUseCase = mock()
    private val getAccountTypeUseCase: GetAccountTypeUseCase = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()
    private val getPricing: GetPricing = mock()
    private val containsMediaItemUseCase: ContainsMediaItemUseCase = mock()
    private val getLocalFileForNode: GetLocalFileForNode = mock()
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase =
        mock()
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase = mock()
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase =
        mock()
    private val httpServerStart: MegaApiHttpServerStartUseCase = mock()
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase = mock()
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase = mock()
    private val getFileUriUseCase: GetFileUriUseCase = mock()

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun setup() {
        whenever(monitorViewType.invoke()).thenReturn(emptyFlow())
        resetMocks()
        initViewModel()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun resetMocks() {
        reset(
            isConnectedToInternetUseCase,
            monitorViewType,
            loginToFolderUseCase,
            checkNameCollisionUseCase,
            legacyCopyNodeUseCase,
            copyRequestMessageMapper,
            hasCredentials,
            rootNodeExistsUseCase,
            setViewType,
            fetchFolderNodesUseCase,
            getFolderParentNodeUseCase,
            getFolderLinkChildrenNodesUseCase,
            addNodeType,
            getPublicNodeListByIds,
            getNodeUseCase,
            getStringFromStringResMapper,
            areAchievementsEnabledUseCase,
            getAccountTypeUseCase,
            getCurrentUserEmail,
            getPricing,
            containsMediaItemUseCase,
            getLocalFileForNode,
            getLocalFolderLinkFromMegaApiFolderUseCase,
            megaApiFolderHttpServerStartUseCase,
            megaApiFolderHttpServerIsRunningUseCase,
            httpServerStart,
            httpServerIsRunning,
            getLocalFolderLinkFromMegaApiUseCase,
            getFileUriUseCase
        )
    }

    private fun initViewModel() {
        underTest = FolderLinkViewModel(
            isConnectedToInternetUseCase,
            monitorViewType,
            loginToFolderUseCase,
            checkNameCollisionUseCase,
            legacyCopyNodeUseCase,
            copyRequestMessageMapper,
            hasCredentials,
            rootNodeExistsUseCase,
            setViewType,
            fetchFolderNodesUseCase,
            getFolderParentNodeUseCase,
            getFolderLinkChildrenNodesUseCase,
            addNodeType,
            getPublicNodeListByIds,
            getNodeUseCase,
            getStringFromStringResMapper,
            areAchievementsEnabledUseCase,
            getAccountTypeUseCase,
            getCurrentUserEmail,
            getPricing,
            containsMediaItemUseCase,
            getLocalFileForNode,
            getLocalFolderLinkFromMegaApiFolderUseCase,
            megaApiFolderHttpServerStartUseCase,
            megaApiFolderHttpServerIsRunningUseCase,
            httpServerStart,
            httpServerIsRunning,
            getLocalFolderLinkFromMegaApiUseCase,
            getFileUriUseCase
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
            assertThat(initial.selectedNodeCount).isEqualTo(0)
            assertThat(initial.finishActivity).isFalse()
            assertThat(initial.importNode).isNull()
            assertThat(initial.openFile).isInstanceOf(consumed<Intent>().javaClass)
            assertThat(initial.downloadNodes).isInstanceOf(consumed<List<MegaNode>>().javaClass)
            assertThat(initial.selectImportLocation).isEqualTo(consumed)
            assertThat(initial.errorDialogTitle).isEqualTo(-1)
            assertThat(initial.errorDialogContent).isEqualTo(-1)
            assertThat(initial.snackBarMessage).isEqualTo(-1)
        }
    }

    @Test
    fun `test that on login into folder and on result OK values are updated correctly`() = runTest {
        val folderLink = "abcd"
        whenever(loginToFolderUseCase(folderLink)).thenReturn(FolderLoginStatus.SUCCESS)
        underTest.state.test {
            underTest.folderLogin(folderLink)
            val newValue = expectMostRecentItem()
            assertThat(newValue.isLoginComplete).isTrue()
            assertThat(newValue.isInitialState).isFalse()
            assertThat(newValue.errorDialogTitle).isEqualTo(-1)
            assertThat(newValue.errorDialogContent).isEqualTo(-1)
        }
    }

    @Test
    fun `test that on login into folder and on result API_INCOMPLETE values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            whenever(loginToFolderUseCase(folderLink)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            underTest.state.test {
                underTest.folderLogin(folderLink)
                val newValue = expectMostRecentItem()
                assertThat(newValue.isLoginComplete).isFalse()
                assertThat(newValue.isInitialState).isFalse()
                assertThat(newValue.askForDecryptionKeyDialog).isTrue()
                assertThat(newValue.errorDialogTitle).isEqualTo(-1)
                assertThat(newValue.errorDialogContent).isEqualTo(-1)
            }
        }

    @Test
    fun `test that on login into folder and on result INCORRECT_KEY values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            val decryptionIntroduced = true
            whenever(loginToFolderUseCase(folderLink)).thenReturn(FolderLoginStatus.INCORRECT_KEY)
            underTest.state.test {
                underTest.folderLogin(folderLink, decryptionIntroduced)
                val newValue = expectMostRecentItem()
                assertThat(newValue.isLoginComplete).isFalse()
                assertThat(newValue.isInitialState).isFalse()
                assertThat(newValue.askForDecryptionKeyDialog).isEqualTo(decryptionIntroduced)
                assertThat(newValue.errorDialogTitle).isEqualTo(-1)
                assertThat(newValue.errorDialogContent).isEqualTo(-1)
                assertThat(newValue.snackBarMessage).isEqualTo(-1)
            }
        }

    @Test
    fun `test that on login into folder and on result ERROR values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            whenever(loginToFolderUseCase(folderLink)).thenReturn(FolderLoginStatus.ERROR)
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
        val node1 = mock<TypedFolderNode>()
        val node2 = mock<TypedFolderNode>()
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
        val childNode = mock<TypedFolderNode>()
        val childrenNodes = listOf(childNode)
        val fetchFolderNodeResult = FetchFolderNodesResult(mock(), mock(), childrenNodes)
        val nodeUIItem = NodeUIItem<TypedNode>(childNode, isSelected = false, isInvisible = false)

        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            underTest.onItemLongClick(nodeUIItem)
            val newValue = expectMostRecentItem()
            assertThat(newValue.selectedNodeCount).isGreaterThan(0)
            assertThat(newValue.nodesList[0].isSelected).isTrue()
        }
    }

    @Test
    fun `test that onSelectAllClicked selects all the nodes`() = runTest {
        val base64Handle = "1234"
        val childrenNodes = listOf<TypedFolderNode>(mock(), mock())
        val fetchFolderNodeResult = FetchFolderNodesResult(mock(), mock(), childrenNodes)

        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            underTest.onSelectAllClicked()
            val newValue = expectMostRecentItem()
            assertThat(newValue.selectedNodeCount).isGreaterThan(0)
            newValue.nodesList.forEach {
                assertThat(it.isSelected).isTrue()
            }
        }
    }

    @Test
    fun `test that onClearAllClicked unselects all the nodes`() = runTest {
        val base64Handle = "1234"
        val childrenNodes = listOf<TypedFolderNode>(mock(), mock())
        val fetchFolderNodeResult = FetchFolderNodesResult(mock(), mock(), childrenNodes)

        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            underTest.onSelectAllClicked()
            var newValue = expectMostRecentItem()
            assertThat(newValue.selectedNodeCount).isGreaterThan(0)
            newValue.nodesList.forEach {
                assertThat(it.isSelected).isTrue()
            }
            underTest.clearAllSelection()
            newValue = expectMostRecentItem()
            assertThat(newValue.selectedNodeCount).isEqualTo(0)
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
        val oldChildNode = mock<TypedFileNode>()
        val newChildNode = mock<TypedFileNode>()
        val oldChildrenNodes = listOf(oldChildNode)
        val newChildrenNodes = listOf(newChildNode)
        val fetchFolderNodeResult =
            FetchFolderNodesResult(mock(), oldParentNode, oldChildrenNodes)

        whenever(newParentNode.name).thenReturn(newParentNodeName)
        whenever(newChildNode.id.longValue).thenReturn(newChildHandle)
        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)
        whenever(getFolderParentNodeUseCase(oldParentNode.id)).thenReturn(newParentNode)
        whenever(getFolderLinkChildrenNodesUseCase(newParentNode.id.longValue, null))
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

    @Test
    fun `test that handleIntent returns correct url and folderSubHandle`() = runTest {
        val url = "https://mega.nz/#F!KDZkRKJK!1meD4csdaj7DWjEhJoHaFw!SDQadJqY"
        val folderSubHandle = "SDQadJqY"
        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(Constants.ACTION_OPEN_MEGA_FOLDER_LINK)
        whenever(intent.dataString).thenReturn(url)

        underTest.state.test {
            underTest.handleIntent(intent)
            val newValue = expectMostRecentItem()
            assertThat(newValue.url).isEqualTo(url)
            assertThat(newValue.folderSubHandle).isEqualTo(folderSubHandle)
        }
    }

    @Test
    fun `test that on resetting openFile sets the value as consumed`() = runTest {
        underTest.state.test {
            underTest.resetOpenFile()
            val newValue = expectMostRecentItem()
            assertThat(newValue.openFile).isInstanceOf(consumed<Intent>().javaClass)
        }
    }

    @Test
    fun `test that on resetting downloadNode sets the value as consumed`() = runTest {
        underTest.state.test {
            underTest.resetDownloadNode()
            val newValue = expectMostRecentItem()
            assertThat(newValue.downloadNodes).isInstanceOf(consumed<List<MegaNode>>().javaClass)
        }
    }

    @Test
    fun `test that on resetting selectImportLocation sets the value as consumed`() = runTest {
        underTest.state.test {
            underTest.resetSelectImportLocation()
            val newValue = expectMostRecentItem()
            assertThat(newValue.selectImportLocation).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that handleImportClick triggers selectImportLocation event`() = runTest {
        val base64Handle = "1234"
        val rootNodeName = "RootNode"
        val rootNode = mock<TypedFolderNode>()
        val childNode = mock<TypedFileNode>()
        val childrenNodes = listOf(childNode)
        val node =
            NodeUIItem<TypedNode>(mock<TypedFolderNode>(), isSelected = false, isInvisible = false)
        val fetchFolderNodeResult =
            FetchFolderNodesResult(rootNode, mock(), childrenNodes)

        whenever(rootNode.name).thenReturn(rootNodeName)
        whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            underTest.handleImportClick(node)
            val newValue = expectMostRecentItem()
            assertThat(newValue.selectImportLocation).isEqualTo(triggered)
            assertThat(newValue.importNode).isEqualTo(node)
        }
    }

    @Test
    fun `test that openFile is triggered when updateImageIntent is invoked`() = runTest {
        val intent = mock<Intent>()
        underTest.updateImageIntent(intent)
        underTest.state.test {
            val res = awaitItem()
            assertThat(res.openFile).isInstanceOf(triggered(intent).javaClass)
        }
    }

    @Test
    fun `test that openFile is triggered with correct pdf intent when updatePdfIntent is invoked`() =
        runTest {
            val uriMock = Mockito.mockStatic(Uri::class.java)
            val intent = mock<Intent>()
            val contentUriMock: Uri = mock()
            val path = "/path"
            val handle = 1234L
            val fileNode = mock<FileNode> {
                on { id.longValue }.thenReturn(handle)
            }

            whenever(megaApiFolderHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getLocalFileForNode(any())).thenReturn(null)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(handle)).thenReturn(path)
            whenever(Uri.parse(path)).thenReturn(contentUriMock)

            underTest.updatePdfIntent(intent, fileNode, "pdf")
            underTest.state.test {
                val res = awaitItem()
                assertThat(res.openFile).isInstanceOf(triggered(intent).javaClass)
            }
            uriMock.close()
        }

    @Test
    fun `test that openFile is triggered when updateTextEditorIntent is invoked`() = runTest {
        val intent = mock<Intent>()
        val fileNode = mock<FileNode> {
            on { id.longValue }.thenReturn(1234L)
        }

        underTest.updateTextEditorIntent(intent, fileNode)
        underTest.state.test {
            val res = awaitItem()
            assertThat(res.openFile).isInstanceOf(triggered(intent).javaClass)
        }
    }

    @Test
    fun `test that downloadNodes is triggered when updateNodesToDownload is invoked`() = runTest {
        val nodes: List<MegaNode> = mock()
        val ids = listOf(1234L)

        whenever(getPublicNodeListByIds(ids)).thenReturn(nodes)
        underTest.updateNodesToDownload(ids)
        underTest.state.test {
            val res = awaitItem()
            assertThat(res.downloadNodes).isInstanceOf(triggered(nodes).javaClass)
        }
    }

}
