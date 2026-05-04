package mega.privacy.android.feature.clouddrive.presentation.filelink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.viewedlinks.SaveViewedLinkUseCase
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkAction
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkContentState
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileLinkViewModelTest {

    private val getPublicNodeUseCase: GetPublicNodeUseCase = mock()
    private val hasCredentialsUseCase: HasCredentialsUseCase = mock()
    private val saveViewedLinkUseCase: SaveViewedLinkUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()

    private lateinit var underTest: FileLinkViewModel

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            getPublicNodeUseCase,
            hasCredentialsUseCase,
            saveViewedLinkUseCase,
            getFeatureFlagValueUseCase,
            fileTypeIconMapper,
        )
    }

    private fun initViewModel(uriString: String? = null) {
        underTest = FileLinkViewModel(
            getPublicNodeUseCase = getPublicNodeUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase,
            saveViewedLinkUseCase = saveViewedLinkUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            args = FileLinkViewModel.Args(uriString = uriString),
        )
    }

    private fun mockFileNode(
        id: Long = 1L,
        name: String = "file.txt",
        size: Long = 1024L,
    ): TypedFileNode = mock {
        on { this.id } doReturn NodeId(id)
        on { this.name } doReturn name
        on { this.size } doReturn size
    }

    @Test
    fun `test that url is set in uiState when uriString is provided`() = runTest {
        val url = "https://mega.nz/file/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenThrow(PublicNodeException.GenericError())
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().url).isEqualTo(url)
        }
    }

    @Test
    fun `test that init does not call getPublicNodeUseCase when uriString is null`() = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(false)
        initViewModel(uriString = null)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(FileLinkContentState.Loading)
        }
        verifyNoInteractions(getPublicNodeUseCase)
    }

    @Test
    fun `test that init emits Loaded with fileNode and iconRes when getPublicNode succeeds`() =
        runTest {
            val url = "https://mega.nz/file/abc#key"
            val node = mockFileNode()
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getPublicNodeUseCase(url)).thenReturn(node)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
            initViewModel(uriString = url)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.contentState).isInstanceOf(FileLinkContentState.Loaded::class.java)
                val loaded = state.contentState as FileLinkContentState.Loaded
                assertThat(loaded.fileNode).isEqualTo(node)
                assertThat(state.hasCredentials).isTrue()
            }
        }

    @Test
    fun `test that init emits DecryptionKeyRequired when DecryptionKeyRequired exception is thrown`() =
        runTest {
            val url = "https://mega.nz/file/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url))
                .thenThrow(PublicNodeException.DecryptionKeyRequired())
            initViewModel(uriString = url)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.contentState).isInstanceOf(
                    FileLinkContentState.DecryptionKeyRequired::class.java
                )
                val keyState = state.contentState as FileLinkContentState.DecryptionKeyRequired
                assertThat(keyState.url).isEqualTo(url)
                assertThat(keyState.isKeyIncorrect).isFalse()
            }
        }

    @Test
    fun `test that init emits Unavailable when InvalidDecryptionKey exception is thrown without prior decryption`() =
        runTest {
            val url = "https://mega.nz/file/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url))
                .thenThrow(PublicNodeException.InvalidDecryptionKey())
            initViewModel(uriString = url)
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().contentState).isEqualTo(FileLinkContentState.Unavailable)
            }
        }

    @Test
    fun `test that init emits Expired when Expired exception is thrown`() = runTest {
        val url = "https://mega.nz/file/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenThrow(PublicNodeException.Expired())
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(FileLinkContentState.Expired)
        }
    }

    @Test
    fun `test that init emits Unavailable when GenericError exception is thrown`() = runTest {
        val url = "https://mega.nz/file/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenThrow(PublicNodeException.GenericError())
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(FileLinkContentState.Unavailable)
        }
    }

    @Test
    fun `test that DecryptionKeyEntered combines url and key with hash for new file link format`() =
        runTest {
            val url = "https://mega.nz/file/abc"
            val key = "decryptionKey"
            val combined = "$url#$key"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url))
                .thenThrow(PublicNodeException.DecryptionKeyRequired())
            whenever(getPublicNodeUseCase(combined)).thenReturn(mockFileNode())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
            initViewModel(uriString = url)
            advanceUntilIdle()

            underTest.processAction(FileLinkAction.DecryptionKeyEntered(key))
            advanceUntilIdle()

            verify(getPublicNodeUseCase).invoke(combined)
            underTest.uiState.test {
                assertThat(awaitItem().contentState)
                    .isInstanceOf(FileLinkContentState.Loaded::class.java)
            }
        }

    @Test
    fun `test that DecryptionKeyEntered combines url and key with bang for old file link format`() =
        runTest {
            val url = "https://mega.nz/#!abc"
            val key = "decryptionKey"
            val combined = "$url!$key"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url))
                .thenThrow(PublicNodeException.DecryptionKeyRequired())
            whenever(getPublicNodeUseCase(combined)).thenReturn(mockFileNode())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
            initViewModel(uriString = url)
            advanceUntilIdle()

            underTest.processAction(FileLinkAction.DecryptionKeyEntered(key))
            advanceUntilIdle()

            verify(getPublicNodeUseCase).invoke(combined)
        }

    @Test
    fun `test that DecryptionKeyEntered with invalid key emits DecryptionKeyRequired with isKeyIncorrect true`() =
        runTest {
            val url = "https://mega.nz/file/abc"
            val key = "wrongKey"
            val combined = "$url#$key"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url))
                .thenThrow(PublicNodeException.DecryptionKeyRequired())
            whenever(getPublicNodeUseCase(combined))
                .thenThrow(PublicNodeException.InvalidDecryptionKey())
            initViewModel(uriString = url)
            advanceUntilIdle()

            underTest.processAction(FileLinkAction.DecryptionKeyEntered(key))
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.contentState).isInstanceOf(
                    FileLinkContentState.DecryptionKeyRequired::class.java
                )
                val keyState = state.contentState as FileLinkContentState.DecryptionKeyRequired
                assertThat(keyState.isKeyIncorrect).isTrue()
            }
        }

    @Test
    fun `test that DecryptionKeyEntered ignores blank keys`() = runTest {
        val url = "https://mega.nz/file/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url))
            .thenThrow(PublicNodeException.DecryptionKeyRequired())
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.processAction(FileLinkAction.DecryptionKeyEntered("   "))
        advanceUntilIdle()

        verify(getPublicNodeUseCase, never()).invoke("$url#")
    }

    @Test
    fun `test that DecryptionKeyDialogDismissed resets contentState to Loading`() = runTest {
        val url = "https://mega.nz/file/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url))
            .thenThrow(PublicNodeException.DecryptionKeyRequired())
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.processAction(FileLinkAction.DecryptionKeyDialogDismissed)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(FileLinkContentState.Loading)
        }
    }

    @Test
    fun `test that viewed link is saved when ViewedLinks feature flag is enabled`() = runTest {
        val url = "https://mega.nz/file/abc#key"
        val node = mockFileNode(id = 42L, name = "doc.pdf")
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenReturn(node)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(true)
        initViewModel(uriString = url)
        advanceUntilIdle()

        verify(saveViewedLinkUseCase).invoke(
            ViewedLink(
                nodeHandle = 42L,
                name = "doc.pdf",
                linkUrl = url,
                type = RecentlyUsedType.FileLink,
                accessedTimestamp = null,
            )
        )
    }

    @Test
    fun `test that viewed link is not saved when ViewedLinks feature flag is disabled`() = runTest {
        val url = "https://mega.nz/file/abc#key"
        val node = mockFileNode()
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenReturn(node)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
        initViewModel(uriString = url)
        advanceUntilIdle()

        verify(saveViewedLinkUseCase, never()).invoke(any())
    }

    @Test
    fun `test that hasCredentials reflects the use case result on init`() = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(true)
        initViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().hasCredentials).isTrue()
        }
    }

}
