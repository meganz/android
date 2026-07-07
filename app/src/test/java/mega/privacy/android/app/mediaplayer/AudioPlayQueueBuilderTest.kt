package mega.privacy.android.app.mediaplayer

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.app.mediaplayer.mapper.AudioNodeToMediaItemMapper
import mega.privacy.android.app.mediaplayer.model.AudioPlayQueueParams
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.GetParentNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodeByHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByEmailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByHandlesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByParentHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromInSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromOutSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromPublicLinksUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudiosByParentHandleFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.LINKS_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioPlayQueueBuilderTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase = mock()
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase = mock()
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase =
        mock()
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase = mock()
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase = mock()
    private val getLocalLinkFromMegaApiUseCase: GetLocalLinkFromMegaApiUseCase = mock()
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase = mock()
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase =
        mock()
    private val getAudioNodeByHandleUseCase: GetAudioNodeByHandleUseCase = mock()
    private val getAudioNodesUseCase: GetAudioNodesUseCase = mock()
    private val getAudioNodesByParentHandleUseCase: GetAudioNodesByParentHandleUseCase = mock()
    private val getAudioNodesByHandlesUseCase: GetAudioNodesByHandlesUseCase = mock()
    private val getAudioNodesByEmailUseCase: GetAudioNodesByEmailUseCase = mock()
    private val getAudioNodesFromPublicLinksUseCase: GetAudioNodesFromPublicLinksUseCase = mock()
    private val getAudioNodesFromInSharesUseCase: GetAudioNodesFromInSharesUseCase = mock()
    private val getAudioNodesFromOutSharesUseCase: GetAudioNodesFromOutSharesUseCase = mock()
    private val getAudiosByParentHandleFromMegaApiFolderUseCase: GetAudiosByParentHandleFromMegaApiFolderUseCase =
        mock()
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase = mock()
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase = mock()
    private val getRootNodeFromMegaApiFolderUseCase: GetRootNodeFromMegaApiFolderUseCase = mock()
    private val getParentNodeFromMegaApiFolderUseCase: GetParentNodeFromMegaApiFolderUseCase =
        mock()
    private val hasCredentialsUseCase: HasCredentialsUseCase = mock()
    private val getFingerprintUseCase: GetFingerprintUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val audioNodeToMediaItemMapper: AudioNodeToMediaItemMapper = mock()

    private lateinit var underTest: AudioPlayQueueBuilder

    @BeforeEach
    fun setUp() = runTest(testDispatcher) {
        clearInvocations(megaApiHttpServerStartUseCase, megaApiFolderHttpServerStartUseCase)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
        whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
        whenever(getAudioNodesUseCase(any())).thenReturn(emptyList())
        whenever(getAudioNodesByHandlesUseCase(any())).thenReturn(emptyList())
        whenever(getAudioNodesFromPublicLinksUseCase(any())).thenReturn(emptyList())
        whenever(getAudioNodesFromInSharesUseCase(any())).thenReturn(emptyList())
        whenever(getAudioNodesFromOutSharesUseCase(any<Long>(), any<SortOrder>())).thenReturn(
            emptyList()
        )
        whenever(getOfflineNodesByParentIdUseCase(any<Int>(), anyOrNull())).thenReturn(emptyList())
        whenever(audioNodeToMediaItemMapper(any<Long>(), any<Uri>())).thenReturn(mock())

        underTest = AudioPlayQueueBuilder(
                ioDispatcher = testDispatcher,
                megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
                megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
                megaApiFolderHttpServerIsRunningUseCase = megaApiFolderHttpServerIsRunningUseCase,
                megaApiFolderHttpServerStartUseCase = megaApiFolderHttpServerStartUseCase,
                getLocalFilePathUseCase = getLocalFilePathUseCase,
                getLocalLinkFromMegaApiUseCase = getLocalLinkFromMegaApiUseCase,
                getLocalFolderLinkFromMegaApiUseCase = getLocalFolderLinkFromMegaApiUseCase,
                getLocalFolderLinkFromMegaApiFolderUseCase = getLocalFolderLinkFromMegaApiFolderUseCase,
                getAudioNodeByHandleUseCase = getAudioNodeByHandleUseCase,
                getAudioNodesUseCase = getAudioNodesUseCase,
                getAudioNodesByParentHandleUseCase = getAudioNodesByParentHandleUseCase,
                getAudioNodesByHandlesUseCase = getAudioNodesByHandlesUseCase,
                getAudioNodesByEmailUseCase = getAudioNodesByEmailUseCase,
                getAudioNodesFromPublicLinksUseCase = getAudioNodesFromPublicLinksUseCase,
                getAudioNodesFromInSharesUseCase = getAudioNodesFromInSharesUseCase,
                getAudioNodesFromOutSharesUseCase = getAudioNodesFromOutSharesUseCase,
                getAudiosByParentHandleFromMegaApiFolderUseCase = getAudiosByParentHandleFromMegaApiFolderUseCase,
                getOfflineNodesByParentIdUseCase = getOfflineNodesByParentIdUseCase,
                getRootNodeUseCase = getRootNodeUseCase,
                getRubbishNodeUseCase = getRubbishNodeUseCase,
                getBackupsNodeUseCase = getBackupsNodeUseCase,
                getRootNodeFromMegaApiFolderUseCase = getRootNodeFromMegaApiFolderUseCase,
                getParentNodeFromMegaApiFolderUseCase = getParentNodeFromMegaApiFolderUseCase,
                hasCredentialsUseCase = hasCredentialsUseCase,
                getFingerprintUseCase = getFingerprintUseCase,
                monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
                monitorAccountDetailUseCase = monitorAccountDetailUseCase,
                getBusinessStatusUseCase = getBusinessStatusUseCase,
                audioNodeToMediaItemMapper = audioNodeToMediaItemMapper,
            )
    }

    @Test
    fun `test that invoke emits first item with single media item and correct metadata`() =
        runTest {
            val params = buildParams(adapterType = AUDIO_BROWSE_ADAPTER, fileName = "song.mp3")

            underTest(params).test {
                val first = awaitItem()
                assertThat(first.mediaItems).hasSize(1)
                assertThat(first.newIndexForCurrentItem).isEqualTo(0)
                assertThat(first.nameToDisplay).isEqualTo("song.mp3")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke emits only one item when isPlayQueue is false`() = runTest {
        val params = buildParams(adapterType = AUDIO_BROWSE_ADAPTER, isPlayQueue = false)

        underTest(params).test {
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke does not start streaming server when adapter is OFFLINE_ADAPTER`() =
        runTest {
            val params = buildParams(adapterType = OFFLINE_ADAPTER, isPlayQueue = true)

            underTest(params).test { cancelAndIgnoreRemainingEvents() }

            verify(megaApiHttpServerStartUseCase, never()).invoke()
            verify(megaApiFolderHttpServerStartUseCase, never()).invoke()
        }

    @Test
    fun `test that invoke does not start streaming server when adapter is ZIP_ADAPTER`() =
        runTest {
            val params = buildParams(adapterType = ZIP_ADAPTER, isPlayQueue = true, zipPath = null)

            underTest(params).test { cancelAndIgnoreRemainingEvents() }

            verify(megaApiHttpServerStartUseCase, never()).invoke()
            verify(megaApiFolderHttpServerStartUseCase, never()).invoke()
        }

    @Test
    fun `test that invoke starts http server when adapter requires streaming and server is not running`() =
        runTest {
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(hasCredentialsUseCase()).thenReturn(true)
            val params = buildParams(adapterType = AUDIO_BROWSE_ADAPTER, isPlayQueue = true)

            underTest(params).test { cancelAndIgnoreRemainingEvents() }

            verify(megaApiHttpServerStartUseCase).invoke()
        }

    @Test
    fun `test that invoke does not start http server when server is already running`() = runTest {
        whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
        whenever(hasCredentialsUseCase()).thenReturn(true)
        val params = buildParams(adapterType = AUDIO_BROWSE_ADAPTER, isPlayQueue = true)

        underTest(params).test { cancelAndIgnoreRemainingEvents() }

        verify(megaApiHttpServerStartUseCase, never()).invoke()
    }

    @Test
    fun `test that invoke routes AUDIO_BROWSE_ADAPTER to getAudioNodesUseCase`() = runTest {
        val params = buildParams(adapterType = AUDIO_BROWSE_ADAPTER, isPlayQueue = true)

        underTest(params).test { cancelAndIgnoreRemainingEvents() }

        verify(getAudioNodesUseCase).invoke(params.sortOrder)
    }

    @Test
    fun `test that invoke routes LINKS_ADAPTER with invalid parent to getAudioNodesFromPublicLinksUseCase`() =
        runTest {
            val params = buildParams(
                adapterType = LINKS_ADAPTER,
                isPlayQueue = true,
                parentHandle = INVALID_HANDLE,
            )

            underTest(params).test { cancelAndIgnoreRemainingEvents() }

            verify(getAudioNodesFromPublicLinksUseCase).invoke(params.sortOrder)
        }

    @Test
    fun `test that invoke routes INCOMING_SHARES_ADAPTER with invalid parent to getAudioNodesFromInSharesUseCase`() =
        runTest {
            val params = buildParams(
                adapterType = INCOMING_SHARES_ADAPTER,
                isPlayQueue = true,
                parentHandle = INVALID_HANDLE,
            )

            underTest(params).test { cancelAndIgnoreRemainingEvents() }

            verify(getAudioNodesFromInSharesUseCase).invoke(params.sortOrder)
        }

    @Test
    fun `test that invoke routes OUTGOING_SHARES_ADAPTER with invalid parent to getAudioNodesFromOutSharesUseCase`() =
        runTest {
            val params = buildParams(
                adapterType = OUTGOING_SHARES_ADAPTER,
                isPlayQueue = true,
                parentHandle = INVALID_HANDLE,
            )

            underTest(params).test { cancelAndIgnoreRemainingEvents() }

            verify(getAudioNodesFromOutSharesUseCase).invoke(
                lastHandle = INVALID_HANDLE,
                order = params.sortOrder,
            )
        }

    @Test
    fun `test that invoke routes RECENTS_ADAPTER to getAudioNodesByHandlesUseCase`() = runTest {
        val handles = listOf(1L, 2L, 3L)
        val params = buildParams(
            adapterType = RECENTS_ADAPTER,
            isPlayQueue = true,
            handles = handles,
        )

        underTest(params).test { cancelAndIgnoreRemainingEvents() }

        verify(getAudioNodesByHandlesUseCase).invoke(handles)
    }

    @Test
    fun `test that invoke emits only first item when RECENTS_ADAPTER has null handles`() =
        runTest {
            val params = buildParams(
                adapterType = RECENTS_ADAPTER,
                isPlayQueue = true,
                handles = null,
            )

            underTest(params).test {
                awaitItem()
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke routes SEARCH_BY_ADAPTER to getAudioNodesByHandlesUseCase`() = runTest {
        val searchHandles = listOf(4L, 5L)
        val params = buildParams(
            adapterType = SEARCH_BY_ADAPTER,
            isPlayQueue = true,
            searchHandles = searchHandles,
        )

        underTest(params).test { cancelAndIgnoreRemainingEvents() }

        verify(getAudioNodesByHandlesUseCase).invoke(searchHandles)
    }

    @Test
    fun `test that invoke emits only first item when SEARCH_BY_ADAPTER has null searchHandles`() =
        runTest {
            val params = buildParams(
                adapterType = SEARCH_BY_ADAPTER,
                isPlayQueue = true,
                searchHandles = null,
            )

            underTest(params).test {
                awaitItem()
                awaitComplete()
            }
        }

    private fun buildParams(
        adapterType: Int = AUDIO_BROWSE_ADAPTER,
        handle: Long = 123L,
        fileName: String = "track.mp3",
        isPlayQueue: Boolean = false,
        parentHandle: Long = INVALID_HANDLE,
        handles: List<Long>? = null,
        searchHandles: List<Long>? = null,
        zipPath: String? = null,
    ) = AudioPlayQueueParams(
        adapterType = adapterType,
        handle = handle,
        fileName = fileName,
        uri = mock<Uri>(),
        parentHandle = parentHandle,
        isPlayQueue = isPlayQueue,
        offlineParentId = -1,
        zipPath = zipPath,
        contactEmail = null,
        handles = handles,
        searchHandles = searchHandles,
        sortOrder = SortOrder.ORDER_DEFAULT_ASC,
        needStopHttpServer = false,
    )
}
