package mega.privacy.android.feature.clouddrive.presentation.filelink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import kotlin.time.Duration.Companion.seconds
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.advertisements.QueryAdsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.viewedlinks.RemoveViewedLinkByUrlUseCase
import mega.privacy.android.domain.usecase.viewedlinks.SaveViewedLinkUseCase
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkAction
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkContentState
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
    private val removeViewedLinkByUrlUseCase: RemoveViewedLinkByUrlUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val queryAdsUseCase: QueryAdsUseCase = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val durationInSecondsTextMapper = DurationInSecondsTextMapper()

    private lateinit var underTest: FileLinkViewModel

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            getPublicNodeUseCase,
            hasCredentialsUseCase,
            saveViewedLinkUseCase,
            removeViewedLinkByUrlUseCase,
            getFeatureFlagValueUseCase,
            queryAdsUseCase,
            fileTypeIconMapper,
        )
    }

    private fun initViewModel(uriString: String? = null) {
        underTest = FileLinkViewModel(
            getPublicNodeUseCase = getPublicNodeUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase,
            saveViewedLinkUseCase = saveViewedLinkUseCase,
            removeViewedLinkByUrlUseCase = removeViewedLinkByUrlUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            queryAdsUseCase = queryAdsUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            args = FileLinkViewModel.Args(uriString = uriString),
        )
    }

    private fun mockFileNode(
        id: Long = 1L,
        name: String = "file.txt",
        size: Long = 1024L,
    ): TypedFileNode {
        val info = UnknownFileTypeInfo(
            mimeType = "",
            extension = name.split('.').last()
        )
        return mock {
            on { this.id } doReturn NodeId(id)
            on { this.name } doReturn name
            on { this.size } doReturn size
            on { this.type } doReturn info
        }
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
    fun `test that formattedDuration is set when fetched node is a video file`() = runTest {
        val url = "https://mega.nz/file/abc#key"
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(7L)
            on { name } doReturn "Hobbiton.mp4"
            on { size } doReturn 647_000_000L
            on { type } doReturn VideoFileTypeInfo(
                mimeType = "video/mp4",
                extension = "mp4",
                duration = 170.seconds,
            )
        }
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenReturn(node)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.contentState).isInstanceOf(FileLinkContentState.Loaded::class.java)
            val loaded = state.contentState as FileLinkContentState.Loaded
            assertThat(loaded.formattedDuration).isEqualTo("2:50")
            assertThat(loaded.isVideo).isTrue()
        }
    }

    @Test
    fun `test that formattedDuration is set when fetched node is an audio file`() = runTest {
        val url = "https://mega.nz/file/abc#key"
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(8L)
            on { name } doReturn "Song.mp3"
            on { size } doReturn 5_000_000L
            on { type } doReturn AudioFileTypeInfo(
                mimeType = "audio/mpeg",
                extension = "mp3",
                duration = 245.seconds,
            )
        }
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenReturn(node)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.contentState).isInstanceOf(FileLinkContentState.Loaded::class.java)
            val loaded = state.contentState as FileLinkContentState.Loaded
            assertThat(loaded.formattedDuration).isEqualTo("4:05")
            assertThat(loaded.isVideo).isFalse()
        }
    }

    @Test
    fun `test that formattedDuration is null when fetched node is not a media file`() = runTest {
        val url = "https://mega.nz/file/abc#key"
        val node = mockFileNode()
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenReturn(node)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.contentState).isInstanceOf(FileLinkContentState.Loaded::class.java)
            val loaded = state.contentState as FileLinkContentState.Loaded
            assertThat(loaded.formattedDuration).isNull()
        }
    }

    @Test
    fun `test that init emits Loaded with fileNode wrapped in PublicLinkFile and iconRes when getPublicNode succeeds`() =
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
                assertThat(state.fileNode).isInstanceOf(PublicLinkFile::class.java)
                assertThat(state.fileNode?.node).isEqualTo(node)
                assertThat(state.fileNode?.parent).isNull()
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

    @Disabled("Stubbing is incorrect. Possible need to convert icon extension to mapper for mocking")
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

    @Disabled("Stubbing is incorrect. Possible need to convert icon extension to mapper for mocking")
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
                type = RecentlyViewedLinkType.FileLink,
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
    fun `test that viewed link is removed when getPublicNode throws and ViewedLinks flag is enabled`() =
        runTest {
            val url = "https://mega.nz/file/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url)).thenThrow(PublicNodeException.GenericError())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(true)
            initViewModel(uriString = url)

            verify(removeViewedLinkByUrlUseCase).invoke(url)
        }

    @Test
    fun `test that viewed link is removed when DecryptionKeyRequired exception is thrown and ViewedLinks flag is enabled`() =
        runTest {
            val url = "https://mega.nz/file/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url))
                .thenThrow(PublicNodeException.DecryptionKeyRequired())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(true)
            initViewModel(uriString = url)

            verify(removeViewedLinkByUrlUseCase).invoke(url)
        }

    @Test
    fun `test that viewed link is not removed when getPublicNode throws but ViewedLinks flag is disabled`() =
        runTest {
            val url = "https://mega.nz/file/abc"
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url)).thenThrow(PublicNodeException.GenericError())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
            initViewModel(uriString = url)

            verify(removeViewedLinkByUrlUseCase, never()).invoke(any())
        }

    @Test
    fun `test that viewed link is not removed when getPublicNode succeeds`() = runTest {
        val url = "https://mega.nz/file/abc#key"
        val node = mockFileNode()
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenReturn(node)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(true)
        initViewModel(uriString = url)

        verify(removeViewedLinkByUrlUseCase, never()).invoke(any())
    }

    @Test
    fun `test that shouldShowAdsForLink reflects queryAdsUseCase result when getPublicNode succeeds`() =
        runTest {
            val url = "https://mega.nz/file/abc#key"
            val node = mockFileNode(id = 99L)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getPublicNodeUseCase(url)).thenReturn(node)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
            whenever(queryAdsUseCase(99L)).thenReturn(true)
            initViewModel(uriString = url)
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().shouldShowAdsForLink).isTrue()
            }
        }

    @Test
    fun `test that shouldShowAdsForLink is false when queryAdsUseCase returns false`() = runTest {
        val url = "https://mega.nz/file/abc#key"
        val node = mockFileNode(id = 99L)
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenReturn(node)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)).thenReturn(false)
        whenever(queryAdsUseCase(99L)).thenReturn(false)
        initViewModel(uriString = url)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().shouldShowAdsForLink).isFalse()
        }
    }

    @Test
    fun `test that queryAdsUseCase is not called when getPublicNode fails`() = runTest {
        val url = "https://mega.nz/file/abc"
        whenever(hasCredentialsUseCase()).thenReturn(false)
        whenever(getPublicNodeUseCase(url)).thenThrow(PublicNodeException.GenericError())
        initViewModel(uriString = url)
        advanceUntilIdle()

        verifyNoInteractions(queryAdsUseCase)
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
