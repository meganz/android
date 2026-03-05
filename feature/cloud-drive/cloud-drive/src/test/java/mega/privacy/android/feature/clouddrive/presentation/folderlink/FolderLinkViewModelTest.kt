package mega.privacy.android.feature.clouddrive.presentation.folderlink

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.folderlink.FetchFolderNodesResult
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FolderLinkViewModelTest {

    private val loginToFolderUseCase: LoginToFolderUseCase = mock()
    private val hasCredentialsUseCase: HasCredentialsUseCase = mock()
    private val fetchFolderNodesUseCase: FetchFolderNodesUseCase = mock()
    private val getFolderLinkChildrenNodesUseCase: GetFolderLinkChildrenNodesUseCase = mock()
    private val getFolderParentNodeUseCase: GetFolderParentNodeUseCase = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()

    private lateinit var underTest: FolderLinkViewModel

    private fun initViewModel(
        args: FolderLinkViewModel.Args = FolderLinkViewModel.Args(uriString = null),
    ) {
        underTest = FolderLinkViewModel(
            loginToFolderUseCase = loginToFolderUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase,
            fetchFolderNodesUseCase = fetchFolderNodesUseCase,
            getFolderLinkChildrenNodesUseCase = getFolderLinkChildrenNodesUseCase,
            getFolderParentNodeUseCase = getFolderParentNodeUseCase,
            nodeUiItemMapper = nodeUiItemMapper,
            args = args,
        )
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            loginToFolderUseCase,
            hasCredentialsUseCase,
            fetchFolderNodesUseCase,
            getFolderLinkChildrenNodesUseCase,
            getFolderParentNodeUseCase,
            nodeUiItemMapper,
        )
    }

    private fun mockFolderNode(id: Long = 1L, name: String = "folder"): TypedFolderNode = mock {
        on { this.id } doReturn NodeId(id)
        on { this.name } doReturn name
    }

    private fun mockFolderNodeUiItem(folder: TypedFolderNode): NodeUiItem<TypedNode> = mock {
        on { node } doReturn folder
    }

    private suspend fun stubNodeUiItemMapper(result: List<NodeUiItem<TypedNode>> = emptyList()) {
        whenever(
            nodeUiItemMapper(
                any(),
                anyOrNull(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                any()
            )
        ).thenReturn(result)
    }

    @Test
    fun `test that init does not call loginToFolderUseCase when uriString is null`() = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.contentState).isEqualTo(FolderLinkContentState.Loading)
        verifyNoInteractions(loginToFolderUseCase)
    }

    @Test
    fun `test that init emits Loaded with hasCredentials true when login succeeds`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(true)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.contentState).isInstanceOf(FolderLinkContentState.Loaded::class.java)
        assertThat(underTest.uiState.value.hasCredentials).isTrue()
    }

    @Test
    fun `test that init emits Loaded with hasCredentials false when login succeeds`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.contentState).isInstanceOf(FolderLinkContentState.Loaded::class.java)
        assertThat(underTest.uiState.value.hasCredentials).isFalse()
    }

    @Test
    fun `test that init emits DecryptionKeyRequired when login returns API_INCOMPLETE`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        val state =
            underTest.uiState.value.contentState as FolderLinkContentState.DecryptionKeyRequired
        assertThat(state.url).isEqualTo(url)
        assertThat(state.isKeyIncorrect).isFalse()
    }

    @Test
    fun `test that init emits DecryptionKeyRequired with isKeyIncorrect when login returns INCORRECT_KEY`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.INCORRECT_KEY)
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            val state =
                underTest.uiState.value.contentState as FolderLinkContentState.DecryptionKeyRequired
            assertThat(state.url).isEqualTo(url)
            assertThat(state.isKeyIncorrect).isTrue()
        }

    @Test
    fun `test that init emits Unavailable when login returns ERROR`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.ERROR)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.contentState).isEqualTo(FolderLinkContentState.Unavailable)
    }

    @Test
    fun `test that init emits Unavailable when loginToFolderUseCase throws`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenThrow(RuntimeException())
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.contentState).isEqualTo(FolderLinkContentState.Unavailable)
    }

    @Test
    fun `test that state becomes Expired when login succeeds but fetchNodes throws Expired`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(null)).thenThrow(FetchFolderNodesException.Expired())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            assertThat(underTest.uiState.value.contentState).isEqualTo(FolderLinkContentState.Expired)
        }

    @Test
    fun `test that state becomes Unavailable when login succeeds but fetchNodes fails`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(null)).thenThrow(RuntimeException())
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.contentState).isEqualTo(FolderLinkContentState.Unavailable)
    }

    @Test
    fun `test that new format url with sub-handle passes sub-handle to fetchFolderNodesUseCase`() =
        runTest {
            val subHandle = "subHandle123"
            val url = "https://mega.nz/folder/abc#key!$subHandle"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(subHandle)).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            verify(fetchFolderNodesUseCase).invoke(subHandle)
        }

    @Test
    fun `test that old format url with sub-handle passes sub-handle to fetchFolderNodesUseCase`() =
        runTest {
            val subHandle = "subHandle123"
            val url = "https://mega.nz/#F!handle!key!$subHandle"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(subHandle)).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            verify(fetchFolderNodesUseCase).invoke(subHandle)
        }

    @Test
    fun `test that url without sub-handle passes null to fetchFolderNodesUseCase`() = runTest {
        val url = "https://mega.nz/folder/abc#key"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        verify(fetchFolderNodesUseCase).invoke(null)
    }

    @Test
    fun `test that DecryptionKeyDialogDismissed updates state to Unavailable`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.DecryptionKeyDialogDismissed)

        assertThat(underTest.uiState.value.contentState).isEqualTo(FolderLinkContentState.Unavailable)
    }

    @Test
    fun `test that DecryptionKeyEntered appends key with hash and retries login for new format url`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            whenever(loginToFolderUseCase("$url#key")).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.DecryptionKeyEntered("key"))
            advanceUntilIdle()

            assertThat(underTest.uiState.value.contentState).isInstanceOf(FolderLinkContentState.Loaded::class.java)
            verify(loginToFolderUseCase).invoke("$url#key")
        }

    @Test
    fun `test that DecryptionKeyEntered appends key with exclamation for old format url`() =
        runTest {
            val url = "https://mega.nz/#F!abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            whenever(loginToFolderUseCase("$url!key")).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.DecryptionKeyEntered("key"))
            advanceUntilIdle()

            verify(loginToFolderUseCase).invoke("$url!key")
        }

    @Test
    fun `test that DecryptionKeyEntered trims whitespace from key`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
        whenever(loginToFolderUseCase("$url#key")).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.DecryptionKeyEntered("  key  "))
        advanceUntilIdle()

        verify(loginToFolderUseCase).invoke("$url#key")
    }

    @Test
    fun `test that clicking a folder node updates Loaded state with folder content`() = runTest {
        val url = "https://mega.nz/folder/abc"
        val folder = mockFolderNode(id = 42L, name = "SubFolder")
        val children = listOf<TypedNode>(mock())
        val childUiItems = listOf<NodeUiItem<TypedNode>>(mock())
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
        whenever(
            nodeUiItemMapper(
                any(),
                anyOrNull(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                any()
            )
        ).thenReturn(emptyList()).thenReturn(childUiItems)
        whenever(getFolderLinkChildrenNodesUseCase(42L, null)).thenReturn(children)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(folder)))
        advanceUntilIdle()

        val loaded = underTest.uiState.value.contentState as FolderLinkContentState.Loaded
        assertThat(underTest.uiState.value.currentFolderNode).isEqualTo(folder)
        assertThat(underTest.uiState.value.title).isEqualTo(LocalizedText.Literal("SubFolder"))
        assertThat(loaded.items).isEqualTo(childUiItems)
    }

    @Test
    fun `test that clicking a folder node emits Unavailable when getFolderLinkChildrenNodesUseCase fails`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            val folder = mockFolderNode(id = 42L)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            whenever(getFolderLinkChildrenNodesUseCase(42L, null)).thenThrow(RuntimeException())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(folder)))
            advanceUntilIdle()

            assertThat(underTest.uiState.value.contentState).isEqualTo(FolderLinkContentState.Unavailable)
        }

    @Test
    fun `test that back press triggers navigateBackEvent when not in Loaded state`() = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.BackPressed)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.navigateBackEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that back press triggers navigateBackEvent when at root folder`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult()) // parentNode = null
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.BackPressed)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.navigateBackEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that back press navigates up to parent folder when in sub-folder`() = runTest {
        val url = "https://mega.nz/folder/abc"
        val subFolder = mockFolderNode(id = 10L, name = "SubFolder")
        val parentFolder = mockFolderNode(id = 1L, name = "Root")
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult(rootNode = parentFolder))
        stubNodeUiItemMapper()
        whenever(getFolderLinkChildrenNodesUseCase(10L, null)).thenReturn(emptyList())
        whenever(getFolderParentNodeUseCase(NodeId(10L))).thenReturn(parentFolder)
        whenever(getFolderLinkChildrenNodesUseCase(1L, null)).thenReturn(emptyList())
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(subFolder)))
        advanceUntilIdle()
        underTest.processAction(FolderLinkAction.BackPressed)
        advanceUntilIdle()

        val loaded = underTest.uiState.value.contentState as FolderLinkContentState.Loaded
        assertThat(underTest.uiState.value.currentFolderNode).isEqualTo(parentFolder)
        assertThat(underTest.uiState.value.title).isEqualTo(LocalizedText.Literal("Root"))
    }

    @Test
    fun `test that back press triggers navigateBackEvent when getFolderParentNodeUseCase fails`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            val subFolder = mockFolderNode(id = 10L)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            whenever(getFolderLinkChildrenNodesUseCase(10L, null)).thenReturn(emptyList())
            whenever(getFolderParentNodeUseCase(NodeId(10L))).thenThrow(FetchFolderNodesException.GenericError())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(subFolder)))
            advanceUntilIdle()
            underTest.processAction(FolderLinkAction.BackPressed)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.navigateBackEvent).isEqualTo(triggered)
        }

    @Test
    fun `test that contentState is set to unavailable when children fails`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            val subFolder = mockFolderNode(id = 10L)
            val parentFolder = mockFolderNode(id = 1L)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(null)).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            whenever(getFolderLinkChildrenNodesUseCase(10L, null)).thenReturn(emptyList())
            whenever(getFolderParentNodeUseCase(NodeId(10L))).thenReturn(parentFolder)
            whenever(getFolderLinkChildrenNodesUseCase(1L, null)).thenThrow(RuntimeException())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(subFolder)))
            advanceUntilIdle()
            underTest.processAction(FolderLinkAction.BackPressed)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.contentState).isEqualTo(FolderLinkContentState.Unavailable)
        }

    @Test
    fun `test that NavigateBackEventConsumed resets navigateBackEvent`() = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.BackPressed)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.navigateBackEvent).isEqualTo(triggered)

        underTest.processAction(FolderLinkAction.NavigateBackEventConsumed)

        assertThat(underTest.uiState.value.navigateBackEvent).isNotEqualTo(triggered)
    }
}
