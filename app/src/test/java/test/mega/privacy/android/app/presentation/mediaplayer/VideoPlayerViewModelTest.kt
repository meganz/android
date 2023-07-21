package test.mega.privacy.android.app.presentation.mediaplayer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_MATCHED_ITEM
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_OFF
import mega.privacy.android.app.mediaplayer.model.SubtitleDisplayState
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import nz.mega.sdk.MegaTransfer
import org.junit.Ignore
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VideoPlayerViewModelTest {
    private lateinit var underTest: VideoPlayerViewModel
    private val getGlobalTransferUseCase = mock<GetGlobalTransferUseCase>()
    private val savedStateHandle = SavedStateHandle(mapOf())

    private val expectedId = 123456L
    private val expectedName = "testName"
    private val expectedUrl = "test url"

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @BeforeEach
    fun setUp() {
        whenever(getGlobalTransferUseCase.get()).thenAnswer {
            Flowable.empty<GetGlobalTransferUseCase.Result>()
        }
        underTest = VideoPlayerViewModel(
            context = mock(),
            mediaPlayerGateway = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
            sendStatisticsMediaPlayerUseCase = mock(),
            offlineThumbnailFileWrapper = mock(),
            getGlobalTransferUseCase = getGlobalTransferUseCase,
            playlistItemMapper = mock(),
            trackPlaybackPositionUseCase = mock(),
            monitorPlaybackTimesUseCase = mock(),
            savePlaybackTimesUseCase = mock(),
            deletePlaybackInformationUseCase = mock(),
            megaApiFolderHttpServerSetMaxBufferSizeUseCase = mock(),
            megaApiFolderHttpServerIsRunningUseCase = mock(),
            megaApiFolderHttpServerStartUseCase = mock(),
            megaApiFolderHttpServerStopUseCase = mock(),
            megaApiHttpServerSetMaxBufferSizeUseCase = mock(),
            megaApiHttpServerIsRunningUseCase = mock(),
            megaApiHttpServerStartUseCase = mock(),
            megaApiHttpServerStop = mock(),
            areCredentialsNullUseCase = mock(),
            getLocalFilePathUseCase = mock(),
            getLocalFolderLinkFromMegaApiFolderUseCase = mock(),
            getLocalFolderLinkFromMegaApiUseCase = mock(),
            getLocalLinkFromMegaApiUseCase = mock(),
            getInboxNodeUseCase = mock(),
            getParentNodeFromMegaApiFolderUseCase = mock(),
            getRootNodeUseCase = mock(),
            getRootNodeFromMegaApiFolderUseCase = mock(),
            getRubbishNodeUseCase = mock(),
            getNodeByHandleUseCase = mock(),
            getVideoNodesFromPublicLinksUseCase = mock(),
            getVideoNodesFromInSharesUseCase = mock(),
            getVideoNodesFromOutSharesUseCase = mock(),
            getVideoNodesUseCase = mock(),
            getVideoNodesByEmailUseCase = mock(),
            getUserNameByEmailUseCase = mock(),
            getVideosByParentHandleFromMegaApiFolderUseCase = mock(),
            getVideoNodesByParentHandleUseCase = mock(),
            getNodesByHandlesUseCase = mock(),
            getFingerprintUseCase = mock(),
            fileDurationMapper = mock(),
            getSRTSubtitleFileListUseCase = mock(),
            setVideoRepeatModeUseCase = mock(),
            savedStateHandle = savedStateHandle,
            monitorVideoRepeatModeUseCase = mock()
        )
        savedStateHandle[underTest.subtitleDialogShowKey] = false
        savedStateHandle[underTest.subtitleShowKey] = false
        savedStateHandle[underTest.videoPlayerPausedForPlaylistKey] = false
        savedStateHandle[underTest.currentSubtitleFileInfoKey] = null
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
        RxAndroidPlugins.reset()
    }

    @Test
    internal fun `test that the initial state is returned`() = runTest {
        val expectedState = SubtitleDisplayState()
        underTest.subtitleDisplayState.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    internal fun `test that showAddSubtitleDialog function is invoked`() = runTest {
        underTest.showAddSubtitleDialog()
        underTest.subtitleDisplayState.test {
            val initial = awaitItem()
            assertThat(initial.isSubtitleShown).isFalse()
            assertThat(initial.isAddSubtitle).isFalse()
            assertThat(initial.isSubtitleDialogShown).isFalse()
            val actual = awaitItem()
            assertThat(actual.isSubtitleShown).isTrue()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isTrue()
        }
    }

    @Test
    internal fun `test that onAddedSubtitleOptionClicked function is invoked`() = runTest {
        underTest.onAddedSubtitleOptionClicked()
        underTest.subtitleDisplayState.test {
            val initial = awaitItem()
            assertThat(initial.isSubtitleShown).isFalse()
            assertThat(initial.isSubtitleDialogShown).isFalse()
            val actual = awaitItem()
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
                    parentName = null
                )
            )
            advanceUntilIdle()
            underTest.subtitleDisplayState.test {
                val actual = awaitItem()
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
        underTest.subtitleDisplayState.test {
            val actual = awaitItem()
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
                parentName = null
            )
        )
        advanceUntilIdle()
        underTest.subtitleDisplayState.test {
            val actual = awaitItem()
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
        underTest.subtitleDisplayState.test {
            val actual = awaitItem()
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
        assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_OFF)
    }

    @Test
    internal fun `test that onDismissRequest function is invoked`() = runTest {
        underTest.onDismissRequest()
        underTest.subtitleDisplayState.test {
            val actual = awaitItem()
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
    }
}