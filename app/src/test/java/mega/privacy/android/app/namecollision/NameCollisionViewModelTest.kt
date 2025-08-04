package mega.privacy.android.app.namecollision

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.InstantExecutorExtension
import mega.privacy.android.app.extensions.withCoroutineExceptions
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.node.CopyCollidedNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyCollidedNodesUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionResultUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionsResultUseCase
import mega.privacy.android.domain.usecase.node.namecollision.NodeCollisionsWithSize
import mega.privacy.android.domain.usecase.node.namecollision.ReorderNodeNameCollisionsUseCase
import mega.privacy.android.domain.usecase.node.namecollision.UpdateNodeNameCollisionsResultUseCase
import mega.privacy.android.domain.usecase.transfers.DeleteCacheFilesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [InstantExecutorExtension::class])
internal class NameCollisionViewModelTest {
    private lateinit var underTest: NameCollisionViewModel

    private val getFileVersionsOption = mock<GetFileVersionsOption>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()
    private val copyCollidedNodesUseCase = mock<CopyCollidedNodesUseCase>()
    private val copyCollidedNodeUseCase = mock<CopyCollidedNodeUseCase>()
    private val copyRequestMessageMapper = mock<CopyRequestMessageMapper>()
    private val getNodeNameCollisionResultUseCase: GetNodeNameCollisionResultUseCase = mock()
    private val getNodeNameCollisionsResultUseCase: GetNodeNameCollisionsResultUseCase = mock()
    private val reorderNodeNameCollisionsUseCase: ReorderNodeNameCollisionsUseCase = mock()
    private val updateNodeNameCollisionsResultUseCase: UpdateNodeNameCollisionsResultUseCase =
        mock()
    private val deleteCacheFilesUseCase = mock<DeleteCacheFilesUseCase>()

    private fun initUnderTest() {
        underTest = NameCollisionViewModel(
            getFileVersionsOption = getFileVersionsOption,
            getNodeNameCollisionResultUseCase = getNodeNameCollisionResultUseCase,
            getNodeNameCollisionsResultUseCase = getNodeNameCollisionsResultUseCase,
            reorderNodeNameCollisionsUseCase = reorderNodeNameCollisionsUseCase,
            updateNodeNameCollisionsResultUseCase = updateNodeNameCollisionsResultUseCase,
            copyCollidedNodesUseCase = copyCollidedNodesUseCase,
            copyCollidedNodeUseCase = copyCollidedNodeUseCase,
            monitorUserUpdates = monitorUserUpdates,
            getNodeByHandleUseCase = mock(),
            setCopyLatestTargetPathUseCase = mock(),
            setMoveLatestTargetPathUseCase = mock(),
            copyRequestMessageMapper = copyRequestMessageMapper,
            nodeMoveRequestMessageMapper = mock(),
            getNodeByFingerprintAndParentNodeUseCase = mock(),
            moveCollidedNodeUseCase = mock(),
            moveCollidedNodesUseCase = mock(),
            deleteCacheFilesUseCase = deleteCacheFilesUseCase,
        )
    }


    @BeforeEach
    fun resetMocks() {
        reset(
            copyCollidedNodeUseCase,
            copyCollidedNodesUseCase,
            getFileVersionsOption,
            monitorUserUpdates,
            deleteCacheFilesUseCase,
        )
    }

    @Test
    internal fun `test that an exception in getFileVersionsOption is caught`() =
        withCoroutineExceptions {
            runTest {
                monitorUserUpdates.stub {
                    on { invoke() }.thenReturn(flowOf(UserChanges.DisableVersions))
                }

                getFileVersionsOption.stub {
                    onBlocking { invoke(true) }.thenAnswer { throw Exception("This test failed") }
                }

                initUnderTest()
                underTest.getFileVersioningInfo().test().assertNoValue()
            }
        }

    @Test
    internal fun `test that action result is set when copying node is successful`() = runTest {
        val nameCollision = mock<NodeNameCollision.Default> {
            on { parentHandle }.thenReturn(2L)
        }
        val message = "1 file copied"
        whenever(copyCollidedNodeUseCase(nameCollision, true)).thenReturn(
            MoveRequestResult.Copy(
                1,
                0
            )
        )
        whenever(copyRequestMessageMapper(any())).thenReturn(message)

        initUnderTest()
        underTest.singleCopy(nameCollision, true)
        advanceUntilIdle()

        assertThat(underTest.onActionResult().value?.message).isEqualTo(message)
    }

    @Test
    internal fun `test that throwable is set when exception is thrown while copying single node`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default>()
            whenever(
                copyCollidedNodeUseCase(
                    nameCollision,
                    true
                )
            ).thenThrow(RuntimeException::class.java)

            initUnderTest()
            underTest.singleCopy(nameCollision, true)
            advanceUntilIdle()

            assertThat(underTest.onExceptionThrown().value).isInstanceOf(RuntimeException::class.java)
        }


    @Test
    internal fun `test that action result is set when collided nodes are copied successful`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default> {
                on { parentHandle }.thenReturn(2L)
            }
            val nameCollision2 = mock<NodeNameCollision.Default>()
            val collisions = listOf(nameCollision, nameCollision2)
            val message = "2 file copied"
            whenever(copyCollidedNodesUseCase(collisions, true)).thenReturn(
                MoveRequestResult.Copy(
                    2,
                    0
                )
            )
            whenever(copyRequestMessageMapper(any())).thenReturn(message)

            initUnderTest()
            underTest.copy(collisions, true)
            advanceUntilIdle()

            assertThat(underTest.onActionResult().value?.message).isEqualTo(message)
        }

    @Test
    internal fun `test that throwable is set when exception is thrown while copying list of collided nodes`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default> {
                on { parentHandle }.thenReturn(2L)
            }
            val nameCollision2 = mock<NodeNameCollision.Default>()
            val collisions = listOf(nameCollision, nameCollision2)
            whenever(copyCollidedNodesUseCase(collisions, true))
                .thenThrow(RuntimeException::class.java)

            initUnderTest()
            underTest.copy(collisions, true)
            advanceUntilIdle()

            assertThat(underTest.onExceptionThrown().value).isInstanceOf(RuntimeException::class.java)
        }

    @ParameterizedTest(name = " and call state changes as joined with {0}")
    @EnumSource(
        value = NameCollisionChoice::class,
        names = ["RENAME", "REPLACE_UPDATE_MERGE"]
    )
    @NullSource
    fun `test that state is updated correctly if upload`(
        choice: NameCollisionChoice?,
    ) = runTest {
        val pathsAndNames = mapOf("path" to "name")
        val destinationId = NodeId(123L)
        val expected = triggered(
            TransferTriggerEvent.StartUpload.CollidedFiles(
                pathsAndNames = pathsAndNames,
                destinationId = destinationId,
                collisionChoice = choice
            )
        )

        with(underTest) {
            uploadFiles(pathsAndNames, destinationId, choice)
            uiState.map { it.uploadEvent }.test {
                assertThat(awaitItem()).isEqualTo(expected)
                consumeUploadEvent()
                assertThat(awaitItem()).isEqualTo(consumed())
            }
        }
    }

    @Test
    fun `test that cancel invokes deleteCacheFilesUseCase for single current collision path when cancel is invoked`() =
        runTest {
            val path = UriPath("/cacheFolder/Mega/file.txt")
            val nameCollision = mock<FileNameCollision> {
                on { this.path } doReturn path
            }
            val collisionResult = mock<NodeNameCollisionResult> {
                on { this.nameCollision } doReturn nameCollision
            }
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn collisionResult
            initUnderTest()
            underTest.setSingleData(nameCollision)
            advanceUntilIdle()

            underTest.cancel(false)
            advanceUntilIdle()

            verify(deleteCacheFilesUseCase).invoke(listOf(path))
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that cancel invokes deleteCacheFilesUseCase for corresponding collision paths when cancel is invoked`(
        applyNextFlag: Boolean,
    ) = runTest {
        val paths = (0..10).map { UriPath("/cacheFolder/Mega/file$it.txt") }
        val expected = if (applyNextFlag) paths else listOf(paths.first())
        val nameCollisions = paths.map { path ->
            mock<FileNameCollision> {
                on { this.path } doReturn path
            }
        }
        val collisionResults = nameCollisions.map { nameCollision ->
            mock<NodeNameCollisionResult> {
                on { this.nameCollision } doReturn nameCollision
            }
        }
        nameCollisions.forEachIndexed { index, nameCollision ->
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn collisionResults[index]
            whenever(getNodeNameCollisionsResultUseCase(nameCollisions.drop(index))) doReturn
                    collisionResults.drop(index)
        }

        whenever(reorderNodeNameCollisionsUseCase(nameCollisions)) doReturn NodeCollisionsWithSize(
            nameCollisions,
            nameCollisions.size - 1,
            0,
        )
        initUnderTest()
        underTest.setData(nameCollisions)
        advanceUntilIdle()

        underTest.cancel(applyNextFlag)
        advanceUntilIdle()

        verify(deleteCacheFilesUseCase).invoke(expected)
    }


    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}