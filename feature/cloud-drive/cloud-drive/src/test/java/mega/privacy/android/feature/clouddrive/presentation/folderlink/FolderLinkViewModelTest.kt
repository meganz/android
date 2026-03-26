package mega.privacy.android.feature.clouddrive.presentation.folderlink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.folderlink.FetchFolderNodesResult
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.model.NodeUiItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
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
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase = mock()
    private val setCloudSortOrderUseCase: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = NodeSortConfigurationUiMapper()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val setViewTypeUseCase: SetViewType = mock()

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
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            monitorViewTypeUseCase = monitorViewTypeUseCase,
            setViewTypeUseCase = setViewTypeUseCase,
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
            monitorSortCloudOrderUseCase,
            setCloudSortOrderUseCase,
            monitorViewTypeUseCase,
            setViewTypeUseCase,
        )
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
    }

    private fun mockFolderNode(id: Long = 1L, name: String = "folder"): TypedFolderNode = mock {
        on { this.id } doReturn NodeId(id)
        on { this.name } doReturn name
    }

    private fun mockFolderNodeUiItem(folder: TypedFolderNode): NodeUiItem<TypedNode> = mock {
        on { node } doReturn folder
    }

    private fun mockFileNode(id: Long = 2L, name: String = "file.txt"): TypedFileNode = mock {
        on { this.id } doReturn NodeId(id)
        on { this.name } doReturn name
    }

    private fun mockFileNodeUiItem(file: TypedFileNode): NodeUiItem<TypedNode> = mock {
        on { node } doReturn file
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
    fun `test that url is set in uiState when uriString is provided`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().url).isEqualTo(url)
        }
    }

    @Test
    fun `test that init does not call loginToFolderUseCase when uriString is null`() = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Loading)
        }
        verifyNoInteractions(loginToFolderUseCase)
    }

    @Test
    fun `test that init emits Loaded with hasCredentials true when login succeeds`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(true)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.contentState).isInstanceOf(FolderLinkContentState.Loaded::class.java)
            assertThat(state.hasCredentials).isTrue()
        }
    }

    @Test
    fun `test that init emits Loaded with hasCredentials false when login succeeds`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.contentState).isInstanceOf(FolderLinkContentState.Loaded::class.java)
            assertThat(state.hasCredentials).isFalse()
        }
    }

    @Test
    fun `test that init emits DecryptionKeyRequired when login returns API_INCOMPLETE`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.uiState.test {
            val decryptionState =
                awaitItem().contentState as FolderLinkContentState.DecryptionKeyRequired
            assertThat(decryptionState.url).isEqualTo(url)
            assertThat(decryptionState.isKeyIncorrect).isFalse()
        }
    }

    @Test
    fun `test that init emits DecryptionKeyRequired with isKeyIncorrect when login returns INCORRECT_KEY`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.INCORRECT_KEY)
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.uiState.test {
                val decryptionState =
                    awaitItem().contentState as FolderLinkContentState.DecryptionKeyRequired
                assertThat(decryptionState.url).isEqualTo(url)
                assertThat(decryptionState.isKeyIncorrect).isTrue()
            }
        }

    @Test
    fun `test that init emits Unavailable when login returns ERROR`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.ERROR)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Unavailable)
        }
    }

    @Test
    fun `test that init emits Unavailable when loginToFolderUseCase throws`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenThrow(RuntimeException())
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Unavailable)
        }
    }

    @Test
    fun `test that state becomes Expired when login succeeds but fetchNodes throws Expired`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenThrow(FetchFolderNodesException.Expired())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Expired)
            }
        }

    @Test
    fun `test that state becomes Unavailable when login succeeds but fetchNodes fails`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenThrow(RuntimeException())
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Unavailable)
        }
    }

    @Test
    fun `test that new format url with sub-handle passes sub-handle to fetchFolderNodesUseCase`() =
        runTest {
            val subHandle = "subHandle123"
            val url = "https://mega.nz/folder/abc#key!$subHandle"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(eq(subHandle), anyOrNull())).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            verify(fetchFolderNodesUseCase).invoke(eq(subHandle), anyOrNull())
        }

    @Test
    fun `test that old format url with sub-handle passes sub-handle to fetchFolderNodesUseCase`() =
        runTest {
            val subHandle = "subHandle123"
            val url = "https://mega.nz/#F!handle!key!$subHandle"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(eq(subHandle), anyOrNull())).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            verify(fetchFolderNodesUseCase).invoke(eq(subHandle), anyOrNull())
        }

    @Test
    fun `test that url without sub-handle passes null to fetchFolderNodesUseCase`() = runTest {
        val url = "https://mega.nz/folder/abc#key"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        verify(fetchFolderNodesUseCase).invoke(eq(null), anyOrNull())
    }

    @Test
    fun `test that DecryptionKeyDialogDismissed trigger back navigation event`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.DecryptionKeyDialogDismissed)

        underTest.uiState.test {
            assertThat(awaitItem().navigateBackEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that DecryptionKeyEntered appends key with hash and retries login for new format url`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            whenever(loginToFolderUseCase("$url#key")).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.DecryptionKeyEntered("key"))
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().contentState).isInstanceOf(FolderLinkContentState.Loaded::class.java)
            }
            verify(loginToFolderUseCase).invoke("$url#key")
        }

    @Test
    fun `test that DecryptionKeyEntered appends key with exclamation for old format url`() =
        runTest {
            val url = "https://mega.nz/#F!abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            whenever(loginToFolderUseCase("$url!key")).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
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
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
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
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
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
        whenever(getFolderLinkChildrenNodesUseCase(eq(42L), anyOrNull())).thenReturn(children)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(folder)))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.contentState).isInstanceOf(FolderLinkContentState.Loaded::class.java)
            assertThat(state.currentFolderNode).isEqualTo(folder)
            assertThat(state.title).isEqualTo(LocalizedText.Literal("SubFolder"))
            assertThat(state.items).isEqualTo(childUiItems)
        }
    }

    @Test
    fun `test that clicking a folder node emits Unavailable when getFolderLinkChildrenNodesUseCase fails`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            val folder = mockFolderNode(id = 42L)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            whenever(getFolderLinkChildrenNodesUseCase(eq(42L), anyOrNull())).thenThrow(RuntimeException())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(folder)))
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Unavailable)
            }
        }

    @Test
    fun `test that back press triggers navigateBackEvent when not in Loaded state`() = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.BackPressed)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().navigateBackEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that back press triggers navigateBackEvent when at root folder`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult()) // parentNode = null
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.BackPressed)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().navigateBackEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that back press navigates up to parent folder when in sub-folder`() = runTest {
        val url = "https://mega.nz/folder/abc"
        val subFolder = mockFolderNode(id = 10L, name = "SubFolder")
        val parentFolder = mockFolderNode(id = 1L, name = "Root")
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult(rootNode = parentFolder))
        stubNodeUiItemMapper()
        whenever(getFolderLinkChildrenNodesUseCase(eq(10L), anyOrNull())).thenReturn(emptyList())
        whenever(getFolderParentNodeUseCase(NodeId(10L))).thenReturn(parentFolder)
        whenever(getFolderLinkChildrenNodesUseCase(eq(1L), anyOrNull())).thenReturn(emptyList())
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(subFolder)))
        advanceUntilIdle()
        underTest.processAction(FolderLinkAction.BackPressed)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.contentState).isInstanceOf(FolderLinkContentState.Loaded::class.java)
            assertThat(state.currentFolderNode).isEqualTo(parentFolder)
            assertThat(state.title).isEqualTo(LocalizedText.Literal("Root"))
        }
    }

    @Test
    fun `test that back press triggers navigateBackEvent when getFolderParentNodeUseCase fails`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            val subFolder = mockFolderNode(id = 10L)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            whenever(getFolderLinkChildrenNodesUseCase(eq(10L), anyOrNull())).thenReturn(emptyList())
            whenever(getFolderParentNodeUseCase(NodeId(10L))).thenThrow(FetchFolderNodesException.GenericError())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(subFolder)))
            advanceUntilIdle()
            underTest.processAction(FolderLinkAction.BackPressed)
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().navigateBackEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that contentState is set to unavailable when children fails`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            val subFolder = mockFolderNode(id = 10L)
            val parentFolder = mockFolderNode(id = 1L)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
            stubNodeUiItemMapper()
            whenever(getFolderLinkChildrenNodesUseCase(eq(10L), anyOrNull())).thenReturn(emptyList())
            whenever(getFolderParentNodeUseCase(NodeId(10L))).thenReturn(parentFolder)
            whenever(getFolderLinkChildrenNodesUseCase(eq(1L), anyOrNull())).thenThrow(RuntimeException())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(subFolder)))
            advanceUntilIdle()
            underTest.processAction(FolderLinkAction.BackPressed)
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Unavailable)
            }
        }

    @Test
    fun `test that NavigateBackEventConsumed resets navigateBackEvent`() = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        underTest.uiState.test {
            awaitItem() // consume initial state

            underTest.processAction(FolderLinkAction.BackPressed)
            advanceUntilIdle()
            assertThat(awaitItem().navigateBackEvent).isEqualTo(triggered)

            underTest.processAction(FolderLinkAction.NavigateBackEventConsumed)
            assertThat(awaitItem().navigateBackEvent).isNotEqualTo(triggered)
        }
    }

    @Test
    fun `test that ItemClicked with a file node sets openedFileNode in state`() = runTest {
        val file = mockFileNode(id = 99L, name = "document.pdf")
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemClicked(mockFileNodeUiItem(file)))

        underTest.uiState.test {
            assertThat(awaitItem().openedFileNode).isEqualTo(file)
        }
    }

    @Test
    fun `test that OpenedFileNodeHandled clears openedFileNode from state`() = runTest {
        val file = mockFileNode(id = 99L, name = "document.pdf")
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        underTest.uiState.test {
            awaitItem() // consume initial state

            underTest.processAction(FolderLinkAction.ItemClicked(mockFileNodeUiItem(file)))
            assertThat(awaitItem().openedFileNode).isEqualTo(file)

            underTest.processAction(FolderLinkAction.OpenedFileNodeHandled)
            assertThat(awaitItem().openedFileNode).isNull()
        }
    }

    @Test
    fun `test that ItemClicked with a folder node does not set openedFileNode`() = runTest {
        val url = "https://mega.nz/folder/abc"
        val folder = mockFolderNode(id = 42L)
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        whenever(getFolderLinkChildrenNodesUseCase(eq(42L), anyOrNull())).thenReturn(emptyList())
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemClicked(mockFolderNodeUiItem(folder)))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().openedFileNode).isNull()
        }
    }

    @Test
    fun `test that sort order is updated in state when monitor emits a new order`() = runTest {
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_SIZE_ASC))
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(FolderLinkViewModel.Args(uriString = null))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedSortOrder).isEqualTo(SortOrder.ORDER_SIZE_ASC)
            assertThat(state.selectedSortConfiguration).isEqualTo(
                NodeSortConfiguration(NodeSortOption.Size, SortDirection.Ascending)
            )
        }
    }

    @Test
    fun `test that fetchNodes is called with the current sort order`() = runTest {
        val url = "https://mega.nz/folder/abc"
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_SIZE_ASC))
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull())).thenReturn(FetchFolderNodesResult())
        stubNodeUiItemMapper()
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        advanceUntilIdle()

        verify(fetchFolderNodesUseCase).invoke(anyOrNull(), eq(SortOrder.ORDER_SIZE_ASC))
    }

    @Test
    fun `test that SortOrderChanged calls setCloudSortOrderUseCase with the correct order`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(false)
            initViewModel(FolderLinkViewModel.Args(uriString = null))
            advanceUntilIdle()

            underTest.processAction(
                FolderLinkAction.SortOrderChanged(
                    NodeSortConfiguration(NodeSortOption.Size, SortDirection.Descending)
                )
            )
            advanceUntilIdle()

            verify(setCloudSortOrderUseCase).invoke(SortOrder.ORDER_SIZE_DESC)
        }

    @Test
    fun `test that currentViewType is updated in state when monitorViewType emits a new type`() =
        runTest {
            val viewTypeFlow = MutableStateFlow(ViewType.LIST)
            whenever(monitorViewTypeUseCase()).thenReturn(viewTypeFlow)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            initViewModel(FolderLinkViewModel.Args(uriString = null))
            advanceUntilIdle()

            viewTypeFlow.value = ViewType.GRID
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().currentViewType).isEqualTo(ViewType.GRID)
            }
        }

    @Test
    fun `test that ChangeViewTypeClicked calls setViewTypeUseCase with GRID when current view type is LIST`() =
        runTest {
            whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
            whenever(hasCredentialsUseCase()).thenReturn(false)
            initViewModel(FolderLinkViewModel.Args(uriString = null))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.ChangeViewTypeClicked)
            advanceUntilIdle()

            verify(setViewTypeUseCase).invoke(ViewType.GRID)
        }

    @Test
    fun `test that ChangeViewTypeClicked calls setViewTypeUseCase with LIST when current view type is GRID`() =
        runTest {
            whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
            whenever(hasCredentialsUseCase()).thenReturn(false)
            initViewModel(FolderLinkViewModel.Args(uriString = null))
            advanceUntilIdle()

            underTest.processAction(FolderLinkAction.ChangeViewTypeClicked)
            advanceUntilIdle()

            verify(setViewTypeUseCase).invoke(ViewType.LIST)
        }

    private fun createNodeUiItem(node: TypedNode): NodeUiItem<TypedNode> =
        NodeUiItem(node = node, isSelected = false)

    private suspend fun initLoadedViewModelWithItems(
        url: String = "https://mega.nz/folder/abc",
        nodes: List<TypedNode>,
    ): List<NodeUiItem<TypedNode>> {
        val items = nodes.map { createNodeUiItem(it) }
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
        whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull()))
            .thenReturn(FetchFolderNodesResult())
        whenever(
            nodeUiItemMapper(
                any(), anyOrNull(), any(), any(), any(), anyOrNull(), anyOrNull(), any()
            )
        ).thenReturn(items)
        initViewModel(FolderLinkViewModel.Args(uriString = url))
        return items
    }

    @Test
    fun `test that ItemLongClicked toggles item selection`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val node2 = mockFolderNode(id = 2L)
        val items = initLoadedViewModelWithItems(nodes = listOf(node1, node2))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemLongClicked(items[0]))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isInSelectionMode).isTrue()
            assertThat(state.items[0].isSelected).isTrue()
            assertThat(state.items[1].isSelected).isFalse()
        }
    }

    @Test
    fun `test that ItemClicked in selection mode toggles item selection`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val node2 = mockFolderNode(id = 2L)
        val items = initLoadedViewModelWithItems(nodes = listOf(node1, node2))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemLongClicked(items[0]))
        advanceUntilIdle()
        underTest.processAction(FolderLinkAction.ItemClicked(items[1]))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isInSelectionMode).isTrue()
            assertThat(state.items[0].isSelected).isTrue()
            assertThat(state.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that SelectAllItems selects all items`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val node2 = mockFolderNode(id = 2L)
        initLoadedViewModelWithItems(nodes = listOf(node1, node2))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.SelectAllItems)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isInSelectionMode).isTrue()
            assertThat(state.isAllSelected).isTrue()
            assertThat(state.items[0].isSelected).isTrue()
            assertThat(state.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that DeselectAllItems deselects all items`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val node2 = mockFolderNode(id = 2L)
        initLoadedViewModelWithItems(nodes = listOf(node1, node2))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.SelectAllItems)
        advanceUntilIdle()
        underTest.processAction(FolderLinkAction.DeselectAllItems)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isInSelectionMode).isFalse()
            assertThat(state.items[0].isSelected).isFalse()
            assertThat(state.items[1].isSelected).isFalse()
        }
    }

    @Test
    fun `test that toggling selection twice deselects item`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val items = initLoadedViewModelWithItems(nodes = listOf(node1))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemLongClicked(items[0]))
        advanceUntilIdle()
        val selectedItem = underTest.uiState.value.items[0]
        underTest.processAction(FolderLinkAction.ItemLongClicked(selectedItem))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isInSelectionMode).isFalse()
            assertThat(state.items[0].isSelected).isFalse()
        }
    }

    @Test
    fun `test that BackPressed deselects all when in selection mode`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val items = initLoadedViewModelWithItems(nodes = listOf(node1))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemLongClicked(items[0]))
        advanceUntilIdle()
        assertThat(underTest.uiState.value.isInSelectionMode).isTrue()

        underTest.processAction(FolderLinkAction.BackPressed)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isInSelectionMode).isFalse()
            assertThat(state.items[0].isSelected).isFalse()
        }
    }

    @Test
    fun `test that selectedItemsCount returns correct count`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val node2 = mockFolderNode(id = 2L)
        val items = initLoadedViewModelWithItems(nodes = listOf(node1, node2))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.selectedItemsCount).isEqualTo(0)

        underTest.processAction(FolderLinkAction.ItemLongClicked(items[0]))
        advanceUntilIdle()
        assertThat(underTest.uiState.value.selectedItemsCount).isEqualTo(1)

        underTest.processAction(FolderLinkAction.ItemLongClicked(items[1]))
        advanceUntilIdle()
        assertThat(underTest.uiState.value.selectedItemsCount).isEqualTo(2)
    }

    @Test
    fun `test that selectedNodes returns correct nodes`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val node2 = mockFolderNode(id = 2L)
        val items = initLoadedViewModelWithItems(nodes = listOf(node1, node2))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.selectedNodes).isEmpty()

        underTest.processAction(FolderLinkAction.ItemLongClicked(items[0]))
        advanceUntilIdle()

        val selectedNodes = underTest.uiState.value.selectedNodes
        assertThat(selectedNodes).hasSize(1)
        assertThat(selectedNodes[0].id).isEqualTo(NodeId(1L))
    }

    @Test
    fun `test that isAllSelected returns true when all items are selected`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val node2 = mockFolderNode(id = 2L)
        initLoadedViewModelWithItems(nodes = listOf(node1, node2))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.SelectAllItems)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.isAllSelected).isTrue()
    }

    @Test
    fun `test that isAllSelected returns false when not all items are selected`() = runTest {
        val node1 = mockFolderNode(id = 1L)
        val node2 = mockFolderNode(id = 2L)
        val items = initLoadedViewModelWithItems(nodes = listOf(node1, node2))
        advanceUntilIdle()

        underTest.processAction(FolderLinkAction.ItemLongClicked(items[0]))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.isAllSelected).isFalse()
    }

    @Test
    fun `test that refreshCurrentFolder re-fetches children with new sort order when monitor emits`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            val parentFolder = mockFolderNode(id = 5L)
            val sortOrderFlow = MutableStateFlow<SortOrder?>(SortOrder.ORDER_DEFAULT_ASC)
            whenever(monitorSortCloudOrderUseCase()).thenReturn(sortOrderFlow)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(fetchFolderNodesUseCase(anyOrNull(), anyOrNull()))
                .thenReturn(FetchFolderNodesResult(parentNode = parentFolder))
            stubNodeUiItemMapper()
            whenever(getFolderLinkChildrenNodesUseCase(eq(5L), anyOrNull())).thenReturn(emptyList())
            initViewModel(FolderLinkViewModel.Args(uriString = url))
            advanceUntilIdle()

            // Simulate the sort-order repository emitting a new value (triggered after setSortOrder persists)
            sortOrderFlow.value = SortOrder.ORDER_DEFAULT_DESC
            advanceUntilIdle()

            verify(getFolderLinkChildrenNodesUseCase).invoke(eq(5L), eq(SortOrder.ORDER_DEFAULT_DESC))
        }
}
