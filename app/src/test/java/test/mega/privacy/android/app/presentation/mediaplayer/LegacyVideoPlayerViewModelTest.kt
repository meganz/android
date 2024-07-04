package test.mega.privacy.android.app.presentation.mediaplayer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.LegacyVideoPlayerViewModel
import mega.privacy.android.app.mediaplayer.LegacyVideoPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
import mega.privacy.android.app.mediaplayer.LegacyVideoPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_MATCHED_ITEM
import mega.privacy.android.app.mediaplayer.LegacyVideoPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_OFF
import mega.privacy.android.app.mediaplayer.model.SubtitleDisplayState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.MonitorVideoRepeatModeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.wheneverBlocking
import test.mega.privacy.android.app.TimberJUnit5Extension
import test.mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension

@ExtendWith(
    value = [
        CoroutineMainDispatcherExtension::class,
        InstantTaskExecutorExtension::class,
        TimberJUnit5Extension::class
    ]
)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LegacyVideoPlayerViewModelTest {
    private lateinit var underTest: LegacyVideoPlayerViewModel
    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val savedStateHandle = SavedStateHandle(mapOf())
    private val monitorVideoRepeatModeUseCase = mock<MonitorVideoRepeatModeUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getOfflineNodesByParentIdUseCase = mock<GetOfflineNodesByParentIdUseCase>()
    private val getOfflineNodeInformationByIdUseCase = mock<GetOfflineNodeInformationByIdUseCase>()
    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        on {
            runBlocking { invoke() }
        }.thenReturn(false)
    }

    private val expectedId = 123456L
    private val expectedName = "testName"
    private val expectedUrl = "test url"

    @BeforeEach
    fun setUp() {
        reset(monitorVideoRepeatModeUseCase, monitorTransferEventsUseCase)
        initViewModel()
    }

    private fun initViewModel() {
        wheneverBlocking { monitorVideoRepeatModeUseCase() }.thenReturn(emptyFlow())
        underTest = LegacyVideoPlayerViewModel(
            context = mock(),
            mediaPlayerGateway = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
            offlineThumbnailFileWrapper = mock(),
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            playlistItemMapper = mock(),
            trackPlaybackPositionUseCase = mock(),
            monitorPlaybackTimesUseCase = mock(),
            savePlaybackTimesUseCase = mock(),
            deletePlaybackInformationUseCase = mock(),
            megaApiFolderHttpServerIsRunningUseCase = mock(),
            megaApiFolderHttpServerStartUseCase = mock(),
            megaApiFolderHttpServerStopUseCase = mock(),
            megaApiHttpServerIsRunningUseCase = mock(),
            megaApiHttpServerStartUseCase = mock(),
            megaApiHttpServerStop = mock(),
            hasCredentialsUseCase = mock(),
            getLocalFilePathUseCase = mock(),
            getLocalFolderLinkFromMegaApiFolderUseCase = mock(),
            getLocalFolderLinkFromMegaApiUseCase = mock(),
            getLocalLinkFromMegaApiUseCase = mock(),
            getBackupsNodeUseCase = mock(),
            getParentNodeFromMegaApiFolderUseCase = mock(),
            getRootNodeUseCase = mock(),
            getRootNodeFromMegaApiFolderUseCase = mock(),
            getRubbishNodeUseCase = mock(),
            getVideoNodeByHandleUseCase = mock(),
            getVideoNodesFromPublicLinksUseCase = mock(),
            getVideoNodesFromInSharesUseCase = mock(),
            getVideoNodesFromOutSharesUseCase = mock(),
            getVideoNodesUseCase = mock(),
            getVideoNodesByEmailUseCase = mock(),
            getUserNameByEmailUseCase = mock(),
            getVideosByParentHandleFromMegaApiFolderUseCase = mock(),
            getVideoNodesByParentHandleUseCase = mock(),
            getVideoNodesByHandlesUseCase = mock(),
            getFingerprintUseCase = mock(),
            getSRTSubtitleFileListUseCase = mock(),
            setVideoRepeatModeUseCase = mock(),
            getVideosBySearchTypeUseCase = mock(),
            savedStateHandle = savedStateHandle,
            monitorVideoRepeatModeUseCase = monitorVideoRepeatModeUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase = mock(),
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getOfflineNodesByParentIdUseCase = getOfflineNodesByParentIdUseCase,
            getThumbnailUseCase = getThumbnailUseCase,
            getOfflineNodeInformationByIdUseCase = getOfflineNodeInformationByIdUseCase,
        )
        savedStateHandle[underTest.subtitleDialogShowKey] = false
        savedStateHandle[underTest.subtitleShowKey] = false
        savedStateHandle[underTest.videoPlayerPausedForPlaylistKey] = false
        savedStateHandle[underTest.currentSubtitleFileInfoKey] = null
    }

    @Test
    internal fun `test that the initial state is returned`() = runTest {
        val expectedState = SubtitleDisplayState()
        underTest.uiState.test {
            assertThat(awaitItem().subtitleDisplayState).isEqualTo(expectedState)
        }
    }

    @Test
    internal fun `test that showAddSubtitleDialog function is invoked`() = runTest {
        underTest.showAddSubtitleDialog()
        underTest.uiState.test {
            val actual = awaitItem().subtitleDisplayState
            assertThat(actual.isSubtitleShown).isTrue()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isTrue()
        }
    }

    @Test
    internal fun `test that onAddedSubtitleOptionClicked function is invoked`() = runTest {
        underTest.onAddedSubtitleOptionClicked()
        underTest.uiState.test {
            val actual = awaitItem().subtitleDisplayState
            assertThat(actual.isSubtitleShown).isTrue()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
        assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM)
    }

    @Test
    internal fun `test that onAddSubtitleFile function is invoked when info is not null`() =
        runTest {
            underTest.onAddSubtitleFile(
                SubtitleFileInfo(
                    id = expectedId,
                    name = expectedName,
                    url = expectedUrl,
                    parentName = null,
                    isMarkedSensitive = false,
                    isSensitiveInherited = false,
                )
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem().subtitleDisplayState
                assertThat(actual.isSubtitleShown).isTrue()
                assertThat(actual.isAddSubtitle).isTrue()
                assertThat(actual.subtitleFileInfo?.id).isEqualTo(expectedId)
                assertThat(actual.subtitleFileInfo?.name).isEqualTo(expectedName)
                assertThat(actual.isSubtitleDialogShown).isFalse()
            }
            assertThat(underTest.selectOptionState).isEqualTo(
                SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
            )
        }

    @Test
    internal fun `test that onAddSubtitleFile function is invoked when info is null`() = runTest {
        underTest.onAddSubtitleFile(null)
        underTest.uiState.test {
            val actual = awaitItem().subtitleDisplayState
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
        assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_OFF)
    }

    @Test
    internal fun `test that onAddSubtitleFile function is invoked and state is reset when info is null`() =
        runTest {
            underTest.onAddSubtitleFile(
                SubtitleFileInfo(
                    id = expectedId,
                    name = expectedName,
                    url = expectedUrl,
                    parentName = null,
                    isMarkedSensitive = false,
                    isSensitiveInherited = false,
                )
            )
            assertThat(underTest.selectOptionState).isEqualTo(
                SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
            )
            underTest.onAddSubtitleFile(null, true)
            underTest.uiState.test {
                val actual = awaitItem().subtitleDisplayState
                assertThat(actual.isSubtitleShown).isFalse()
                assertThat(actual.isAddSubtitle).isFalse()
                assertThat(actual.isSubtitleDialogShown).isFalse()
            }
            assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_OFF)
        }

    @Test
    internal fun `test that onAutoMatchItemClicked function is invoked`() = runTest {
        underTest.onAutoMatchItemClicked(
            SubtitleFileInfo(
                id = expectedId,
                name = expectedName,
                url = expectedUrl,
                parentName = null,
                isMarkedSensitive = false,
                isSensitiveInherited = false,
            )
        )
        advanceUntilIdle()
        underTest.uiState.test {
            val actual = awaitItem().subtitleDisplayState
            assertThat(actual.isSubtitleShown).isTrue()
            assertThat(actual.isAddSubtitle).isTrue()
            assertThat(actual.subtitleFileInfo?.id).isEqualTo(expectedId)
            assertThat(actual.subtitleFileInfo?.name).isEqualTo(expectedName)
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
        assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_MATCHED_ITEM)
    }

    @Test
    internal fun `test that onOffItemClicked function is invoked`() = runTest {
        underTest.onOffItemClicked()
        underTest.uiState.test {
            val actual = awaitItem().subtitleDisplayState
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
        assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_OFF)
    }

    @Test
    internal fun `test that onDismissRequest function is invoked`() = runTest {
        underTest.onDismissRequest()
        underTest.uiState.test {
            val actual = awaitItem().subtitleDisplayState
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
    }

    @Test
    internal fun `test that the errorState is updated correctly when emit BlockedMegaException`() =
        runTest {
            mockBlockedMegaException()
            initViewModel()
            underTest.errorState.test {
                assertThat(awaitItem()).isInstanceOf(BlockedMegaException::class.java)
            }
        }

    private fun mockBlockedMegaException() {
        val expectedTransfer = mock<Transfer> {
            on { isForeignOverQuota }.thenReturn(true)
            on { nodeHandle }.thenReturn(MegaApiJava.INVALID_HANDLE)
        }
        val event = mock<TransferEvent.TransferTemporaryErrorEvent> {
            on { transfer }.thenReturn(expectedTransfer)
            on { error }.thenReturn(mock<BlockedMegaException>())
        }
        monitorTransferEventsUseCase.stub {
            on { invoke() }.thenReturn(flowOf(event))
        }
    }

    @Test
    internal fun `test that the errorState is updated correctly when emit QuotaExceededMegaException`() =
        runTest {
            mockQuotaExceededMegaException()
            initViewModel()
            underTest.errorState.test {
                assertThat(awaitItem()).isInstanceOf(QuotaExceededMegaException::class.java)
            }
        }

    private fun mockQuotaExceededMegaException() {
        val expectedTransfer = mock<Transfer> {
            on { isForeignOverQuota }.thenReturn(false)
            on { nodeHandle }.thenReturn(MegaApiJava.INVALID_HANDLE)
        }
        val expectedError = mock<QuotaExceededMegaException> {
            on { value }.thenReturn(1)
        }
        val event = mock<TransferEvent.TransferTemporaryErrorEvent> {
            on { transfer }.thenReturn(expectedTransfer)
            on { error }.thenReturn(expectedError)
        }
        monitorTransferEventsUseCase.stub {
            on { invoke() }.thenReturn(flowOf(event))
        }
    }
}