package mega.privacy.android.feature.texteditor.presentation

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.chat.SendToChatResult
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.usecase.continuewhereleftoff.GetTextEditorScrollUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SaveRecentlyUsedItemIfQualifiesUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SaveRecentlyUsedItemUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SaveTextEditorScrollUseCase
import mega.privacy.android.domain.usecase.filenode.GetNodeVersionsByHandleUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapTypedNodeToPublicLinkUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForFileLinkUseCase
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForFolderLinkUseCase
import mega.privacy.android.domain.usecase.texteditor.GetShowLineNumbersPreferenceUseCase
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForTextEditorUseCase
import mega.privacy.android.domain.usecase.texteditor.SaveTextContentForTextEditorUseCase
import mega.privacy.android.domain.usecase.texteditor.SetShowLineNumbersPreferenceUseCase
import mega.privacy.android.feature.texteditor.presentation.TextEditorComposeViewModel.Args
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorNodeEffect
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorComposeViewModelTest {

    private val getTextContentForTextEditorUseCase: GetTextContentForTextEditorUseCase = mock()
    private val getTextContentForFileLinkUseCase: GetTextContentForFileLinkUseCase = mock()
    private val getTextContentForFolderLinkUseCase: GetTextContentForFolderLinkUseCase = mock()
    private val getPublicChildNodeFromIdUseCase: GetPublicChildNodeFromIdUseCase = mock()
    private val saveTextContentForTextEditorUseCase: SaveTextContentForTextEditorUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getNodeAccessUseCase: GetNodeAccessUseCase = mock()
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase = mock()
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase = mock()
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase = mock()
    private val exportNodeUseCase: ExportNodeUseCase = mock()
    private val getChatFileUseCase: GetChatFileUseCase = mock()
    private val getPublicNodeUseCase: GetPublicNodeUseCase = mock()
    private val mapTypedNodeToPublicLinkUseCase: MapTypedNodeToPublicLinkUseCase = mock()
    private val getShowLineNumbersPreferenceUseCase: GetShowLineNumbersPreferenceUseCase = mock()
    private val setShowLineNumbersPreferenceUseCase: SetShowLineNumbersPreferenceUseCase = mock()
    private val saveTextEditorScrollUseCase: SaveTextEditorScrollUseCase = mock()
    private val getTextEditorScrollUseCase: GetTextEditorScrollUseCase = mock()
    private val saveRecentlyUsedItemUseCase: SaveRecentlyUsedItemUseCase = mock()
    private val saveRecentlyUsedItemIfQualifiesUseCase:
            SaveRecentlyUsedItemIfQualifiesUseCase = mock()
    private val removeRecentlyUsedItemUseCase: RemoveRecentlyUsedItemUseCase = mock()
    private val getNodeVersionsByHandleUseCase: GetNodeVersionsByHandleUseCase = mock()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val snackbarEventQueue: SnackbarEventQueue = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val textEditorBottomBarActionsMapper: TextEditorBottomBarActionsMapper =
        TextEditorBottomBarActionsMapper()
    private val fileLinkPublicNode: TypedFileNode = mock {
        whenever(it.id).thenReturn(NodeId(999L))
        whenever(it.name).thenReturn("public.txt")
    }

    private val folderLinkNode: TypedFileNode = mock {
        whenever(it.id).thenReturn(NodeId(FOLDER_LINK_HANDLE))
        whenever(it.name).thenReturn("folder-link.txt")
    }

    private lateinit var underTest: TextEditorComposeViewModel

    private companion object {
        const val FILE_LINK_URL = "https://mega.nz/file/abc"
        const val FOLDER_LINK_HANDLE = 555L
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getTextContentForTextEditorUseCase,
            getTextContentForFileLinkUseCase,
            getTextContentForFolderLinkUseCase,
            getPublicChildNodeFromIdUseCase,
            saveTextContentForTextEditorUseCase,
            getNodeByIdUseCase,
            getNodeAccessUseCase,
            isNodeInBackupsUseCase,
            attachMultipleNodesUseCase,
            get1On1ChatIdUseCase,
            exportNodeUseCase,
            getChatFileUseCase,
            getPublicNodeUseCase,
            mapTypedNodeToPublicLinkUseCase,
            getShowLineNumbersPreferenceUseCase,
            setShowLineNumbersPreferenceUseCase,
            saveTextEditorScrollUseCase,
            getTextEditorScrollUseCase,
            saveRecentlyUsedItemUseCase,
            saveRecentlyUsedItemIfQualifiesUseCase,
            removeRecentlyUsedItemUseCase,
            getNodeVersionsByHandleUseCase,
            monitorNodeUpdatesUseCase,
            monitorConnectivityUseCase,
            isConnectedToInternetUseCase,
            snackbarEventQueue,
            getFeatureFlagValueUseCase,
        )
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
            whenever(getNodeVersionsByHandleUseCase(any())).thenReturn(emptyList())
        }
        whenever(monitorNodeUpdatesUseCase()).thenReturn(flowOf())
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
    }

    private fun initUnderTest(
        nodeHandle: Long = 0L,
        mode: TextEditorMode = TextEditorMode.View,
        fileName: String? = null,
        inExcludedAdapterForGetLinkAndEdit: Boolean = false,
        showDownload: Boolean = true,
        showShare: Boolean = true,
        showSendToChat: Boolean = false,
        isFromSharedFolder: Boolean = false,
        fromHome: Boolean = false,
        chatId: Long? = null,
        messageId: Long? = null,
        localPath: String? = null,
        publicUrl: String? = null,
        isFolderLink: Boolean = false,
        defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    ) {
        underTest = TextEditorComposeViewModel(
            args = Args(
                nodeHandle = nodeHandle,
                mode = mode,
                fileName = fileName,
                inExcludedAdapterForGetLinkAndEdit = inExcludedAdapterForGetLinkAndEdit,
                showDownload = showDownload,
                showShare = showShare,
                showSendToChat = showSendToChat,
                isFromSharedFolder = isFromSharedFolder,
                fromHome = fromHome,
                chatId = chatId,
                messageId = messageId,
                localPath = localPath,
                publicUrl = publicUrl,
                isFolderLink = isFolderLink,
            ),
            defaultDispatcher = defaultDispatcher,
            getTextContentForTextEditorUseCase = getTextContentForTextEditorUseCase,
            getTextContentForFileLinkUseCase = getTextContentForFileLinkUseCase,
            getTextContentForFolderLinkUseCase = getTextContentForFolderLinkUseCase,
            getPublicChildNodeFromIdUseCase = getPublicChildNodeFromIdUseCase,
            saveTextContentForTextEditorUseCase = saveTextContentForTextEditorUseCase,
            getShowLineNumbersPreferenceUseCase = getShowLineNumbersPreferenceUseCase,
            setShowLineNumbersPreferenceUseCase = setShowLineNumbersPreferenceUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getNodeAccessUseCase = getNodeAccessUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            textEditorBottomBarActionsMapper = textEditorBottomBarActionsMapper,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            exportNodeUseCase = exportNodeUseCase,
            getChatFileUseCase = getChatFileUseCase,
            getPublicNodeUseCase = getPublicNodeUseCase,
            mapTypedNodeToPublicLinkUseCase = mapTypedNodeToPublicLinkUseCase,
            saveTextEditorScrollUseCase = saveTextEditorScrollUseCase,
            getTextEditorScrollUseCase = getTextEditorScrollUseCase,
            saveRecentlyUsedItemUseCase = saveRecentlyUsedItemUseCase,
            saveRecentlyUsedItemIfQualifiesUseCase = saveRecentlyUsedItemIfQualifiesUseCase,
            removeRecentlyUsedItemUseCase = removeRecentlyUsedItemUseCase,
            getNodeVersionsByHandleUseCase = getNodeVersionsByHandleUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            snackbarEventQueue = snackbarEventQueue,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @Test
    fun `test that initial uiState reflects Args`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "notes.txt",
        )
        advanceUntilIdle()
        val state = underTest.uiState.value
        assertThat(state.fileName).isEqualTo("notes.txt")
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `test that isMarkdown is true when flag enabled and file is md`() = runTest {
        stubEmptyLoad()
        whenever(getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering))
            .thenReturn(true)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "README.md")
        advanceUntilIdle()
        val state = underTest.uiState.value
        assertThat(state.isMarkdownEnabled).isTrue()
        assertThat(state.isMarkdown).isTrue()
    }

    @Test
    fun `test that isMarkdown is false when flag disabled`() = runTest {
        stubEmptyLoad()
        whenever(getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering))
            .thenReturn(false)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "README.md")
        advanceUntilIdle()
        assertThat(underTest.uiState.value.isMarkdown).isFalse()
    }

    @Test
    fun `test that isMarkdown is false for non markdown file even when flag enabled`() = runTest {
        stubEmptyLoad()
        whenever(getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering))
            .thenReturn(true)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "notes.txt")
        advanceUntilIdle()
        assertThat(underTest.uiState.value.isMarkdown).isFalse()
    }

    @Test
    fun `test that getMarkdownPreviewContent returns joined content for normal lines`() = runTest {
        doReturn(flowOf(listOf("# Title", "body"))).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "README.md")
        advanceUntilIdle()
        assertThat(underTest.getMarkdownPreviewContent()).isEqualTo("# Title\nbody")
    }

    private fun stubEmptyLoad() {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
    }

    @Test
    fun `test that uiState with Edit mode reflects Args`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.Edit,
            fileName = "notes.txt",
        )
        val state = underTest.uiState.value
        assertThat(state.fileName).isEqualTo("notes.txt")
        assertThat(state.mode).isEqualTo(TextEditorMode.Edit)
    }

    @Test
    fun `test that Args with View mode sets mode to View`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "readme.txt",
        )
        assertThat(underTest.uiState.value.mode).isEqualTo(TextEditorMode.View)
    }

    @Test
    fun `test that Args with Create mode sets mode to Create`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.uiState.value.mode).isEqualTo(TextEditorMode.Create)
    }

    @Test
    fun `test that null fileName results in empty string in uiState`() {
        initUnderTest(fileName = null)
        assertThat(underTest.uiState.value.fileName).isEmpty()
    }

    @Test
    fun `test that fileName is set from node when opening file in View mode`() = runTest {
        val node = mock<TypedNode>()
        whenever(node.name).thenReturn("fetched-name.txt")
        whenever(node.exportedData).thenReturn(null)
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(node)
            whenever(getNodeAccessUseCase(any())).thenReturn(AccessPermission.OWNER)
        }
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "caller-given.txt",
        )
        advanceUntilIdle()
        assertThat(underTest.uiState.value.fileName).isEqualTo("fetched-name.txt")
    }

    @Test
    fun `test that Create mode keeps fileName from Args`() {
        initUnderTest(mode = TextEditorMode.Create, fileName = "newfile.txt")
        assertThat(underTest.uiState.value.fileName).isEqualTo("newfile.txt")
    }

    @Test
    fun `test that Create mode is not loading`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `test that onMenuAction Download emits StartDownloadNode transferEvent when node exists`() = runTest {
        val node = mock<TypedNode>()
        runBlocking { whenever(getNodeByIdUseCase(NodeId(5L))).thenReturn(node) }
        initUnderTest(nodeHandle = 5L)
        advanceUntilIdle()

        underTest.onMenuAction(TextEditorTopBarAction.Download)
        advanceUntilIdle()

        val event = underTest.uiState.value.transferEvent
        check(event is StateEventWithContentTriggered<*>)
        val content = event.content
        assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
        assertThat((content as TransferTriggerEvent.StartDownloadNode).nodes).containsExactly(node)
    }

    @Test
    fun `test that onMenuAction Download does not emit transferEvent when node is null`() = runTest {
        runBlocking { whenever(getNodeByIdUseCase(any())).thenReturn(null) }
        initUnderTest(nodeHandle = 5L)
        advanceUntilIdle()

        underTest.onMenuAction(TextEditorTopBarAction.Download)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.transferEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that onMenuAction LineNumbers toggles showLineNumbers`() = runTest {
        initUnderTest()
        advanceUntilIdle()
        assertThat(underTest.uiState.value.showLineNumbers).isFalse()

        underTest.onMenuAction(TextEditorTopBarAction.LineNumbers)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.showLineNumbers).isTrue()

        underTest.onMenuAction(TextEditorTopBarAction.LineNumbers)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.showLineNumbers).isFalse()
    }

    @Test
    fun `test that onMenuAction LineNumbers persists preference`() = runTest {
        initUnderTest()
        advanceUntilIdle()

        underTest.onMenuAction(TextEditorTopBarAction.LineNumbers)
        advanceUntilIdle()

        verify(setShowLineNumbersPreferenceUseCase).invoke(true)
    }

    @Test
    fun `test that showLineNumbers restores persisted preference on init`() = runTest {
        whenever(getShowLineNumbersPreferenceUseCase()).thenReturn(true)
        initUnderTest()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.showLineNumbers).isTrue()
    }

    @Test
    fun `test that onMenuAction SendToChat triggers SendToChat node effect`() {
        initUnderTest(nodeHandle = 12L)
        underTest.onMenuAction(TextEditorTopBarAction.SendToChat)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(TextEditorNodeEffect.SendToChat(12L))
    }


    @Test
    fun `test that onMenuAction GetLink triggers ManageLink node effect`() {
        initUnderTest(nodeHandle = 9L)
        underTest.onMenuAction(TextEditorTopBarAction.GetLink)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(TextEditorNodeEffect.ManageLink(9L))
    }

    @Test
    fun `test that onMenuAction Share triggers Share node effect`() {
        initUnderTest(nodeHandle = 3L, fileName = "doc.txt")
        underTest.onMenuAction(TextEditorTopBarAction.Share)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(
            TextEditorNodeEffect.Share(
                nodeHandle = 3L,
                localPath = null,
                fileName = "doc.txt",
            ),
        )
    }

    @Test
    fun `test that onMenuAction Share triggers Share with localPath for offline file`() {
        initUnderTest(
            nodeHandle = -1L,
            fileName = "offline.txt",
            localPath = "/data/offline/offline.txt",
        )
        underTest.onMenuAction(TextEditorTopBarAction.Share)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(
            TextEditorNodeEffect.Share(
                nodeHandle = -1L,
                localPath = "/data/offline/offline.txt",
                fileName = "offline.txt",
            ),
        )
    }

    @Test
    fun `test that isContentDirty returns true when chunk state is edited`() = runTest {
        val lines = (1..100).map { "line$it" }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "modified content") }

        assertThat(underTest.isContentDirty()).isTrue()
    }

    @Test
    fun `test that isContentDirty returns false when no chunks are edited`() = runTest {
        val lines = (1..100).map { "line$it" }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.getOrCreateChunkState(0)
        assertThat(underTest.isContentDirty()).isFalse()
    }

    @Test
    fun `test that isContentDirty returns false in Create mode with no edits`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.getOrCreateChunkState(0)
        assertThat(underTest.isContentDirty()).isFalse()
    }

    @Test
    fun `test that confirmDiscard in Create mode closes dialog and emits exitAfterCreateDiscardEvent`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.requestShowDiscardDialog()
        assertThat(underTest.uiState.value.showDiscardDialog).isTrue()

        underTest.confirmDiscard()

        val state = underTest.uiState.value
        assertThat(state.showDiscardDialog).isFalse()
        assertThat(state.exitAfterCreateDiscardEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that consumeExitAfterCreateDiscardEvent resets exitAfterCreateDiscardEvent`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.confirmDiscard()
        assertThat(underTest.uiState.value.exitAfterCreateDiscardEvent).isEqualTo(triggered)

        underTest.consumeExitAfterCreateDiscardEvent()
        assertThat(underTest.uiState.value.exitAfterCreateDiscardEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that saveFile forwards fromHome from Args to save use case`() = runTest {
        val saveResult = TextEditorSaveResult.UploadRequired(
            tempPath = "/tmp/new.txt",
            parentHandle = 1L,
            isEditMode = false,
            fromHome = true,
        )
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(saveResult)
        initUnderTest(mode = TextEditorMode.Create, fromHome = true)
        advanceUntilIdle()

        underTest.saveFile()
        advanceUntilIdle()

        val fromHomeCaptor = argumentCaptor<Boolean>()
        verify(saveTextContentForTextEditorUseCase).invoke(
            any(),
            any(),
            any(),
            any(),
            fromHomeCaptor.capture(),
            any(),
        )
        assertThat(fromHomeCaptor.firstValue).isTrue()
    }

    @Test
    fun `test that confirmDiscard in Create mode does not invoke save use case`() = runTest {
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(
                TextEditorSaveResult.UploadRequired(
                    tempPath = "/tmp/x.txt",
                    parentHandle = 1L,
                    isEditMode = false,
                    fromHome = false,
                ),
            )
        initUnderTest(mode = TextEditorMode.Create)
        advanceUntilIdle()
        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "edited") }

        underTest.confirmDiscard()
        advanceUntilIdle()

        verify(saveTextContentForTextEditorUseCase, never()).invoke(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `test that saveFile in Create mode emits closeEvent and does not queue snackbar`() =
        runTest {
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(
                    TextEditorSaveResult.UploadRequired(
                        tempPath = "/tmp/new.txt",
                        parentHandle = 1L,
                        isEditMode = false,
                        fromHome = false,
                    )
                )
            initUnderTest(mode = TextEditorMode.Create)
            advanceUntilIdle()

            underTest.saveFile()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.closeEvent).isEqualTo(triggered)
            verify(snackbarEventQueue, never()).queueMessage(sharedR.string.general_changes_saved)
        }

    @Test
    fun `test that saveFile in Edit mode emits closeEvent and queues snackbar`() =
        runTest {
            val lines = listOf("hello")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(
                    TextEditorSaveResult.UploadRequired(
                        tempPath = "/tmp/edit.txt",
                        parentHandle = 1L,
                        isEditMode = true,
                        fromHome = false,
                    )
                )
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.saveFile()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.closeEvent).isEqualTo(triggered)
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_changes_saved)
        }

    @Test
    fun `test that isContentDirty returns true after chunk disposed with edits`() = runTest {
        val lines = (1..100).map { "line$it" }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "edited") }
        underTest.disposeChunkState(0)

        assertThat(underTest.isContentDirty()).isTrue()
    }

    @Test
    fun `test that shouldPopDestinationOnCleanEditExit is true when opened in Edit mode`() {
        initUnderTest(mode = TextEditorMode.Edit)
        assertThat(underTest.shouldPopDestinationOnCleanEditExit()).isTrue()
    }

    @Test
    fun `test that shouldPopDestinationOnCleanEditExit is false when opened in View mode`() {
        initUnderTest(mode = TextEditorMode.View)
        assertThat(underTest.shouldPopDestinationOnCleanEditExit()).isFalse()
    }

    @Test
    fun `test that consumeErrorEvent consumes errorEvent`() {
        initUnderTest()
        underTest.consumeErrorEvent()
        assertThat(underTest.uiState.value.errorEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that getText failure triggers errorEvent and clears loading`() = runTest {
        doReturn(flow<List<String>> { throw RuntimeException("load failed") })
            .whenever(getTextContentForTextEditorUseCase).invoke(any<Long>(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.errorEvent).isEqualTo(triggered)
        assertThat(underTest.uiState.value.isLoading).isFalse()
        assertThat(underTest.uiState.value.errorMessage).isEqualTo("load failed")
        assertThat(underTest.uiState.value.isNoInternetError).isFalse()
    }

    @Test
    fun `test that offline at start with no localPath sets isNoInternetError without invoking use case`() =
        runTest {
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, localPath = null)
            advanceUntilIdle()
            val state = underTest.uiState.value
            assertThat(state.errorEvent).isEqualTo(triggered)
            assertThat(state.isLoading).isFalse()
            assertThat(state.isNoInternetError).isTrue()
            verify(getTextContentForTextEditorUseCase, never())
                .invoke(any<Long>(), anyOrNull(), any())
        }

    @Test
    fun `test that offline at start with localPath proceeds with load`() = runTest {
        whenever(isConnectedToInternetUseCase()).thenReturn(false)
        doReturn(flowOf(listOf("local content")))
            .whenever(getTextContentForTextEditorUseCase)
            .invoke(any<Long>(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, localPath = "/some/local.txt")
        advanceUntilIdle()
        val state = underTest.uiState.value
        assertThat(state.errorEvent).isEqualTo(consumed)
        assertThat(state.isLoading).isFalse()
        assertThat(state.isNoInternetError).isFalse()
    }

    @Test
    fun `test that consumeErrorEvent clears isNoInternetError`() = runTest {
        whenever(isConnectedToInternetUseCase()).thenReturn(false)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, localPath = null)
        advanceUntilIdle()
        underTest.consumeErrorEvent()
        assertThat(underTest.uiState.value.errorEvent).isEqualTo(consumed)
        assertThat(underTest.uiState.value.isNoInternetError).isFalse()
    }

    @Test
    fun `test that connectivity drop during load cancels load and sets isNoInternetError`() =
        runTest {
            val connectivity = MutableStateFlow(true)
            whenever(monitorConnectivityUseCase()).thenReturn(connectivity)
            val hangingFlow = flow<List<String>> { awaitCancellation() }
            doReturn(hangingFlow)
                .whenever(getTextContentForTextEditorUseCase)
                .invoke(any<Long>(), anyOrNull(), any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()
            assertThat(underTest.uiState.value.isLoading).isTrue()

            connectivity.value = false
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.errorEvent).isEqualTo(triggered)
            assertThat(state.isLoading).isFalse()
            assertThat(state.isNoInternetError).isTrue()
        }

    @Test
    fun `test that connectivity drop after load does not trigger error`() = runTest {
        val connectivity = MutableStateFlow(true)
        whenever(monitorConnectivityUseCase()).thenReturn(connectivity)
        doReturn(flowOf(listOf("hello")))
            .whenever(getTextContentForTextEditorUseCase)
            .invoke(any<Long>(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.isLoading).isFalse()

        connectivity.value = false
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.errorEvent).isEqualTo(consumed)
        assertThat(state.isNoInternetError).isFalse()
    }

    @Test
    fun `test that setEditMode updates uiState to edit mode`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "a.txt",
        )
        underTest.setEditMode()

        val state = underTest.uiState.value
        assertThat(state.mode).isEqualTo(TextEditorMode.Edit)
    }

    @Test
    fun `test that setEditMode creates chunks from loaded content`() = runTest {
        // Use lines long enough so total chars exceed CHUNK_MAX_CHARS per chunk
        val lineContent = "x".repeat(100)
        val totalLines = 2000
        val allLines = (1..totalLines).map { "$lineContent$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        underTest.setEditMode()

        assertThat(underTest.getChunkCount()).isGreaterThan(1)
        val chunk0State = underTest.getOrCreateChunkState(0)
        val chunk0Text = chunk0State.text.toString()
        assertThat(chunk0Text.split("\n").first()).isEqualTo("${lineContent}1")
    }

    @Test
    fun `test that setViewMode updates uiState to view mode`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.Edit,
            fileName = "a.txt",
        )
        underTest.setViewMode()

        val state = underTest.uiState.value
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
    }

    @Test
    fun `test that setViewMode without edits switches directly to View`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.setViewMode()

        val state = underTest.uiState.value
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
        assertThat(state.isRestoringContent).isFalse()
    }

    @Test
    fun `test that onBottomBarAction Edit sets mode to Edit`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "a.txt",
        )
        underTest.onBottomBarAction(TextEditorBottomBarAction.Edit)

        assertThat(underTest.uiState.value.mode).isEqualTo(TextEditorMode.Edit)
    }

    @Test
    fun `test that onBottomBarAction Download emits StartDownloadNode transferEvent when node exists`() = runTest {
        val node = mock<TypedNode>()
        runBlocking { whenever(getNodeByIdUseCase(NodeId(42L))).thenReturn(node) }
        initUnderTest(nodeHandle = 42L, mode = TextEditorMode.View)
        advanceUntilIdle()

        underTest.onBottomBarAction(TextEditorBottomBarAction.Download)
        advanceUntilIdle()

        val event = underTest.uiState.value.transferEvent
        check(event is StateEventWithContentTriggered<*>)
        val content = event.content
        assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
        assertThat((content as TransferTriggerEvent.StartDownloadNode).nodes).containsExactly(node)
    }

    @Test
    fun `test that onBottomBarAction GetLink triggers ManageLink node effect`() {
        initUnderTest(nodeHandle = 99L, mode = TextEditorMode.View)
        underTest.onBottomBarAction(TextEditorBottomBarAction.GetLink)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(TextEditorNodeEffect.ManageLink(99L))
    }

    @Test
    fun `test that onBottomBarAction Share triggers Share node effect`() {
        initUnderTest(nodeHandle = 7L, mode = TextEditorMode.View, fileName = "a.txt")
        underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(
            TextEditorNodeEffect.Share(
                nodeHandle = 7L,
                localPath = null,
                fileName = "a.txt",
            ),
        )
    }

    @Test
    fun `test that onBottomBarAction SendToChat triggers SendToChat node effect`() = runTest {
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()
        underTest.onBottomBarAction(TextEditorBottomBarAction.SendToChat)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(TextEditorNodeEffect.SendToChat(1L))
    }

    @Test
    fun `test that View mode loads bottomBarActions from node and access and updates uiState`() = runTest {
        val node = mock<TypedNode>()
        whenever(node.exportedData).thenReturn(mock())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(node)
            whenever(getNodeAccessUseCase(any())).thenReturn(AccessPermission.OWNER)
        }
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, showShare = false)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.bottomBarActions)
            .containsExactly(TextEditorBottomBarAction.Download, TextEditorBottomBarAction.Edit)
    }

    @Test
    fun `test that View mode hides Edit action when node is in backups`() = runTest {
        val node = mock<TypedNode>()
        whenever(node.exportedData).thenReturn(mock())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(node)
            whenever(getNodeAccessUseCase(any())).thenReturn(AccessPermission.OWNER)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(true)
        }
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, showShare = false)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.bottomBarActions)
            .doesNotContain(TextEditorBottomBarAction.Edit)
    }

    @Test
    fun `test that gradual load updates totalLineCount`() = runTest {
        val chunk1 = (1..500).map { "line$it" }
        val chunk2 = (501..1000).map { "line$it" }
        doReturn(flowOf(chunk1, chunk2)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.totalLineCount).isEqualTo(1000)
        assertThat(state.isFullyLoaded).isTrue()
    }

    @Test
    fun `test that getChunkText returns correct lines for chunk index`() = runTest {
        val lineContent = "x".repeat(100)
        val totalLines = 1000
        val allLines = (1..totalLines).map { "$lineContent$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        val chunkCount = underTest.getChunkCount()
        assertThat(chunkCount).isGreaterThan(1)

        val chunk0 = underTest.getChunkText(0)
        val chunk0Lines = chunk0.split("\n")
        assertThat(chunk0Lines.first()).isEqualTo("${lineContent}1")

        val lastChunk = underTest.getChunkText(chunkCount - 1)
        val lastChunkLines = lastChunk.split("\n")
        assertThat(lastChunkLines.last()).isEqualTo("$lineContent$totalLines")
    }

    @Test
    fun `test that getChunkText returns empty for out of range index`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.getChunkText(999)).isEmpty()
    }

    @Test
    fun `test that disposeChunkState flushes edits back to chunk data`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "EDITED\nline2") }
        underTest.disposeChunkState(0)

        val newState = underTest.getOrCreateChunkState(0)
        assertThat(newState.text.toString()).startsWith("EDITED")
    }

    @Test
    fun `test that disposeChunkState preserves cursor position on recreation`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { selection = TextRange(5) }
        underTest.disposeChunkState(0)

        val newState = underTest.getOrCreateChunkState(0)
        assertThat(newState.selection.start).isEqualTo(5)
        assertThat(newState.selection.end).isEqualTo(5)
    }

    @Test
    fun `test that disposeChunkState sets restoreFocusChunkIndex for focused chunk`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.getOrCreateChunkState(0)
        underTest.disposeChunkState(0)

        assertThat(underTest.uiState.value.restoreFocusChunkIndex).isEqualTo(0)
    }

    @Test
    fun `test that disposeChunkState does not set restoreFocusChunkIndex for non-focused chunk`() = runTest {
        val allLines = (1..500).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.setFocusedEditChunk(0)
        underTest.getOrCreateChunkState(1)
        underTest.disposeChunkState(1)

        assertThat(underTest.uiState.value.restoreFocusChunkIndex).isNull()
    }

    @Test
    fun `test that consumeRestoreFocusChunkIndex clears the value`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.getOrCreateChunkState(0)
        underTest.disposeChunkState(0)
        assertThat(underTest.uiState.value.restoreFocusChunkIndex).isEqualTo(0)

        underTest.consumeRestoreFocusChunkIndex()
        assertThat(underTest.uiState.value.restoreFocusChunkIndex).isNull()
    }

    @Test
    fun `test that requestShowDiscardDialog sets showDiscardDialog to true`() {
        initUnderTest(mode = TextEditorMode.Edit)
        underTest.requestShowDiscardDialog()
        assertThat(underTest.uiState.value.showDiscardDialog).isTrue()
    }

    @Test
    fun `test that dismissDiscardDialog sets showDiscardDialog to false`() {
        initUnderTest(mode = TextEditorMode.Edit)
        underTest.requestShowDiscardDialog()
        underTest.dismissDiscardDialog()
        assertThat(underTest.uiState.value.showDiscardDialog).isFalse()
    }

    @Test
    fun `test that confirmDiscard restores content and switches to View mode`() = runTest {
        val lines = listOf("line1", "line2", "line3")
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.confirmDiscard()
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
        assertThat(state.isRestoringContent).isFalse()
        assertThat(state.showDiscardDialog).isFalse()
        assertThat(underTest.getChunkText(0)).isEqualTo(lines.joinToString("\n"))
    }

    @Test
    fun `test that setViewMode with discardChanges sets isRestoringContent to true during restore`() =
        runTest {
            val lines = listOf("line1", "line2")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            initUnderTest(
                nodeHandle = 1L,
                mode = TextEditorMode.Edit,
                defaultDispatcher = StandardTestDispatcher(testScheduler),
            )
            advanceUntilIdle()

            underTest.requestShowDiscardDialog()
            assertThat(underTest.uiState.value.showDiscardDialog).isTrue()

            underTest.setViewMode(discardChanges = true)
            assertThat(underTest.uiState.value.isRestoringContent).isTrue()

            advanceUntilIdle()
            assertThat(underTest.uiState.value.isRestoringContent).isFalse()
        }

    @Test
    fun `test that saveFile flushes edits and saves full content`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        val saveResult = TextEditorSaveResult.UploadRequired(
            tempPath = "/tmp/test.txt",
            parentHandle = 1L,
            isEditMode = true,
            fromHome = false,
        )
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(saveResult)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, 5, "EDITED") }
        underTest.saveFile()
        advanceUntilIdle()

        val textCaptor = argumentCaptor<String>()
        verify(saveTextContentForTextEditorUseCase).invoke(
            any(), textCaptor.capture(), any(), any(), any(), any(),
        )
        val savedLines = textCaptor.firstValue.split("\n")
        assertThat(savedLines.first()).startsWith("EDITED")
        assertThat(savedLines).hasSize(100)
        assertThat(savedLines.last()).isEqualTo("line100")
    }

    @Test
    fun `test that saveFile in Create mode saves content as-is`() = runTest {
        val saveResult = TextEditorSaveResult.UploadRequired(
            tempPath = "/tmp/new.txt",
            parentHandle = 1L,
            isEditMode = false,
            fromHome = false,
        )
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(saveResult)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Create)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "brand new content") }
        underTest.saveFile()
        advanceUntilIdle()

        val textCaptor = argumentCaptor<String>()
        verify(saveTextContentForTextEditorUseCase).invoke(
            any(), textCaptor.capture(), any(), any(), any(), any(),
        )
        assertThat(textCaptor.firstValue).isEqualTo("brand new content")
    }

    @Test
    fun `test that saveFile saves when started in View mode and switched to Edit mode`() =
        runTest {
            doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            val saveResult = TextEditorSaveResult.UploadRequired(
                tempPath = "/tmp/edited.txt",
                parentHandle = 1L,
                isEditMode = true,
                fromHome = false,
            )
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(saveResult)

            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()
            underTest.setEditMode()
            underTest.saveFile()
            advanceUntilIdle()

            verify(saveTextContentForTextEditorUseCase).invoke(
                any(), any(), any(), any(), any(), any(),
            )
        }

    @Test
    fun `test that saveFile sets closeEvent to triggered and mode to View on success`() =
        runTest {
            doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            val saveResult = TextEditorSaveResult.UploadRequired(
                tempPath = "/tmp/test.txt",
                parentHandle = 1L,
                isEditMode = true,
                fromHome = false,
            )
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(saveResult)

            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.saveFile()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.closeEvent).isEqualTo(triggered)
            assertThat(state.mode).isEqualTo(TextEditorMode.View)
        }

    @Test
    fun `test that saveFile in Edit mode queues global snackbar message`() =
        runTest {
            doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            val saveResult = TextEditorSaveResult.UploadRequired(
                tempPath = "/tmp/test.txt",
                parentHandle = 1L,
                isEditMode = true,
                fromHome = false,
            )
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(saveResult)

            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.saveFile()
            advanceUntilIdle()

            verify(snackbarEventQueue).queueMessage(
                sharedR.string.general_changes_saved
            )
        }

    @Test
    fun `test that saveFile failure triggers errorEvent and sets errorMessage`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("disk full"))

        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.saveFile()
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.errorEvent).isEqualTo(triggered)
        assertThat(state.errorMessage).isEqualTo("disk full")
    }

    @Test
    fun `test that saveFile is no-op when mode is View`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        val before = underTest.uiState.value
        underTest.saveFile()
        advanceUntilIdle()
        assertThat(underTest.uiState.value).isEqualTo(before)
    }

    @Test
    fun `test that getChunkStartLine returns 1 for first chunk`() = runTest {
        val allLines = (1..400).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.getChunkStartLine(0)).isEqualTo(1)
    }

    @Test
    fun `test that getChunkStartLine returns correct offset for second chunk`() = runTest {
        val lineContent = "x".repeat(100)
        val totalLines = 1000
        val allLines = (1..totalLines).map { "$lineContent$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.getChunkCount()).isGreaterThan(1)
        // Second chunk starts after all lines in the first chunk
        val chunk0LineCount = underTest.getChunkText(0).split("\n").size
        assertThat(underTest.getChunkStartLine(1)).isEqualTo(chunk0LineCount + 1)
    }

    @Test
    fun `test that setFocusedEditChunk updates focusedEditChunk in uiState`() = runTest {
        val allLines = (1..400).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.setFocusedEditChunk(1)
        assertThat(underTest.uiState.value.focusedEditChunk).isEqualTo(1)
    }

    @Test
    fun `test that consumeTransferEvent resets transfer event`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        val saveResult = TextEditorSaveResult.UploadRequired(
            tempPath = "/tmp/test.txt",
            parentHandle = 1L,
            isEditMode = true,
            fromHome = false,
        )
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(saveResult)

        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()
        underTest.saveFile()
        advanceUntilIdle()

        underTest.consumeTransferEvent()
        assertThat(underTest.uiState.value.transferEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that consumeNodeEffectEvent resets node effect`() {
        initUnderTest(nodeHandle = 2L)
        underTest.onBottomBarAction(TextEditorBottomBarAction.GetLink)
        check(underTest.uiState.value.nodeEffectEvent is StateEventWithContentTriggered<*>)
        underTest.consumeNodeEffectEvent()
        assertThat(underTest.uiState.value.nodeEffectEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that onBottomBarAction Share emits resolvedPublicLink when exportNodeUseCase returns a link`() =
        runTest {
            val publicLink = "https://mega.nz/file/abc123"
            runBlocking {
                whenever(getNodeByIdUseCase(NodeId(5L))).thenReturn(null)
                whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenReturn(publicLink)
            }
            initUnderTest(nodeHandle = 5L, fileName = "doc.txt")
            advanceUntilIdle()

            underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
            advanceUntilIdle()

            val ev = underTest.uiState.value.nodeEffectEvent
            check(ev is StateEventWithContentTriggered<*>)
            assertThat(ev.content).isEqualTo(
                TextEditorNodeEffect.Share(
                    nodeHandle = 5L,
                    localPath = null,
                    fileName = "doc.txt",
                    resolvedPublicLink = publicLink,
                ),
            )
        }

    @Test
    fun `test that onBottomBarAction Share uses existing public link from node when already exported`() =
        runTest {
            val existingLink = "https://mega.nz/file/existing"
            val node = mock<TypedNode>()
            whenever(node.exportedData).thenReturn(ExportedData(publicLink = existingLink, publicLinkCreationTime = 0L))
            runBlocking { whenever(getNodeByIdUseCase(NodeId(7L))).thenReturn(node) }
            initUnderTest(nodeHandle = 7L, fileName = "report.pdf")
            advanceUntilIdle()

            underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
            advanceUntilIdle()

            val ev = underTest.uiState.value.nodeEffectEvent
            check(ev is StateEventWithContentTriggered<*>)
            assertThat(ev.content).isEqualTo(
                TextEditorNodeEffect.Share(
                    nodeHandle = 7L,
                    localPath = null,
                    fileName = "report.pdf",
                    resolvedPublicLink = existingLink,
                ),
            )
        }

    @Test
    fun `test that onBottomBarAction Share triggers shareErrorEvent when exportNodeUseCase throws`() =
        runTest {
            runBlocking {
                whenever(getNodeByIdUseCase(any())).thenReturn(null)
                whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenThrow(RuntimeException("export failed"))
            }
            initUnderTest(nodeHandle = 5L)
            advanceUntilIdle()

            underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.shareErrorEvent).isEqualTo(triggered)
        }

    @Test
    fun `test that GetLink and SendToChat are no-op when node handle is invalid`() {
        initUnderTest(nodeHandle = -1L)
        underTest.onBottomBarAction(TextEditorBottomBarAction.GetLink)
        assertThat(underTest.uiState.value.nodeEffectEvent).isEqualTo(consumed())
        underTest.onBottomBarAction(TextEditorBottomBarAction.SendToChat)
        assertThat(underTest.uiState.value.nodeEffectEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that onBottomBarAction Download is no-op when node handle is invalid`() {
        initUnderTest(nodeHandle = -1L)
        underTest.onBottomBarAction(TextEditorBottomBarAction.Download)
        assertThat(underTest.uiState.value.transferEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that onBottomBarAction Share is no-op when node handle is invalid`() {
        initUnderTest(nodeHandle = -1L)
        underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
        assertThat(underTest.uiState.value.nodeEffectEvent).isEqualTo(consumed())
        assertThat(underTest.uiState.value.shareErrorEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that consumeShareErrorEvent resets shareErrorEvent`() = runTest {
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenThrow(RuntimeException())
        }
        initUnderTest(nodeHandle = 5L)
        advanceUntilIdle()

        underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
        advanceUntilIdle()
        check(underTest.uiState.value.shareErrorEvent == triggered)

        underTest.consumeShareErrorEvent()
        assertThat(underTest.uiState.value.shareErrorEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that attachNodesToChat passes combined chatIds from user handles and direct chatIds`() =
        runTest {
            val chatIdFromHandle1 = 100L
            val chatIdFromHandle2 = 200L
            whenever(get1On1ChatIdUseCase(10L)).thenReturn(chatIdFromHandle1)
            whenever(get1On1ChatIdUseCase(20L)).thenReturn(chatIdFromHandle2)
            initUnderTest(nodeHandle = 5L)

            val result = SendToChatResult(
                nodeIds = longArrayOf(5L),
                chatIds = longArrayOf(300L),
                userHandles = longArrayOf(10L, 20L),
            )
            underTest.attachNodesToChat(result)
            advanceUntilIdle()

            val chatIdsCaptor = argumentCaptor<List<Long>>()
            verify(attachMultipleNodesUseCase).invoke(any(), chatIdsCaptor.capture())
            assertThat(chatIdsCaptor.firstValue).containsExactly(100L, 200L, 300L)
        }

    @Test
    fun `test that attachNodesToChat skips user handles that fail to resolve`() = runTest {
        whenever(get1On1ChatIdUseCase(10L)).thenReturn(100L)
        whenever(get1On1ChatIdUseCase(20L)).thenThrow(RuntimeException("resolve failed"))
        initUnderTest(nodeHandle = 5L)

        val result = SendToChatResult(
            nodeIds = longArrayOf(5L),
            chatIds = longArrayOf(),
            userHandles = longArrayOf(10L, 20L),
        )
        underTest.attachNodesToChat(result)
        advanceUntilIdle()

        val chatIdsCaptor = argumentCaptor<List<Long>>()
        verify(attachMultipleNodesUseCase).invoke(any(), chatIdsCaptor.capture())
        assertThat(chatIdsCaptor.firstValue).containsExactly(100L)
    }

    @Test
    fun `test that attachNodesToChat emits sendToChatErrorEvent when attachMultipleNodesUseCase throws`() =
        runTest {
            whenever(get1On1ChatIdUseCase(any())).thenReturn(100L)
            whenever(attachMultipleNodesUseCase(any(), any()))
                .thenThrow(RuntimeException("attach failed"))
            initUnderTest(nodeHandle = 5L)

            val result = SendToChatResult(
                nodeIds = longArrayOf(5L),
                chatIds = longArrayOf(),
                userHandles = longArrayOf(10L),
            )
            underTest.attachNodesToChat(result)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.sendToChatErrorEvent).isEqualTo(triggered)
        }

    @Test
    fun `test that consumeSendToChatErrorEvent resets sendToChatErrorEvent`() = runTest {
        whenever(get1On1ChatIdUseCase(any())).thenReturn(100L)
        whenever(attachMultipleNodesUseCase(any(), any()))
            .thenThrow(RuntimeException("attach failed"))
        initUnderTest(nodeHandle = 5L)

        val result = SendToChatResult(
            nodeIds = longArrayOf(5L),
            chatIds = longArrayOf(),
            userHandles = longArrayOf(10L),
        )
        underTest.attachNodesToChat(result)
        advanceUntilIdle()
        check(underTest.uiState.value.sendToChatErrorEvent == triggered)

        underTest.consumeSendToChatErrorEvent()
        assertThat(underTest.uiState.value.sendToChatErrorEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that getChunkCount returns correct count in View mode`() = runTest {
        val lineContent = "x".repeat(100)
        val totalLines = 2000
        val allLines = (1..totalLines).map { "$lineContent$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.getChunkCount()).isGreaterThan(1)
    }

    @Test
    fun `test that getChunkCount returns 1 for Create mode with empty content`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.getChunkCount()).isEqualTo(1)
    }

    @Test
    fun `test that getChunkCount returns correct count in Edit mode`() = runTest {
        val lineContent = "x".repeat(100)
        val totalLines = 2000
        val allLines = (1..totalLines).map { "$lineContent$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        underTest.setEditMode()
        assertThat(underTest.getChunkCount()).isGreaterThan(1)
    }

    @Test
    fun `test that handleClose emits closeEvent when Create mode has no edits`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.getOrCreateChunkState(0)

        underTest.handleClose()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that handleClose shows discard dialog when Create mode has edits`() {
        initUnderTest(mode = TextEditorMode.Create)
        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "edited") }

        underTest.handleClose()

        assertThat(underTest.uiState.value.showDiscardDialog).isTrue()
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that handleClose emits closeEvent when Edit mode opened as Edit has no edits`() =
        runTest {
            val lines = listOf("line1")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.handleClose()

            assertThat(underTest.uiState.value.closeEvent).isEqualTo(triggered)
        }

    @Test
    fun `test that handleClose switches to View mode when Edit mode opened as View has no edits`() =
        runTest {
            val lines = listOf("line1")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()

            underTest.setEditMode()
            underTest.handleClose()

            assertThat(underTest.uiState.value.mode).isEqualTo(TextEditorMode.View)
            assertThat(underTest.uiState.value.closeEvent).isEqualTo(consumed)
        }

    @Test
    fun `test that closing a read-through text file removes it from CWLO and does not save scroll state`() =
        runTest {
            val lines = listOf("line1")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            whenever(saveRecentlyUsedItemIfQualifiesUseCase(any(), any(), any(), any()))
                .thenReturn(false)
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()

            // First visible chunk still near the top, but the bottom of the viewport has
            // reached the end of the file.
            underTest.updateScrollPosition(
                fraction = 0.1f,
                scrollOffset = 0,
                readThroughFraction = 0.95f,
            )
            underTest.handleClose()
            advanceUntilIdle()

            verify(saveRecentlyUsedItemIfQualifiesUseCase).invoke(
                nodeHandle = 1L,
                type = RecentlyUsedType.TextEditor,
                fileName = "",
                progress = 0.95f,
            )
            verify(saveTextEditorScrollUseCase, never()).invoke(any())
        }

    @Test
    fun `test that closing a not read-through text file saves scroll state`() =
        runTest {
            val lines = listOf("line1")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            whenever(saveRecentlyUsedItemIfQualifiesUseCase(any(), any(), any(), any()))
                .thenReturn(true)
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()

            underTest.updateScrollPosition(
                fraction = 0.5f,
                scrollOffset = 0,
                readThroughFraction = 0.5f,
            )
            underTest.handleClose()
            advanceUntilIdle()

            verify(saveRecentlyUsedItemIfQualifiesUseCase).invoke(
                nodeHandle = 1L,
                type = RecentlyUsedType.TextEditor,
                fileName = "",
                progress = 0.5f,
            )
            verify(saveTextEditorScrollUseCase).invoke(any())
        }

    @Test
    fun `test that closing does not persist Continue Where Left Off for an invalid node handle`() =
        runTest {
            val lines = listOf("line1")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            initUnderTest(nodeHandle = -1L, mode = TextEditorMode.View)
            advanceUntilIdle()

            underTest.updateScrollPosition(
                fraction = 0.95f,
                scrollOffset = 0,
                readThroughFraction = 0.95f,
            )
            underTest.handleClose()
            advanceUntilIdle()

            verifyNoInteractions(saveRecentlyUsedItemIfQualifiesUseCase)
        }

    @Test
    fun `test that handleClose shows discard dialog when Edit mode has edits`() = runTest {
        val lines = (1..100).map { "line$it" }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "modified") }

        underTest.handleClose()

        assertThat(underTest.uiState.value.showDiscardDialog).isTrue()
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that consumeCloseEvent resets closeEvent`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.getOrCreateChunkState(0)
        underTest.handleClose()
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(triggered)

        underTest.consumeCloseEvent()
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that saveRecentlyUsedItem is called after content loads`() = runTest {
        whenever(getTextContentForTextEditorUseCase(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any()))
            .thenReturn(flowOf(listOf("line1")))
        initUnderTest(nodeHandle = 42L, fileName = "test.txt")
        advanceUntilIdle()

        verify(saveRecentlyUsedItemUseCase).invoke(
            nodeHandle = 42L,
            type = RecentlyUsedType.TextEditor,
            fileName = "test.txt",
        )
    }

    @Test
    fun `test that saveRecentlyUsedItem is not called in Create mode`() = runTest {
        // In Create mode the node handle is the destination parent folder, not a real file (the
        // file does not exist until the upload completes), so recording here would add the parent
        // folder (e.g. "Cloud Drive") to the list. It must be skipped.
        initUnderTest(nodeHandle = 100L, mode = TextEditorMode.Create, fileName = "new.txt")
        underTest.getOrCreateChunkState(0)
        underTest.handleClose()
        advanceUntilIdle()

        verify(saveRecentlyUsedItemUseCase, never()).invoke(any(), any(), any())
        verify(saveRecentlyUsedItemIfQualifiesUseCase, never()).invoke(any(), any(), any(), any())
    }

    @Test
    fun `test that previous versions are removed from recently used when content loads`() = runTest {
        whenever(getTextContentForTextEditorUseCase(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any()))
            .thenReturn(flowOf(listOf("line1")))
        val currentVersion = mock<TypedFileNode> { on { id } doReturn NodeId(42L) }
        val previousVersion = mock<TypedFileNode> { on { id } doReturn NodeId(10L) }
        whenever(getNodeVersionsByHandleUseCase(NodeId(42L)))
            .thenReturn(listOf<UnTypedNode>(currentVersion, previousVersion))
        initUnderTest(nodeHandle = 42L, fileName = "test.txt")
        advanceUntilIdle()

        verify(removeRecentlyUsedItemUseCase).invoke(10L)
        verify(removeRecentlyUsedItemUseCase, never()).invoke(42L)
    }

    @Test
    fun `test that scroll state is restored after content loads when saved state exists`() = runTest {
        whenever(getTextContentForTextEditorUseCase(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any()))
            .thenReturn(flowOf((1..1000).map { "line $it" }))
        whenever(getTextEditorScrollUseCase(42L)).thenReturn(
            TextEditorScroll(
                nodeHandle = 42L,
                cursorPosition = 0,
                scrollFraction = 0.5f,
            )
        )
        initUnderTest(nodeHandle = 42L)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.restoreScrollIndex).isNotNull()
    }

    @Test
    fun `test that restoreScrollOffset is populated when saved state has non-zero cursorPosition`() =
        runTest {
            whenever(
                getTextContentForTextEditorUseCase(
                    nodeHandle = any(),
                    localPath = anyOrNull(),
                    chunkSizeLines = any()
                )
            ).thenReturn(flowOf((1..1000).map { "line $it" }))
            whenever(getTextEditorScrollUseCase(42L)).thenReturn(
                TextEditorScroll(
                    nodeHandle = 42L,
                    cursorPosition = 500,
                    scrollFraction = 0.5f,
                )
            )
            initUnderTest(nodeHandle = 42L)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.restoreScrollOffset).isEqualTo(500)
        }

    @Test
    fun `test that restoreScrollIndex is null when no saved state exists`() = runTest {
        whenever(getTextContentForTextEditorUseCase(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any()))
            .thenReturn(flowOf(listOf("line1")))
        whenever(getTextEditorScrollUseCase(any())).thenReturn(null)
        initUnderTest(nodeHandle = 42L)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.restoreScrollIndex).isNull()
    }

    @Test
    fun `test that handleClose saves scroll state before closing`() = runTest {
        whenever(getTextContentForTextEditorUseCase(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any()))
            .thenReturn(flowOf(listOf("line1")))
        whenever(saveRecentlyUsedItemIfQualifiesUseCase(any(), any(), any(), any()))
            .thenReturn(true)
        initUnderTest(nodeHandle = 42L, mode = TextEditorMode.View)
        advanceUntilIdle()

        underTest.updateScrollPosition(0.7f, 0)
        underTest.handleClose()
        advanceUntilIdle()

        val captor = argumentCaptor<TextEditorScroll>()
        verify(saveTextEditorScrollUseCase).invoke(captor.capture())
        assertThat(captor.firstValue.scrollFraction).isEqualTo(0.7f)
    }

    @Test
    fun `test that consumeRestoreScrollIndex resets to null`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.consumeRestoreScrollIndex()
        assertThat(underTest.uiState.value.restoreScrollIndex).isNull()
    }

    @Test
    fun `test that scroll fraction is saved as zero when updateScrollPosition is never called`() =
        runTest {
            whenever(
                getTextContentForTextEditorUseCase(
                    nodeHandle = any(),
                    localPath = anyOrNull(),
                    chunkSizeLines = any()
                )
            ).thenReturn(flowOf(listOf("line1")))
            whenever(saveRecentlyUsedItemIfQualifiesUseCase(any(), any(), any(), any()))
                .thenReturn(true)
            initUnderTest(nodeHandle = 42L, mode = TextEditorMode.View)
            advanceUntilIdle()

            underTest.handleClose()
            advanceUntilIdle()

            val captor = argumentCaptor<TextEditorScroll>()
            verify(saveTextEditorScrollUseCase).invoke(captor.capture())
            assertThat(captor.firstValue.scrollFraction).isEqualTo(0f)
        }

    @Test
    fun `test that init resolves chat file and loads content when chatId and messageId are provided`() =
        runTest {
            val chatId = 100L
            val messageId = 200L
            val resolvedHandle = 42L
            val typedFileNode = mock<TypedFileNode> {
                on { id } doReturn NodeId(resolvedHandle)
                on { name } doReturn "chat_file.txt"
            }
            val chatFile = ChatDefaultFile(
                typedFileNode = typedFileNode,
                chatId = chatId,
                messageId = messageId,
            )
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            doReturn(flowOf(listOf("hello from chat"))).whenever(getTextContentForTextEditorUseCase)
                .invoke(resolvedNode = any(), localPath = anyOrNull(), chunkSizeLines = any())

            initUnderTest(
                nodeHandle = -1L,
                chatId = chatId,
                messageId = messageId,
            )
            advanceUntilIdle()

            verify(getChatFileUseCase, atLeast(1)).invoke(chatId, messageId)
            verify(getTextContentForTextEditorUseCase).invoke(
                resolvedNode = chatFile,
                localPath = null,
                chunkSizeLines = 500,
            )
            val state = underTest.uiState.value
            assertThat(state.fileName).isEqualTo("chat_file.txt")
            assertThat(state.isLoading).isFalse()
        }

    @Test
    fun `test that init shows error when chat file is not found`() = runTest {
        val chatId = 100L
        val messageId = 200L
        whenever(getChatFileUseCase(chatId, messageId)).thenReturn(null)

        initUnderTest(
            nodeHandle = -1L,
            chatId = chatId,
            messageId = messageId,
        )
        advanceUntilIdle()

        verify(getChatFileUseCase, atLeast(1)).invoke(chatId, messageId)
        verify(getTextContentForTextEditorUseCase, never()).invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        verify(getTextContentForTextEditorUseCase, never()).invoke(resolvedNode = any(), localPath = anyOrNull(), chunkSizeLines = any())
        val state = underTest.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that init shows error with message when getChatFileUseCase throws`() = runTest {
        val chatId = 100L
        val messageId = 200L
        whenever(getChatFileUseCase(chatId, messageId))
            .thenThrow(RuntimeException("SDK error"))

        initUnderTest(
            nodeHandle = -1L,
            chatId = chatId,
            messageId = messageId,
        )
        advanceUntilIdle()

        verify(getChatFileUseCase, atLeast(1)).invoke(chatId, messageId)
        verify(getTextContentForTextEditorUseCase, never()).invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        verify(getTextContentForTextEditorUseCase, never()).invoke(resolvedNode = any(), localPath = anyOrNull(), chunkSizeLines = any())
        val state = underTest.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorEvent).isEqualTo(triggered)
        assertThat(state.errorMessage).isEqualTo("SDK error")
    }

    private suspend fun stubFileLinkInit() {
        whenever(getPublicNodeUseCase(FILE_LINK_URL)).thenReturn(fileLinkPublicNode)
        whenever(
            getTextContentForFileLinkUseCase(
                urlFileLink = any<String>(),
                chunkSizeLines = any(),
            )
        ).thenReturn(flowOf(listOf("hello")))
        whenever(getNodeByIdUseCase(any())).thenReturn(fileLinkPublicNode)
        whenever(getNodeAccessUseCase(any())).thenReturn(null)
    }

    @Test
    fun `test that init resolves public node when publicUrl is set`() = runTest {
        stubFileLinkInit()

        initUnderTest(publicUrl = FILE_LINK_URL)
        advanceUntilIdle()

        verify(getPublicNodeUseCase).invoke(FILE_LINK_URL)
        verify(getTextContentForFileLinkUseCase).invoke(
            urlFileLink = any<String>(),
            chunkSizeLines = any(),
        )
        val state = underTest.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.fileName).isEqualTo("public.txt")
    }

    @Test
    fun `test that init shows error when getPublicNodeUseCase fails`() = runTest {
        whenever(getPublicNodeUseCase(FILE_LINK_URL))
            .thenThrow(RuntimeException("Public node error"))

        initUnderTest(publicUrl = FILE_LINK_URL)
        advanceUntilIdle()

        verify(getPublicNodeUseCase).invoke(FILE_LINK_URL)
        verify(getTextContentForTextEditorUseCase, never()).invoke(
            nodeHandle = any(),
            localPath = anyOrNull(),
            chunkSizeLines = any(),
        )
        val state = underTest.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorEvent).isEqualTo(triggered)
        assertThat(state.errorMessage).isEqualTo("Public node error")
    }

    @Test
    fun `test that share uses publicUrl directly when set`() = runTest {
        stubFileLinkInit()

        initUnderTest(publicUrl = FILE_LINK_URL, showShare = true)
        advanceUntilIdle()

        underTest.onMenuAction(TextEditorTopBarAction.Share)
        advanceUntilIdle()

        val event = underTest.uiState.value.nodeEffectEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        val shareEffect =
            (event as StateEventWithContentTriggered).content as TextEditorNodeEffect.Share
        assertThat(shareEffect.resolvedPublicLink).isEqualTo(FILE_LINK_URL)
    }

    @Test
    fun `test that download uses mapTypedNodeToPublicLinkUseCase when publicUrl is set`() =
        runTest {
            stubFileLinkInit()
            val publicLinkFile = PublicLinkFile(fileLinkPublicNode, null)
            whenever(mapTypedNodeToPublicLinkUseCase(fileLinkPublicNode, null))
                .thenReturn(publicLinkFile)

            initUnderTest(publicUrl = FILE_LINK_URL, showDownload = true)
            advanceUntilIdle()

            underTest.onMenuAction(TextEditorTopBarAction.Download)
            advanceUntilIdle()

            val event = underTest.uiState.value.transferEvent
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val downloadEvent =
                (event as StateEventWithContentTriggered).content as TransferTriggerEvent.StartDownloadNode
            assertThat(downloadEvent.nodes).hasSize(1)
            assertThat(downloadEvent.nodes.first()).isInstanceOf(PublicLinkFile::class.java)
        }

    private suspend fun stubFolderLinkInit() {
        whenever(getPublicChildNodeFromIdUseCase(NodeId(FOLDER_LINK_HANDLE)))
            .thenReturn(PublicLinkFile(folderLinkNode, null))
        whenever(
            getTextContentForFolderLinkUseCase(
                node = any(),
                chunkSizeLines = any(),
            )
        ).thenReturn(flowOf(listOf("hello folder")))
        whenever(getNodeAccessUseCase(any())).thenReturn(null)
    }

    @Test
    fun `test that init resolves folder link node and loads content when isFolderLink is set`() =
        runTest {
            stubFolderLinkInit()

            initUnderTest(nodeHandle = FOLDER_LINK_HANDLE, isFolderLink = true)
            advanceUntilIdle()

            verify(getPublicChildNodeFromIdUseCase).invoke(NodeId(FOLDER_LINK_HANDLE))
            verify(getTextContentForFolderLinkUseCase).invoke(
                node = any(),
                chunkSizeLines = any(),
            )
            val state = underTest.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.fileName).isEqualTo("folder-link.txt")
        }

    @Test
    fun `test that init does not load via cloud use case for folder link`() = runTest {
        stubFolderLinkInit()

        initUnderTest(nodeHandle = FOLDER_LINK_HANDLE, isFolderLink = true)
        advanceUntilIdle()

        verify(getTextContentForTextEditorUseCase, never()).invoke(
            nodeHandle = any(),
            localPath = anyOrNull(),
            chunkSizeLines = any(),
        )
    }

    @Test
    fun `test that init shows error when folder link node cannot be resolved`() = runTest {
        whenever(getPublicChildNodeFromIdUseCase(NodeId(FOLDER_LINK_HANDLE))).thenReturn(null)

        initUnderTest(nodeHandle = FOLDER_LINK_HANDLE, isFolderLink = true)
        advanceUntilIdle()

        verify(getTextContentForFolderLinkUseCase, never()).invoke(
            node = any(),
            chunkSizeLines = any(),
        )
        val state = underTest.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that folder link download uses resolved public node without remapping`() = runTest {
        stubFolderLinkInit()

        initUnderTest(nodeHandle = FOLDER_LINK_HANDLE, isFolderLink = true, showDownload = true)
        advanceUntilIdle()

        underTest.onMenuAction(TextEditorTopBarAction.Download)
        advanceUntilIdle()

        verify(mapTypedNodeToPublicLinkUseCase, never()).invoke(any(), anyOrNull())
        val event = underTest.uiState.value.transferEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        val downloadEvent =
            (event as StateEventWithContentTriggered).content as TransferTriggerEvent.StartDownloadNode
        assertThat(downloadEvent.nodes).hasSize(1)
        assertThat(downloadEvent.nodes.first()).isInstanceOf(PublicLinkFile::class.java)
    }

    @Test
    fun `test that fileName is updated when current node is renamed`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        val renamedNode = mock<Node> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("renamed.txt")
        }
        whenever(monitorNodeUpdatesUseCase()).thenReturn(
            flowOf(NodeUpdate(mapOf(renamedNode to listOf(NodeChanges.Name))))
        )

        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "original.txt")
        advanceUntilIdle()

        assertThat(underTest.uiState.value.fileName).isEqualTo("renamed.txt")
    }

    @Test
    fun `test that fileName is unchanged when a different node is renamed`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        val otherNode = mock<Node> {
            on { id }.thenReturn(NodeId(2L))
            on { name }.thenReturn("other.txt")
        }
        whenever(monitorNodeUpdatesUseCase()).thenReturn(
            flowOf(NodeUpdate(mapOf(otherNode to listOf(NodeChanges.Name))))
        )

        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "original.txt")
        advanceUntilIdle()

        assertThat(underTest.uiState.value.fileName).isEqualTo("original.txt")
    }

    @Test
    fun `test that fileName is unchanged when current node update has no Name change`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        val sameNode = mock<Node> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("renamed.txt")
        }
        whenever(monitorNodeUpdatesUseCase()).thenReturn(
            flowOf(NodeUpdate(mapOf(sameNode to listOf(NodeChanges.Favourite))))
        )

        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "original.txt")
        advanceUntilIdle()

        assertThat(underTest.uiState.value.fileName).isEqualTo("original.txt")
    }

    @Test
    fun `test that chunks are split by character count when lines exceed CHUNK_MAX_CHARS`() =
        runTest {
            // 10 lines of 10K chars each = 100K total; with CHUNK_MAX_CHARS = 50K
            // they should split into more chunks than the line-only limit would produce
            val lines = (1..10).map { "x".repeat(10_000) }
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()

            // Line-only chunking would put all 10 lines in 1 chunk.
            // Char-count cap should split into at least 2 chunks.
            assertThat(underTest.getChunkCount()).isGreaterThan(1)
        }

    @Test
    fun `test that a long line is split across chunks`() = runTest {
        val longLine = "a".repeat(CHUNK_MAX_CHARS * 2)
        val normalLine = "normal"
        val original = listOf(normalLine, longLine, normalLine)
        doReturn(flowOf(original))
            .whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        // Long line is split mid-character, so more chunks than lines
        assertThat(underTest.getChunkCount()).isGreaterThan(original.size)
        // Every chunk respects the char limit
        for (i in 0 until underTest.getChunkCount()) {
            assertThat(underTest.getChunkText(i).length).isAtMost(CHUNK_MAX_CHARS)
        }
    }

    @Test
    fun `test that save round-trips correctly with long lines`() = runTest {
        val longLine = "b".repeat(CHUNK_MAX_CHARS * 2 + 500)
        val original = listOf("first", longLine, "last")
        val originalText = original.joinToString("\n")

        doReturn(flowOf(original)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        doReturn(TextEditorSaveResult.UploadRequired("tmp", 1L, true, false))
            .whenever(saveTextContentForTextEditorUseCase)
            .invoke(
                nodeHandle = any(),
                text = any(),
                fileName = any(),
                mode = any(),
                fromHome = any(),
                isFromSharedFolder = any(),
            )
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit, fileName = "test.txt")
        advanceUntilIdle()

        underTest.saveFile()
        advanceUntilIdle()

        val textCaptor = argumentCaptor<String>()
        verify(saveTextContentForTextEditorUseCase).invoke(
            nodeHandle = any(),
            text = textCaptor.capture(),
            fileName = any(),
            mode = any(),
            fromHome = any(),
            isFromSharedFolder = any(),
        )
        assertThat(textCaptor.firstValue).isEqualTo(originalText)
    }

    @Test
    fun `test that long lines are split into multiple chunks in view mode`() = runTest {
        val longLine = "c".repeat(CHUNK_MAX_CHARS + 1)
        val lines = (1..10).map { longLine }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        // Each line exceeds CHUNK_MAX_CHARS and is split, so more chunks than lines.
        assertThat(underTest.getChunkCount()).isGreaterThan(10)
        // Every chunk respects the char limit
        for (i in 0 until underTest.getChunkCount()) {
            assertThat(underTest.getChunkText(i).length).isAtMost(CHUNK_MAX_CHARS)
        }
    }

    @Test
    fun `test that edit mode splits long lines and each chunk respects char limit`() = runTest {
        val longLine = "d".repeat(CHUNK_MAX_CHARS * 2)
        val lines = listOf("short", longLine, "short2")
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        // Long line is split, so more than 3 chunks
        assertThat(underTest.getChunkCount()).isGreaterThan(3)
        for (i in 0 until underTest.getChunkCount()) {
            val chunkState = underTest.getOrCreateChunkState(i)
            assertThat(chunkState.text.length).isAtMost(CHUNK_MAX_CHARS)
        }
    }
}
