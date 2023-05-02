package test.mega.privacy.android.app.presentation.mediaplayer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_MATCHED_ITEM
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_OFF
import mega.privacy.android.app.mediaplayer.model.SubtitleDisplayState
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
internal class MediaPlayerViewModelTest {
    private lateinit var underTest: MediaPlayerViewModel
    private val scheduler = TestCoroutineScheduler()

    private val savedStateHandle = SavedStateHandle(mapOf())

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val expectedId = 123456L
    private val expectedName = "testName"
    private val expectedUrl = "test url"

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = MediaPlayerViewModel(
            checkNameCollisionUseCase = mock(),
            copyNodeUseCase = mock(),
            moveNodeUseCase = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
            sendStatisticsMediaPlayerUseCase = mock(),
            savedStateHandle = savedStateHandle
        )
        savedStateHandle[underTest.subtitleDialogShowKey] = false
        savedStateHandle[underTest.subtitleShowKey] = false
        savedStateHandle[underTest.videoPlayerPausedForPlaylistKey] = false
        savedStateHandle[underTest.currentSubtitleFileInfoKey] = null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test default state`() = runTest {
        val expectedState = SubtitleDisplayState()
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test showAddSubtitleDialog function is invoked`() = runTest {
        underTest.showAddSubtitleDialog()
        underTest.state.test {
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
    fun `test onAddedSubtitleOptionClicked function is invoked`() = runTest {
        underTest.onAddedSubtitleOptionClicked()
        underTest.state.test {
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
    fun `test onAddSubtitleFile function is invoked and info is not null`() = runTest {
        underTest.onAddSubtitleFile(
            SubtitleFileInfo(
                id = expectedId,
                name = expectedName,
                url = expectedUrl,
                parentName = null
            )
        )
        underTest.state.test {
            awaitItem()
            val actual = awaitItem()
            assertThat(actual.isSubtitleShown).isTrue()
            assertThat(actual.isAddSubtitle).isTrue()
            assertThat(actual.subtitleFileInfo?.id).isEqualTo(expectedId)
            assertThat(actual.subtitleFileInfo?.name).isEqualTo(expectedName)
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
        assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM)
    }

    @Test
    fun `test onAddSubtitleFile function is invoked and info is null`() = runTest {
        underTest.onAddSubtitleFile(null)
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
        assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_OFF)
    }

    @Test
    fun `test onAutoMatchItemClicked function is invoked`() = runTest {
        underTest.onAutoMatchItemClicked(
            SubtitleFileInfo(
                id = expectedId,
                name = expectedName,
                url = expectedUrl,
                parentName = null
            )
        )
        underTest.state.test {
            awaitItem()
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
    fun `test onOffItemClicked function is invoked`() = runTest {
        underTest.onOffItemClicked()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
        assertThat(underTest.selectOptionState).isEqualTo(SUBTITLE_SELECTED_STATE_OFF)
    }

    @Test
    fun `test onDismissRequest function is invoked`() = runTest {
        underTest.onDismissRequest()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
    }
}