package mega.privacy.android.app.presentation.folderlink

import android.content.Intent
import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.folderlink.model.LinkErrorState
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeContentUriIntentMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.FolderInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.folderlink.FetchFolderNodesResult
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.StopAudioService
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.advertisements.QueryAdsUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicLinkInformationUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.GetFolderLinkNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.navigation.MegaNavigator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderLinkViewModelTest {

    private lateinit var underTest: FolderLinkViewModel
    private val monitorViewType: MonitorViewType = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val loginToFolderUseCase: LoginToFolderUseCase = mock()
    private val copyNodesUseCase: CopyNodesUseCase = mock()
    private val copyRequestMessageMapper: CopyRequestMessageMapper = mock()
    private val hasCredentialsUseCase: HasCredentialsUseCase = mock()
    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val setViewType: SetViewType = mock()
    private val fetchFolderNodesUseCase: FetchFolderNodesUseCase = mock()
    private val getFolderParentNodeUseCase: GetFolderParentNodeUseCase = mock()
    private val getFolderLinkChildrenNodesUseCase: GetFolderLinkChildrenNodesUseCase = mock()
    private val addNodeType: AddNodeType = mock()
    private val getStringFromStringResMapper: GetStringFromStringResMapper = mock()
    private val areAchievementsEnabledUseCase: AreAchievementsEnabledUseCase = mock()
    private val getAccountTypeUseCase: GetAccountTypeUseCase = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()
    private val getPricing: GetPricing = mock()
    private val containsMediaItemUseCase: ContainsMediaItemUseCase = mock()
    private val getLocalFileForNodeUseCase: GetLocalFileForNodeUseCase = mock()
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase =
        mock()
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase = mock()
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase =
        mock()
    private val httpServerStart: MegaApiHttpServerStartUseCase = mock()
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase = mock()
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase = mock()
    private val getFileUriUseCase: GetFileUriUseCase = mock()
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase = mock()
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase = mock()
    private val getFolderLinkNodeContentUriUseCase: GetFolderLinkNodeContentUriUseCase = mock()
    private val megaNavigator: MegaNavigator = mock()
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper = mock()
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase = mock()
    private val updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase =
        mock()
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase = mock()
    private val stopAudioService: StopAudioService = mock()
    private val getPublicLinkInformationUseCase: GetPublicLinkInformationUseCase = mock()
    private val queryAdsUseCase: QueryAdsUseCase = mock()


    @BeforeEach
    fun setup() {
        resetMocks()
        whenever(monitorViewType.invoke()).thenReturn(emptyFlow())
        initViewModel()
    }

    private fun resetMocks() {
        reset(
            isConnectedToInternetUseCase,
            monitorViewType,
            loginToFolderUseCase,
            copyNodesUseCase,
            copyRequestMessageMapper,
            hasCredentialsUseCase,
            rootNodeExistsUseCase,
            setViewType,
            fetchFolderNodesUseCase,
            getFolderParentNodeUseCase,
            getFolderLinkChildrenNodesUseCase,
            addNodeType,
            getStringFromStringResMapper,
            areAchievementsEnabledUseCase,
            getAccountTypeUseCase,
            getCurrentUserEmail,
            getPricing,
            containsMediaItemUseCase,
            getLocalFileForNodeUseCase,
            getLocalFolderLinkFromMegaApiFolderUseCase,
            megaApiFolderHttpServerStartUseCase,
            megaApiFolderHttpServerIsRunningUseCase,
            httpServerStart,
            httpServerIsRunning,
            getLocalFolderLinkFromMegaApiUseCase,
            getFileUriUseCase,
            mapNodeToPublicLinkUseCase,
            checkNodesNameCollisionUseCase,
            getFolderLinkNodeContentUriUseCase,
            megaNavigator,
            nodeContentUriIntentMapper,
            getNodePreviewFileUseCase,
            updateCrashAndPerformanceReportersUseCase,
            isUserLoggedInUseCase,
            stopAudioService,
            getPublicLinkInformationUseCase,
            queryAdsUseCase
        )
    }

    private fun initViewModel() {
        underTest = FolderLinkViewModel(
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            monitorViewType = monitorViewType,
            loginToFolderUseCase = loginToFolderUseCase,
            copyNodesUseCase = copyNodesUseCase,
            copyRequestMessageMapper = copyRequestMessageMapper,
            hasCredentialsUseCase = hasCredentialsUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            setViewType = setViewType,
            fetchFolderNodesUseCase = fetchFolderNodesUseCase,
            getFolderParentNodeUseCase = getFolderParentNodeUseCase,
            getFolderLinkChildrenNodesUseCase = getFolderLinkChildrenNodesUseCase,
            addNodeType = addNodeType,
            getStringFromStringResMapper = getStringFromStringResMapper,
            areAchievementsEnabledUseCase = areAchievementsEnabledUseCase,
            getAccountTypeUseCase = getAccountTypeUseCase,
            getCurrentUserEmail = getCurrentUserEmail,
            getPricing = getPricing,
            containsMediaItemUseCase = containsMediaItemUseCase,
            getLocalFileForNodeUseCase = getLocalFileForNodeUseCase,
            getLocalFolderLinkFromMegaApiFolderUseCase = getLocalFolderLinkFromMegaApiFolderUseCase,
            megaApiFolderHttpServerStartUseCase = megaApiFolderHttpServerStartUseCase,
            megaApiFolderHttpServerIsRunningUseCase = megaApiFolderHttpServerIsRunningUseCase,
            httpServerStart = httpServerStart,
            httpServerIsRunning = httpServerIsRunning,
            getLocalFolderLinkFromMegaApiUseCase = getLocalFolderLinkFromMegaApiUseCase,
            getFileUriUseCase = getFileUriUseCase,
            mapNodeToPublicLinkUseCase = mapNodeToPublicLinkUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            getFolderLinkNodeContentUriUseCase = getFolderLinkNodeContentUriUseCase,
            megaNavigator = megaNavigator,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            updateCrashAndPerformanceReportersUseCase = updateCrashAndPerformanceReportersUseCase,
            isUserLoggedInUseCase = isUserLoggedInUseCase,
            stopAudioService = stopAudioService,
            applicationScope = CoroutineScope(UnconfinedTestDispatcher()),
            monitorMiscLoadedUseCase = mock(),
            getPublicLinkInformationUseCase = getPublicLinkInformationUseCase,
            queryAdsUseCase = queryAdsUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.isInitialState).isEqualTo(true)
            assertThat(initial.isLoginComplete).isEqualTo(false)
            assertThat(initial.isNodesFetched).isEqualTo(false)
            assertThat(initial.askForDecryptionKeyDialogEvent).isEqualTo(consumed)
            assertThat(initial.collisionsEvent).isEqualTo(consumed())
            assertThat(initial.copyResultEvent).isEqualTo(consumed())
            assertThat(initial.hasDbCredentials).isFalse()
            assertThat(initial.nodesList).isEmpty()
            assertThat(initial.rootNode).isNull()
            assertThat(initial.parentNode).isNull()
            assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initial.title).isEqualTo("")
            assertThat(initial.selectedNodeCount).isEqualTo(0)
            assertThat(initial.finishActivityEvent).isEqualTo(consumed)
            assertThat(initial.importNode).isNull()
            assertThat(initial.openFile).isInstanceOf(consumed().javaClass)
            assertThat(initial.openFileNodeEvent).isEqualTo(consumed())
            assertThat(initial.selectImportLocation).isEqualTo(consumed)
            assertThat(initial.errorState).isEqualTo(LinkErrorState.NoError)
            assertThat(initial.snackBarMessage).isEqualTo(-1)
        }
    }

    @Test
    fun `test that on login into folder and on result OK values are updated correctly`() = runTest {
        val folderLink = "abcd"
        val childNode = mock<TypedFolderNode>()
        val childrenNodes = listOf(childNode)
        val fetchFolderNodeResult = FetchFolderNodesResult(mock(), mock(), childrenNodes)
        whenever(loginToFolderUseCase(folderLink)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull())).thenReturn(fetchFolderNodeResult)
        val folderInfo = mock<FolderInfo> {
            on { id }.thenReturn(NodeId(1234L))
        }
        whenever(getPublicLinkInformationUseCase(folderLink)).thenReturn(folderInfo)
        whenever(queryAdsUseCase(folderInfo.id.longValue)).thenReturn(false)
        underTest.state.test {
            underTest.folderLogin(folderLink)
            val newValue = expectMostRecentItem()
            assertThat(newValue.isLoginComplete).isTrue()
            assertThat(newValue.isInitialState).isFalse()
            assertThat(newValue.errorState).isEqualTo(LinkErrorState.NoError)
        }
    }

    @Test
    fun `test that errorState is updated correctly when link is expired`() = runTest {
        val base64Handle = "1234"
        whenever(fetchFolderNodesUseCase(base64Handle)).thenThrow(
            FetchFolderNodesException.Expired()
        )

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            val newValue = expectMostRecentItem()
            assertThat(newValue.isNodesFetched).isTrue()
            assertThat(newValue.errorState).isEqualTo(LinkErrorState.Expired)
        }
    }

    @Test
    fun `test that errorState is updated correctly when link is unavailable`() = runTest {
        val base64Handle = "1234"
        whenever(fetchFolderNodesUseCase(base64Handle)).thenThrow(
            FetchFolderNodesException.LinkRemoved()
        )

        underTest.state.test {
            underTest.fetchNodes(base64Handle)
            val newValue = expectMostRecentItem()
            assertThat(newValue.isNodesFetched).isTrue()
            assertThat(newValue.errorState).isEqualTo(LinkErrorState.Unavailable)
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
                assertThat(newValue.askForDecryptionKeyDialogEvent).isEqualTo(triggered)
                assertThat(newValue.errorState).isEqualTo(LinkErrorState.NoError)
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
                assertThat(newValue.askForDecryptionKeyDialogEvent).isEqualTo(triggered)
                assertThat(newValue.errorState).isEqualTo(LinkErrorState.NoError)
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
                assertThat(newValue.askForDecryptionKeyDialogEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that error state is set when folder login throws exception`() =
        runTest {
            val folderLink = "abcd"
            whenever(loginToFolderUseCase(folderLink)).thenThrow(IllegalArgumentException())
            underTest.state.test {
                underTest.folderLogin(folderLink)
                val newValue = expectMostRecentItem()
                assertThat(newValue.errorState).isEqualTo(LinkErrorState.Unavailable)
            }
        }

    @Test
    fun `test that on valid credentials and no root node shouldShowLogin is returned true`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(rootNodeExistsUseCase()).thenReturn(false)
            underTest.state.test {
                underTest.checkLoginRequired()
                val value = expectMostRecentItem()
                assertThat(value.showLoginEvent).isEqualTo(triggered)
                assertThat(value.hasDbCredentials).isTrue()
            }
        }

    @Test
    fun `test that on valid credentials and root node shouldShowLogin is returned false`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(rootNodeExistsUseCase()).thenReturn(true)
            underTest.state.test {
                underTest.checkLoginRequired()
                val value = expectMostRecentItem()
                assertThat(value.showLoginEvent).isEqualTo(consumed)
                assertThat(value.hasDbCredentials).isTrue()
            }
        }

    @Test
    fun `test that launchCollisionActivity values are reset `() = runTest {
        underTest.state.test {
            underTest.resetLaunchCollisionActivity()
            val newValue = expectMostRecentItem()
            assertThat(newValue.collisionsEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that showCopyResult values are reset `() = runTest {
        underTest.state.test {
            underTest.resetShowCopyResult()
            val newValue = expectMostRecentItem()
            assertThat(newValue.copyResultEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that askForDecryptionKeyDialog values are reset `() = runTest {
        underTest.state.test {
            underTest.resetAskForDecryptionKeyDialog()
            val newValue = expectMostRecentItem()
            assertThat(newValue.askForDecryptionKeyDialogEvent).isEqualTo(consumed)
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
            assertThat(newValue.openFile).isInstanceOf(consumed().javaClass)
        }
    }

    @Test
    fun `test that on resetting downloadNode sets the value as consumed`() = runTest {
        underTest.state.test {
            underTest.resetDownloadNode()
            val newValue = expectMostRecentItem()
            assertThat(newValue.downloadEvent).isInstanceOf(consumed().javaClass)
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
            whenever(getLocalFileForNodeUseCase(any())).thenReturn(null)
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
    fun `test that node handle is extracted from importNode when multiple node is not selected`() =
        runTest {
            val base64Handle = "1234"
            val rootNodeName = "RootNode"
            val rootNode = mock<TypedFolderNode>()
            val importNode = mock<TypedFolderNode> {
                on { id.longValue }.thenReturn(1111L)
            }
            val parentNode = mock<TypedFolderNode>()
            val node1 = mock<TypedFolderNode>()
            val node2 = mock<TypedFolderNode>()
            val childrenNodes = listOf(node1, node2)
            val fetchFolderNodeResult = FetchFolderNodesResult(rootNode, parentNode, childrenNodes)

            whenever(rootNode.name).thenReturn(rootNodeName)
            whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)
            whenever(checkNodesNameCollisionUseCase(any(), any())).thenReturn(mock())
            underTest.state.test {
                underTest.fetchNodes(base64Handle)
                underTest.handleImportClick(
                    NodeUIItem(
                        node = importNode,
                        isSelected = true
                    )
                )

                underTest.importNodes(3333L)

                assertThat(expectMostRecentItem().importNode).isNull()
                verify(checkNodesNameCollisionUseCase).invoke(
                    nodes = mapOf(1111L to 3333L),
                    type = NodeNameCollisionType.COPY
                )
                initViewModel()
            }
        }

    @Test
    fun `test that node handle is extracted from rootNode when multiple node is not selected and importNode is null`() =
        runTest {
            val base64Handle = "1234"
            val rootNodeName = "RootNode"
            val rootNode = mock<TypedFolderNode> {
                on { id.longValue }.thenReturn(1111L)
            }
            val parentNode = mock<TypedFolderNode>()
            val node1 = mock<TypedFolderNode>()
            val node2 = mock<TypedFolderNode>()
            val childrenNodes = listOf(node1, node2)
            val fetchFolderNodeResult = FetchFolderNodesResult(rootNode, parentNode, childrenNodes)

            whenever(rootNode.name).thenReturn(rootNodeName)
            whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)
            whenever(checkNodesNameCollisionUseCase(any(), any())).thenReturn(mock())
            underTest.state.test {
                underTest.fetchNodes(base64Handle)
                underTest.importNodes(3333L)

                assertThat(expectMostRecentItem().importNode).isNull()
                verify(checkNodesNameCollisionUseCase).invoke(
                    nodes = mapOf(1111L to 3333L),
                    type = NodeNameCollisionType.COPY
                )
                initViewModel()
            }
        }

    @Test
    fun `test that node handle is selected nodes when multiple nodes are selected`() =
        runTest {
            val base64Handle = "1234"
            val rootNodeName = "RootNode"
            val rootNode = mock<TypedFolderNode>()
            val parentNode = mock<TypedFolderNode>()
            val node1 = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(1111L))
            }
            val node2 = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(2222L))
            }
            val childrenNodes = listOf(node1, node2)
            val fetchFolderNodeResult = FetchFolderNodesResult(rootNode, parentNode, childrenNodes)

            whenever(rootNode.name).thenReturn(rootNodeName)
            whenever(fetchFolderNodesUseCase(base64Handle)).thenReturn(fetchFolderNodeResult)
            whenever(checkNodesNameCollisionUseCase(any(), any())).thenReturn(mock())
            underTest.state.test {
                underTest.fetchNodes(base64Handle)
                underTest.onSelectAllClicked()

                underTest.importNodes(3333L)
                cancelAndConsumeRemainingEvents()

                verify(checkNodesNameCollisionUseCase).invoke(
                    nodes = mapOf(1111L to 3333L, 2222L to 3333L),
                    type = NodeNameCollisionType.COPY
                )
                initViewModel()
            }
        }

    @Test
    fun `test that non-conflict nodes are copied when checkNameCollision is invoked`() = runTest {
        val nodeMap = mapOf(Pair(1234L, 2345L), Pair(234L, 2345L))
        whenever(checkNodesNameCollisionUseCase(nodeMap, NodeNameCollisionType.COPY)).thenReturn(
            NodeNameCollisionsResult(
                noConflictNodes = nodeMap,
                conflictNodes = emptyMap(),
                type = NodeNameCollisionType.COPY
            )
        )
        whenever(copyNodesUseCase(nodeMap)).thenReturn(mock<MoveRequestResult.Copy>())

        underTest.checkNameCollision(listOf(1234L, 234L), 2345L)

        verify(copyNodesUseCase).invoke(nodeMap)
    }

    @Test
    fun `test that downloadEvent is triggered when updateNodeToPreview is invoked`() =
        runTest {
            val node = mock<TypedFileNode>()
            val link = mock<PublicLinkFile>()
            whenever(mapNodeToPublicLinkUseCase(node, null)).thenReturn(link)
            whenever(getNodePreviewFileUseCase(node)).thenReturn(null)
            underTest.openOtherTypeFile(mock(), node)
            underTest.state.test {
                val res = awaitItem()
                assertThat(res.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((res.downloadEvent as StateEventWithContentTriggered).content.nodes)
                    .containsExactly(link)
            }
        }

    @Test
    fun `test that getNodeContentUri returns as expected`() = runTest {
        val expectedNodeContentUri = mock<NodeContentUri.RemoteContentUri>()

        whenever(getFolderLinkNodeContentUriUseCase(anyOrNull())).thenReturn(expectedNodeContentUri)
        assertThat(underTest.getNodeContentUri(mock())).isEqualTo(expectedNodeContentUri)
    }

    @Test
    fun `test that stopAudioService is invoked when the user is not logged in`() = runTest {
        whenever(isUserLoggedInUseCase()).thenReturn(false)
        underTest.stopAudioPlayerServiceWithoutLogin()
        verify(stopAudioService).invoke()
    }

    @Test
    fun `test that stopAudioService is not invoked when the user is logged in`() = runTest {
        whenever(isUserLoggedInUseCase()).thenReturn(true)
        underTest.stopAudioPlayerServiceWithoutLogin()
        verify(stopAudioService, times(0)).invoke()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that show ads for link is returned correctly`(shouldShowAdsForLink: Boolean) =
        runTest {
            val folderLink = "abcd"
            val childNode = mock<TypedFolderNode>()
            val childrenNodes = listOf(childNode)
            val fetchFolderNodeResult = FetchFolderNodesResult(mock(), mock(), childrenNodes)
            whenever(loginToFolderUseCase(folderLink)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(anyOrNull())).thenReturn(fetchFolderNodeResult)
            val folderInfo = mock<FolderInfo> {
                on { id }.thenReturn(NodeId(1234L))
            }
            whenever(getPublicLinkInformationUseCase(folderLink)).thenReturn(folderInfo)
            whenever(queryAdsUseCase(folderInfo.id.longValue)).thenReturn(shouldShowAdsForLink)
            underTest.folderLogin(folderLink)
            underTest.state.test {
                val newValue = expectMostRecentItem()
                assertThat(newValue.shouldShowAdsForLink).isEqualTo(shouldShowAdsForLink)
            }
        }

    @Test
    fun `test that openFile triggers openFileNodeEvent`() = runTest {
        val nodeUIItem = mock<NodeUIItem<TypedNode>>()

        underTest.openFile(nodeUIItem)

        underTest.state.test {
            val result = awaitItem()
            assertThat(result.openFileNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((result.openFileNodeEvent as StateEventWithContentTriggered).content).isEqualTo(
                nodeUIItem
            )
        }
    }

    @Test
    fun `test that openFile with different nodeUIItems triggers different events`() = runTest {
        val nodeUIItem1 = mock<NodeUIItem<TypedNode>>()
        val nodeUIItem2 = mock<NodeUIItem<TypedNode>>()

        underTest.state.test {
            underTest.openFile(nodeUIItem1)
            val result1 = expectMostRecentItem()
            assertThat((result1.openFileNodeEvent as StateEventWithContentTriggered).content).isEqualTo(
                nodeUIItem1
            )

            underTest.resetOpenFileNodeEvent()
            val resetResult = expectMostRecentItem()
            assertThat(resetResult.openFileNodeEvent).isEqualTo(consumed())

            underTest.openFile(nodeUIItem2)
            val result2 = expectMostRecentItem()
            assertThat((result2.openFileNodeEvent as StateEventWithContentTriggered).content).isEqualTo(
                nodeUIItem2
            )

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that resetOpenFileNodeEvent consumes the event`() = runTest {
        val nodeUIItem = mock<NodeUIItem<TypedNode>>()

        underTest.openFile(nodeUIItem)
        underTest.resetOpenFileNodeEvent()

        underTest.state.test {
            val result = awaitItem()
            assertThat(result.openFileNodeEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that fetchNodes with subHandle does not trigger openFileNodeEvent when folderSubHandle not set`() =
        runTest {
            val subHandle = "testSubHandle"
            val imageFileNode = mock<TypedFileNode> {
                on { type }.thenReturn(
                    StaticImageFileTypeInfo(
                        mimeType = "image/jpeg",
                        extension = "jpg"
                    )
                )
            }

            val fetchFolderNodeResult = FetchFolderNodesResult(
                rootNode = mock(),
                parentNode = mock(),
                childrenNodes = listOf(imageFileNode)
            )

            whenever(fetchFolderNodesUseCase(subHandle)).thenReturn(fetchFolderNodeResult)

            underTest.state.test {
                underTest.fetchNodes(subHandle)

                val finalState = expectMostRecentItem()
                assertThat(finalState.openFileNodeEvent).isEqualTo(consumed())

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that fetchNodes with subHandle does not trigger openFileNodeEvent for video files when folderSubHandle not set`() =
        runTest {
            val subHandle = "testSubHandle"
            val videoFileNode = mock<TypedFileNode> {
                on { type }.thenReturn(
                    VideoFileTypeInfo(
                        extension = "mp4",
                        mimeType = "video",
                        duration = Duration.INFINITE
                    )
                )
            }

            val fetchFolderNodeResult = FetchFolderNodesResult(
                rootNode = mock(),
                parentNode = mock(),
                childrenNodes = listOf(videoFileNode)
            )

            whenever(fetchFolderNodesUseCase(subHandle)).thenReturn(fetchFolderNodeResult)

            underTest.state.test {
                underTest.fetchNodes(subHandle)

                val finalState = expectMostRecentItem()
                assertThat(finalState.openFileNodeEvent).isEqualTo(consumed())

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that openFile method works correctly with NodeUIItem`() = runTest {
        val imageFileNode = mock<TypedFileNode> {
            on { type }.thenReturn(
                StaticImageFileTypeInfo(
                    mimeType = "image/jpeg",
                    extension = "jpg"
                )
            )
        }
        val nodeUIItem = NodeUIItem<TypedNode>(
            node = imageFileNode,
            isSelected = false,
            isInvisible = false
        )

        underTest.state.test {
            underTest.openFile(nodeUIItem)

            val finalState = expectMostRecentItem()
            assertThat(finalState.openFileNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            val triggeredEvent = finalState.openFileNodeEvent as StateEventWithContentTriggered
            assertThat(triggeredEvent.content).isEqualTo(nodeUIItem)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that fetchNodes with subHandle does not trigger openFileNodeEvent for non-media files`() =
        runTest {
            val subHandle = "testSubHandle"
            val textFileNode = mock<TypedFileNode> {
                on { type }.thenReturn(mock<TextFileTypeInfo>())
            }

            val fetchFolderNodeResult = FetchFolderNodesResult(
                rootNode = mock(),
                parentNode = mock(),
                childrenNodes = listOf(textFileNode)
            )

            whenever(fetchFolderNodesUseCase(subHandle)).thenReturn(fetchFolderNodeResult)

            underTest.state.test {
                underTest.fetchNodes(subHandle)

                val finalState = expectMostRecentItem()
                assertThat(finalState.openFileNodeEvent).isEqualTo(consumed())

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that fetchNodes with subHandle does not trigger openFileNodeEvent for folders`() =
        runTest {
            val subHandle = "testSubHandle"
            val folderNode = mock<TypedFolderNode>()

            val fetchFolderNodeResult = FetchFolderNodesResult(
                rootNode = mock(),
                parentNode = mock(),
                childrenNodes = listOf(folderNode)
            )

            whenever(fetchFolderNodesUseCase(subHandle)).thenReturn(fetchFolderNodeResult)

            underTest.state.test {
                underTest.fetchNodes(subHandle)

                val finalState = expectMostRecentItem()
                assertThat(finalState.openFileNodeEvent).isEqualTo(consumed())

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that fetchNodes with null subHandle does not trigger openFileNodeEvent`() = runTest {
        val fetchFolderNodeResult = FetchFolderNodesResult(
            rootNode = mock(),
            parentNode = mock(),
            childrenNodes = listOf(mock<TypedFileNode>())
        )

        whenever(fetchFolderNodesUseCase(null)).thenReturn(fetchFolderNodeResult)

        underTest.state.test {
            underTest.fetchNodes(null)

            val finalState = expectMostRecentItem()
            assertThat(finalState.openFileNodeEvent).isEqualTo(consumed())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that fetchNodes with subHandle does not trigger openFileNodeEvent when no media files found`() =
        runTest {
            val subHandle = "testSubHandle"
            val nonMediaFileNode = mock<TypedFileNode> {
                on { type }.thenReturn(mock<TextFileTypeInfo>())
            }

            val fetchFolderNodeResult = FetchFolderNodesResult(
                rootNode = mock(),
                parentNode = mock(),
                childrenNodes = listOf(nonMediaFileNode)
            )

            whenever(fetchFolderNodesUseCase(subHandle)).thenReturn(fetchFolderNodeResult)

            underTest.state.test {
                underTest.fetchNodes(subHandle)

                // Get the final state after fetchNodes completes
                val finalState = expectMostRecentItem()
                assertThat(finalState.openFileNodeEvent).isEqualTo(consumed())

                // Consume any remaining events
                cancelAndConsumeRemainingEvents()
            }
        }
}