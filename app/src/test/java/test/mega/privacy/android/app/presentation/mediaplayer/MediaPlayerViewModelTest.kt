package test.mega.privacy.android.app.presentation.mediaplayer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_MATCHED_ITEM
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel.Companion.SUBTITLE_SELECTED_STATE_OFF
import mega.privacy.android.app.mediaplayer.model.SubtitleDisplayState
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import org.junit.jupiter.api.AfterAll
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
internal class MediaPlayerViewModelTest {
    private lateinit var underTest: MediaPlayerViewModel

    private val savedStateHandle = SavedStateHandle(mapOf())


    private val expectedId = 123456L
    private val expectedName = "testName"
    private val expectedUrl = "test url"
    private lateinit var checkNameCollision: CheckNameCollision
    private lateinit var copyNodeUseCase: CopyNodeUseCase
    private lateinit var moveNodeUseCase: MoveNodeUseCase

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        checkNameCollision = mock()
        copyNodeUseCase = mock()
        moveNodeUseCase = mock()
        underTest = MediaPlayerViewModel(
            checkNameCollision = checkNameCollision,
            copyNodeUseCase = copyNodeUseCase,
            moveNodeUseCase = moveNodeUseCase,
            ioDispatcher = UnconfinedTestDispatcher(),
            sendStatisticsMediaPlayerUseCase = mock(),
            savedStateHandle = savedStateHandle
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
    internal fun `test default state`() = runTest {
        val expectedState = SubtitleDisplayState()
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    internal fun `test showAddSubtitleDialog function is invoked`() = runTest {
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
    internal fun `test onAddedSubtitleOptionClicked function is invoked`() = runTest {
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
    internal fun `test onAddSubtitleFile function is invoked and info is not null`() = runTest {
        underTest.onAddSubtitleFile(
            SubtitleFileInfo(
                id = expectedId,
                name = expectedName,
                url = expectedUrl,
                parentName = null
            )
        )
        advanceUntilIdle()
        underTest.state.test {
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
    internal fun `test onAddSubtitleFile function is invoked and info is null`() = runTest {
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
    internal fun `test onAutoMatchItemClicked function is invoked`() = runTest {
        underTest.onAutoMatchItemClicked(
            SubtitleFileInfo(
                id = expectedId,
                name = expectedName,
                url = expectedUrl,
                parentName = null
            )
        )
        advanceUntilIdle()
        underTest.state.test {
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
    internal fun `test onOffItemClicked function is invoked`() = runTest {
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
    internal fun `test onDismissRequest function is invoked`() = runTest {
        underTest.onDismissRequest()
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.isSubtitleShown).isFalse()
            assertThat(actual.isAddSubtitle).isFalse()
            assertThat(actual.isSubtitleDialogShown).isFalse()
        }
    }

    @Test
    internal fun `test copy complete snack bar is shown when file is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenReturn(NodeId(selectedNode))
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()
            underTest.onSnackbarMessage().test().assertValue(R.string.context_correctly_copied)
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenAnswer { throw runtimeException }
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test move complete snack bar is shown when file is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenReturn(NodeId(selectedNode))
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().test().assertValue(R.string.context_correctly_moved)
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenThrow(runtimeException)
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }
}