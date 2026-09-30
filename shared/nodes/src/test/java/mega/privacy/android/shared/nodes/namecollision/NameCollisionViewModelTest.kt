package mega.privacy.android.shared.nodes.namecollision

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.node.CopyCollidedNodesUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionResultUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionsResultUseCase
import mega.privacy.android.domain.usecase.node.namecollision.IsCopyToOriginUseCase
import mega.privacy.android.domain.usecase.node.namecollision.ReorderNodeNameCollisionsUseCase
import mega.privacy.android.domain.usecase.node.namecollision.UpdateNodeNameCollisionsResultUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.DeleteCacheFilesUseCase
import mega.privacy.android.shared.nodes.mapper.message.CopyRequestMessageMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NameCollisionViewModelTest {
    private lateinit var underTest: NameCollisionViewModel

    private val getFileVersionsOption = mock<GetFileVersionsOption>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()
    private val copyCollidedNodesUseCase = mock<CopyCollidedNodesUseCase>()
    private val copyRequestMessageMapper = mock<CopyRequestMessageMapper>()
    private val getNodeNameCollisionResultUseCase: GetNodeNameCollisionResultUseCase = mock()
    private val getNodeNameCollisionsResultUseCase: GetNodeNameCollisionsResultUseCase = mock()
    private val reorderNodeNameCollisionsUseCase = ReorderNodeNameCollisionsUseCase()
    private val updateNodeNameCollisionsResultUseCase: UpdateNodeNameCollisionsResultUseCase =
        mock()
    private val deleteCacheFilesUseCase = mock<DeleteCacheFilesUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isCopyToOriginUseCase = mock<IsCopyToOriginUseCase>()
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

    private fun initUnderTest(args: NameCollisionArgs) {
        underTest = NameCollisionViewModel(
            getFileVersionsOption = getFileVersionsOption,
            getNodeNameCollisionResultUseCase = getNodeNameCollisionResultUseCase,
            getNodeNameCollisionsResultUseCase = getNodeNameCollisionsResultUseCase,
            reorderNodeNameCollisionsUseCase = reorderNodeNameCollisionsUseCase,
            updateNodeNameCollisionsResultUseCase = updateNodeNameCollisionsResultUseCase,
            copyCollidedNodesUseCase = copyCollidedNodesUseCase,
            monitorUserUpdates = monitorUserUpdates,
            setCopyLatestTargetPathUseCase = mock(),
            setMoveLatestTargetPathUseCase = mock(),
            copyRequestMessageMapper = copyRequestMessageMapper,
            nodeMoveRequestMessageMapper = mock(),
            isCopyToOriginUseCase = isCopyToOriginUseCase,
            moveCollidedNodesUseCase = mock(),
            deleteCacheFilesUseCase = deleteCacheFilesUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            applicationScope = applicationScope,
            args = args,
        )
        underTest.startLoading()
    }

    /**
     * [NameCollisionViewModel.uiState] is lazy: the collisions only load once it is first read, as
     * happens when the screen starts collecting it.
     */
    private fun NameCollisionViewModel.startLoading() = uiState.value

    private fun singleArgs(collision: mega.privacy.android.domain.entity.node.NameCollision) =
        NameCollisionArgs.Single(collision = collision, isFolderUploadContext = false)

    private fun multipleArgs(
        collisions: List<mega.privacy.android.domain.entity.node.NameCollision>,
        isFolderUploadContext: Boolean = false,
    ) = NameCollisionArgs.Multiple(
        collisions = collisions,
        isFolderUploadContext = isFolderUploadContext,
    )

    @BeforeEach
    fun resetMocks() {
        reset(
            copyCollidedNodesUseCase,
            getFileVersionsOption,
            monitorUserUpdates,
            deleteCacheFilesUseCase,
            getNodeAccessPermission,
            getNodeNameCollisionResultUseCase,
            getNodeNameCollisionsResultUseCase,
            updateNodeNameCollisionsResultUseCase,
            copyRequestMessageMapper,
            isCopyToOriginUseCase,
        )
        whenever(monitorUserUpdates()).thenReturn(emptyFlow())
    }

    @Test
    internal fun `test that fileVersioningInfo is not set when getFileVersionsOption throws`() =
        runTest {
            whenever(monitorUserUpdates()).thenReturn(flowOf(UserChanges.DisableVersions))
            whenever(getFileVersionsOption(true)).thenAnswer { throw Exception("This test failed") }

            initUnderTest(multipleArgs(emptyList()))
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().fileVersioningInfo).isNull()
            }
        }

    private fun copyCollision(parentHandle: Long = 2L, isFile: Boolean = false) =
        mock<NodeNameCollision.Default> {
            on { this.parentHandle } doReturn parentHandle
            on { type } doReturn NodeNameCollisionType.COPY
            on { this.isFile } doReturn isFile
        }

    private fun uploadCollision(
        path: String,
        name: String,
        renameName: String? = null,
        isFile: Boolean = true,
    ) = mock<FileNameCollision> {
        on { this.path } doReturn UriPath(path)
        on { this.name } doReturn name
        on { this.renameName } doReturn renameName
        on { this.parentHandle } doReturn PARENT_HANDLE
        on { this.isFile } doReturn isFile
    }

    /**
     * Stubs the collision results for a queue that loads as [orderedCollisions]: the first one is the
     * current collision and the rest are the pending ones.
     */
    private suspend fun stubQueue(orderedCollisions: List<NameCollision>) {
        whenever(getNodeNameCollisionResultUseCase(orderedCollisions.first())) doReturn
                NodeNameCollisionResult(nameCollision = orderedCollisions.first())
        whenever(getNodeNameCollisionsResultUseCase(orderedCollisions.drop(1))) doReturn
                orderedCollisions.drop(1).map { NodeNameCollisionResult(nameCollision = it) }
    }

    private fun expectedUpload(
        pathsAndNames: Map<String, String?>,
        choice: NameCollisionChoice?,
    ) = triggered(
        TransferTriggerEvent.StartUpload.CollidedFiles(
            pathsAndNames = pathsAndNames,
            destinationId = NodeId(PARENT_HANDLE),
            collisionChoice = choice,
            pitagTrigger = PitagTrigger.NotApplicable,
        )
    )

    @Test
    internal fun `test that the outcome is completed when the current node is copied`() = runTest {
        val nameCollision = copyCollision()
        val message = "1 file copied"
        whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                NodeNameCollisionResult(nameCollision = nameCollision)
        whenever(copyCollidedNodesUseCase(listOf(nameCollision), false)) doReturn MoveRequestResult.Copy(1, 0)
        whenever(copyRequestMessageMapper(any())) doReturn message

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()
        underTest.replaceUpdateOrMerge(applyOnNext = false)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = expectMostRecentItem()
            val event = state.outcome
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat(((event as StateEventWithContentTriggered).content as NameCollisionOutcome.ActionCompleted).result.message)
                .isEqualTo(message)
            assertThat(state.dismissEvent).isEqualTo(consumed)
        }
    }

    @Test
    internal fun `test that the outcome is failed when copying the current node throws`() = runTest {
        val nameCollision = copyCollision()
        whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                NodeNameCollisionResult(nameCollision = nameCollision)
        whenever(copyCollidedNodesUseCase(listOf(nameCollision), false)) doThrow RuntimeException()

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()
        underTest.replaceUpdateOrMerge(applyOnNext = false)
        advanceUntilIdle()

        underTest.uiState.test {
            val event = expectMostRecentItem().outcome
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat(((event as StateEventWithContentTriggered).content as NameCollisionOutcome.Failed).throwable)
                .isInstanceOf(RuntimeException::class.java)
        }
    }

    @Test
    internal fun `test that the outcome is completed when the pending nodes are copied too`() =
        runTest {
            val nameCollision = copyCollision()
            val nameCollision2 = copyCollision()
            val collisions = listOf(nameCollision, nameCollision2)
            val message = "2 file copied"
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                    NodeNameCollisionResult(nameCollision = nameCollision)
            whenever(getNodeNameCollisionsResultUseCase(listOf(nameCollision2))) doReturn
                    listOf(NodeNameCollisionResult(nameCollision = nameCollision2))
            whenever(copyCollidedNodesUseCase(collisions, false)) doReturn MoveRequestResult.Copy(2, 0)
            whenever(copyRequestMessageMapper(any())) doReturn message

            initUnderTest(multipleArgs(collisions))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                val event = expectMostRecentItem().outcome
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat(((event as StateEventWithContentTriggered).content as NameCollisionOutcome.ActionCompleted).result.message)
                    .isEqualTo(message)
            }
        }

    @Test
    internal fun `test that the outcome is failed when copying the pending nodes throws`() =
        runTest {
            val nameCollision = copyCollision()
            val nameCollision2 = copyCollision()
            val collisions = listOf(nameCollision, nameCollision2)
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                    NodeNameCollisionResult(nameCollision = nameCollision)
            whenever(getNodeNameCollisionsResultUseCase(listOf(nameCollision2))) doReturn
                    listOf(NodeNameCollisionResult(nameCollision = nameCollision2))
            whenever(copyCollidedNodesUseCase(collisions, false)) doThrow RuntimeException()

            initUnderTest(multipleArgs(collisions))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                val event = expectMostRecentItem().outcome
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat(((event as StateEventWithContentTriggered).content as NameCollisionOutcome.Failed).throwable)
                    .isInstanceOf(RuntimeException::class.java)
            }
        }

    @Test
    fun `test that the upload event carries the replace choice when the current file is uploaded`() =
        runTest {
            val nameCollision = uploadCollision(path = "path", name = "name")
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                    NodeNameCollisionResult(nameCollision = nameCollision)

            initUnderTest(singleArgs(nameCollision))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = false)
            advanceUntilIdle()

            underTest.uiState.map { it.uploadEvent }.test {
                assertThat(expectMostRecentItem()).isEqualTo(
                    expectedUpload(
                        pathsAndNames = mapOf("path" to "name"),
                        choice = NameCollisionChoice.REPLACE_UPDATE_MERGE,
                    )
                )
            }

            underTest.consumeUploadEvent()
            advanceUntilIdle()
            underTest.uiState.map { it.uploadEvent }.test {
                assertThat(expectMostRecentItem()).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that the upload event carries the rename choice when the current file is renamed`() =
        runTest {
            val nameCollision = uploadCollision(path = "path", name = "name", renameName = "name (1)")
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                    NodeNameCollisionResult(nameCollision = nameCollision)
            whenever(updateNodeNameCollisionsResultUseCase(emptyList(), listOf("name (1)"), false))
                .doReturn(emptyList<NodeNameCollisionResult>() to listOf("name (1)"))

            initUnderTest(singleArgs(nameCollision))
            advanceUntilIdle()
            underTest.rename(applyOnNext = false)
            advanceUntilIdle()

            underTest.uiState.map { it.uploadEvent }.test {
                assertThat(expectMostRecentItem()).isEqualTo(
                    expectedUpload(
                        pathsAndNames = mapOf("path" to "name (1)"),
                        choice = NameCollisionChoice.RENAME,
                    )
                )
            }
        }

    @Test
    fun `test that the upload event carries no choice when the pending files are uploaded too`() =
        runTest {
            val nameCollision = uploadCollision(path = "path1", name = "name1")
            val nameCollision2 = uploadCollision(path = "path2", name = "name2")
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                    NodeNameCollisionResult(nameCollision = nameCollision)
            whenever(getNodeNameCollisionsResultUseCase(listOf(nameCollision2))) doReturn
                    listOf(NodeNameCollisionResult(nameCollision = nameCollision2))

            initUnderTest(multipleArgs(listOf(nameCollision, nameCollision2)))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.map { it.uploadEvent }.test {
                assertThat(expectMostRecentItem()).isEqualTo(
                    expectedUpload(
                        pathsAndNames = mapOf("path1" to "name1", "path2" to "name2"),
                        choice = null,
                    )
                )
            }

            underTest.consumeUploadEvent()
            advanceUntilIdle()
            underTest.uiState.map { it.uploadEvent }.test {
                assertThat(expectMostRecentItem()).isEqualTo(consumed())
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
            initUnderTest(singleArgs(nameCollision))
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

        initUnderTest(multipleArgs(nameCollisions))
        advanceUntilIdle()

        underTest.cancel(applyNextFlag)
        advanceUntilIdle()

        verify(deleteCacheFilesUseCase).invoke(expected)
    }

    @Test
    fun `test that the pending collision counts are set in the state when the collisions are loaded`() =
        runTest {
            val files = (0..1).map {
                mock<FileNameCollision> { on { this.isFile } doReturn true }
            }
            val folders = (0..2).map {
                mock<FileNameCollision> { on { this.isFile } doReturn false }
            }
            whenever(getNodeNameCollisionsResultUseCase(any())) doReturn emptyList()

            initUnderTest(multipleArgs(files + folders))
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.pendingFileCollisions).isEqualTo(1)
                assertThat(state.pendingFolderCollisions).isEqualTo(2)
            }
        }

    @Test
    fun `test that hasPendingCollisions is cleared when the user cancels the remaining collisions`() =
        runTest {
            val nameCollisions = (0..1).map {
                mock<FileNameCollision> { on { this.isFile } doReturn true }
            }
            val collisionResults = nameCollisions.map { nameCollision ->
                mock<NodeNameCollisionResult> { on { this.nameCollision } doReturn nameCollision }
            }
            whenever(getNodeNameCollisionResultUseCase(nameCollisions.first())) doReturn
                    collisionResults.first()
            whenever(getNodeNameCollisionsResultUseCase(any())) doReturn collisionResults.drop(1)

            initUnderTest(multipleArgs(nameCollisions))
            advanceUntilIdle()
            underTest.uiState.test {
                assertThat(expectMostRecentItem().hasPendingCollisions).isTrue()
            }

            underTest.cancel(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().hasPendingCollisions).isFalse()
            }
        }

    @Test
    fun `test that uploadedFilesCount is 0 initially`() = runTest {
        initUnderTest(multipleArgs(emptyList()))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().uploadedFilesCount).isEqualTo(0)
        }
    }

    @Test
    fun `test that uploadedFilesCount increments when single file is uploaded`() = runTest {
        val path = UriPath("/cacheFolder/Mega/file.txt")
        val nameCollision = mock<FileNameCollision> {
            on { this.path } doReturn path
            on { this.isFile } doReturn true
            on { this.parentHandle } doReturn 123L
            on { this.name } doReturn "file.txt"
            on { this.renameName } doReturn null
            on { this.pitagTrigger } doReturn PitagTrigger.NotApplicable
        }
        val collisionResult = mock<NodeNameCollisionResult> {
            on { this.nameCollision } doReturn nameCollision
        }
        whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn collisionResult
        whenever(getFileVersionsOption(any())).thenReturn(false)

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().uploadedFilesCount).isEqualTo(0)
        }

        underTest.replaceUpdateOrMerge(false)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().uploadedFilesCount).isEqualTo(1)
        }
    }

    @Test
    fun `test that uploadedFilesCount increments correctly when multiple files are uploaded`() =
        runTest {
            val paths = listOf(
                UriPath("/cacheFolder/Mega/file1.txt"),
                UriPath("/cacheFolder/Mega/file2.txt"),
                UriPath("/cacheFolder/Mega/file3.txt")
            )
            val nameCollisions = paths.mapIndexed { index, path ->
                mock<FileNameCollision> {
                    on { this.path } doReturn path
                    on { this.isFile } doReturn true
                    on { this.parentHandle } doReturn 123L
                    on { this.name } doReturn "file${index + 1}.txt"
                    on { this.renameName } doReturn null
                    on { this.pitagTrigger } doReturn PitagTrigger.NotApplicable
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

            whenever(getFileVersionsOption(any())).thenReturn(false)

            initUnderTest(multipleArgs(nameCollisions))
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().uploadedFilesCount).isEqualTo(0)
            }

            underTest.replaceUpdateOrMerge(true)
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().uploadedFilesCount).isEqualTo(3)
            }
        }

    @Test
    fun `test that uploadedFilesCount does not increment when folder is uploaded`() = runTest {
        val path = UriPath("/cacheFolder/Mega/folder")
        val nameCollision = mock<FileNameCollision> {
            on { this.path } doReturn path
            on { this.isFile } doReturn false
            on { this.parentHandle } doReturn 123L
            on { this.name } doReturn "folder"
            on { this.renameName } doReturn null
            on { this.pitagTrigger } doReturn PitagTrigger.NotApplicable
        }
        val collisionResult = mock<NodeNameCollisionResult> {
            on { this.nameCollision } doReturn nameCollision
        }
        whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn collisionResult
        whenever(getFileVersionsOption(any())).thenReturn(false)

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().uploadedFilesCount).isEqualTo(0)
        }

        underTest.replaceUpdateOrMerge(false)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().uploadedFilesCount).isEqualTo(0)
        }
    }

    @Test
    fun `test that cancelAll clears pendingCollisions for non-folder-upload context`() =
        runTest {
            val paths = listOf(
                UriPath("/cacheFolder/Mega/file1.txt"),
                UriPath("/cacheFolder/Mega/file2.txt"),
            )
            val nameCollisions = paths.mapIndexed { index, path ->
                mock<FileNameCollision> {
                    on { this.path } doReturn path
                    on { this.isFile } doReturn true
                    on { this.parentHandle } doReturn 123L
                    on { this.name } doReturn "file${index + 1}.txt"
                    on { this.renameName } doReturn null
                    on { this.pitagTrigger } doReturn PitagTrigger.NotApplicable
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

            whenever(getFileVersionsOption(any())).thenReturn(false)

            initUnderTest(multipleArgs(nameCollisions, isFolderUploadContext = false))
            advanceUntilIdle()

            underTest.cancel(true)
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().hasPendingCollisions).isFalse()
            }
        }

    private suspend fun stubUploadCollision(isFile: Boolean = true): FileNameCollision {
        val nameCollision = mock<FileNameCollision> {
            on { this.collisionHandle } doReturn COLLISION_HANDLE
            on { this.isFile } doReturn isFile
        }
        val collisionResult = mock<NodeNameCollisionResult> {
            on { this.nameCollision } doReturn nameCollision
        }
        whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn collisionResult
        whenever(getFileVersionsOption(any())) doReturn false
        return nameCollision
    }

    @ParameterizedTest
    @EnumSource(
        value = AccessPermission::class,
        names = ["FULL", "OWNER"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that canReplaceOrUpdate is false when the colliding file has no full access`(
        accessPermission: AccessPermission,
    ) = runTest {
        val nameCollision = stubUploadCollision()
        whenever(getNodeAccessPermission(NodeId(COLLISION_HANDLE))) doReturn accessPermission

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().fileVersioningInfo?.canReplaceOrUpdate).isFalse()
        }
    }

    @Test
    fun `test that canReplaceOrUpdate is false when getFileVersionsOption throws and the colliding file has no full access`() =
        runTest {
            val nameCollision = stubUploadCollision()
            whenever(getFileVersionsOption(any())) doThrow RuntimeException()
            whenever(getNodeAccessPermission(NodeId(COLLISION_HANDLE))) doReturn
                    AccessPermission.READWRITE

            initUnderTest(singleArgs(nameCollision))
            advanceUntilIdle()

            underTest.uiState.test {
                val info = expectMostRecentItem().fileVersioningInfo
                assertThat(info?.canReplaceOrUpdate).isFalse()
                assertThat(info?.isFileVersioningEnabled).isFalse()
            }
        }

    @ParameterizedTest
    @EnumSource(value = AccessPermission::class, names = ["FULL", "OWNER"])
    fun `test that canReplaceOrUpdate is true when the colliding file has full access`(
        accessPermission: AccessPermission,
    ) = runTest {
        val nameCollision = stubUploadCollision()
        whenever(getNodeAccessPermission(NodeId(COLLISION_HANDLE))) doReturn accessPermission

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().fileVersioningInfo?.canReplaceOrUpdate).isTrue()
        }
    }

    @Test
    fun `test that canReplaceOrUpdate is true when the access permission cannot be obtained`() =
        runTest {
            val nameCollision = stubUploadCollision()
            whenever(getNodeAccessPermission(NodeId(COLLISION_HANDLE))) doReturn null

            initUnderTest(singleArgs(nameCollision))
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().fileVersioningInfo?.canReplaceOrUpdate).isTrue()
            }
        }

    @Test
    fun `test that canReplaceOrUpdate is true when getting the access permission throws`() =
        runTest {
            val nameCollision = stubUploadCollision()
            whenever(getNodeAccessPermission(NodeId(COLLISION_HANDLE))) doThrow RuntimeException()

            initUnderTest(singleArgs(nameCollision))
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().fileVersioningInfo?.canReplaceOrUpdate).isTrue()
            }
        }

    @Test
    fun `test that the access permission is not checked when the collision is a folder`() =
        runTest {
            val nameCollision = stubUploadCollision(isFile = false)

            initUnderTest(singleArgs(nameCollision))
            advanceUntilIdle()

            verifyNoInteractions(getNodeAccessPermission)
            underTest.uiState.test {
                assertThat(expectMostRecentItem().fileVersioningInfo?.canReplaceOrUpdate).isTrue()
            }
        }

    @Test
    fun `test that the access permission is not checked when the collision is not an upload`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Chat> {
                on { this.collisionHandle } doReturn COLLISION_HANDLE
                on { this.isFile } doReturn true
            }
            val collisionResult = mock<NodeNameCollisionResult> {
                on { this.nameCollision } doReturn nameCollision
            }
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn collisionResult
            whenever(getFileVersionsOption(any())) doReturn false

            initUnderTest(singleArgs(nameCollision))
            advanceUntilIdle()

            verifyNoInteractions(getNodeAccessPermission)
            underTest.uiState.test {
                assertThat(expectMostRecentItem().fileVersioningInfo?.canReplaceOrUpdate).isTrue()
            }
        }

    @ParameterizedTest(name = "isCopyToOriginUseCase returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isCopyToOrigin is set from isCopyToOriginUseCase when a file link collision is loaded`(
        expected: Boolean,
    ) = runTest {
        val collision = mock<NodeNameCollision.FileLink>()
        whenever(isCopyToOriginUseCase(collision)) doReturn expected
        whenever(getNodeNameCollisionResultUseCase(collision)) doReturn
                NodeNameCollisionResult(nameCollision = collision)

        initUnderTest(singleArgs(collision))
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isCopyToOrigin).isEqualTo(expected)
        }
    }

    @Test
    fun `test that isCopyToOrigin is false when isCopyToOriginUseCase throws`() = runTest {
        val collision = mock<NodeNameCollision.Default> {
            on { type } doReturn NodeNameCollisionType.COPY
        }
        whenever(isCopyToOriginUseCase(collision)) doThrow RuntimeException()
        whenever(getNodeNameCollisionResultUseCase(collision)) doReturn
                NodeNameCollisionResult(nameCollision = collision)

        initUnderTest(singleArgs(collision))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isCopyToOrigin).isFalse()
            assertThat(state.currentCollision?.nameCollision).isEqualTo(collision)
        }
    }

    @Test
    fun `test that the resolved outcome carries the choice when the last folder-upload collision is resolved`() =
        runTest {
            val nameCollision = mock<FileNameCollision> {
                on { this.isFile } doReturn true
            }
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                    NodeNameCollisionResult(nameCollision = nameCollision)
            whenever(getFileVersionsOption(any())) doReturn false

            initUnderTest(multipleArgs(listOf(nameCollision), isFolderUploadContext = true))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = false)
            advanceUntilIdle()

            underTest.uiState.test {
                val event = expectMostRecentItem().outcome
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                val resolved = (event as StateEventWithContentTriggered).content
                assertThat(resolved).isInstanceOf(NameCollisionOutcome.Resolved::class.java)
                assertThat((resolved as NameCollisionOutcome.Resolved).collisions.map { it.choice })
                    .containsExactly(NameCollisionChoice.REPLACE_UPDATE_MERGE)
            }

            underTest.consumeOutcome()
            underTest.uiState.test {
                assertThat(expectMostRecentItem().outcome).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that the dismiss event is triggered when the launch source is missing`() = runTest {
        initUnderTest(NameCollisionArgs.MissingLaunch)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().dismissEvent).isEqualTo(triggered)
        }

        underTest.consumeDismissEvent()
        underTest.uiState.test {
            assertThat(expectMostRecentItem().dismissEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that the dismiss event is triggered when the last collision is skipped`() = runTest {
        val nameCollision = mock<FileNameCollision> {
            on { this.path } doReturn UriPath("/cacheFolder/Mega/file.txt")
        }
        val collisionResult = mock<NodeNameCollisionResult> {
            on { this.nameCollision } doReturn nameCollision
        }
        whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn collisionResult

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()
        underTest.cancel(applyOnNext = false)
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().dismissEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that the dismiss event is triggered when the last file upload is consumed`() = runTest {
        val nameCollision = uploadCollision(path = "path", name = "name")
        whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                NodeNameCollisionResult(nameCollision = nameCollision)

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()
        underTest.replaceUpdateOrMerge(applyOnNext = false)
        advanceUntilIdle()
        underTest.consumeUploadEvent()
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().dismissEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that the dismiss event is triggered when the batch upload of the pending files is consumed`() =
        runTest {
            val nameCollision = uploadCollision(path = "path1", name = "name1")
            val nameCollision2 = uploadCollision(path = "path2", name = "name2")
            whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                    NodeNameCollisionResult(nameCollision = nameCollision)
            whenever(getNodeNameCollisionsResultUseCase(listOf(nameCollision2))) doReturn
                    listOf(NodeNameCollisionResult(nameCollision = nameCollision2))

            initUnderTest(multipleArgs(listOf(nameCollision, nameCollision2)))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()
            underTest.consumeUploadEvent()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().dismissEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that replaceUpdateOrMerge presents the first folder when the batched files leave folders pending`() =
        runTest {
            val (file1, file2) = List(2) { copyCollision(isFile = true) }
            val (folder1, folder2) = List(2) { copyCollision(isFile = false) }
            stubQueue(listOf(file1, file2, folder1, folder2))
            whenever(copyCollidedNodesUseCase(listOf(file1, file2), false)) doReturn
                    MoveRequestResult.Copy(2, 0)

            initUnderTest(multipleArgs(listOf(file1, folder1, file2, folder2)))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()

            verify(copyCollidedNodesUseCase).invoke(listOf(file1, file2), false)
            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.currentCollision?.nameCollision).isEqualTo(folder1)
                assertThat(state.pendingFileCollisions).isEqualTo(0)
                assertThat(state.pendingFolderCollisions).isEqualTo(1)
                assertThat(state.hasPendingCollisions).isTrue()
                assertThat(state.outcome).isEqualTo(consumed())
                assertThat(state.dismissEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that the outcome is completed when the folders left after the batched files are resolved one by one`() =
        runTest {
            val (file1, file2) = List(2) { copyCollision(isFile = true) }
            val (folder1, folder2) = List(2) { copyCollision(isFile = false) }
            val message = "2 folders merged"
            stubQueue(listOf(file1, file2, folder1, folder2))
            whenever(copyCollidedNodesUseCase(listOf(file1, file2), false)) doReturn
                    MoveRequestResult.Copy(2, 0)
            whenever(copyCollidedNodesUseCase(listOf(folder1), false)) doReturn
                    MoveRequestResult.Copy(1, 0)
            whenever(copyCollidedNodesUseCase(listOf(folder2), false)) doReturn
                    MoveRequestResult.Copy(1, 0)
            whenever(copyRequestMessageMapper(any())) doReturn message

            initUnderTest(multipleArgs(listOf(file1, folder1, file2, folder2)))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = false)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.currentCollision?.nameCollision).isEqualTo(folder2)
                assertThat(state.pendingFolderCollisions).isEqualTo(0)
                assertThat(state.hasPendingCollisions).isFalse()
                assertThat(state.outcome).isEqualTo(consumed())
            }

            underTest.replaceUpdateOrMerge(applyOnNext = false)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                val event = state.outcome
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat(((event as StateEventWithContentTriggered).content as NameCollisionOutcome.ActionCompleted).result.message)
                    .isEqualTo(message)
                assertThat(state.dismissEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that cancel presents the first folder when the skipped files leave folders pending`() =
        runTest {
            val file1 = uploadCollision(path = "path1", name = "name1")
            val file2 = uploadCollision(path = "path2", name = "name2")
            val folder1 = uploadCollision(path = "folder1", name = "folder1", isFile = false)
            val folder2 = uploadCollision(path = "folder2", name = "folder2", isFile = false)
            stubQueue(listOf(file1, file2, folder1, folder2))

            initUnderTest(multipleArgs(listOf(file1, folder1, file2, folder2)))
            advanceUntilIdle()
            underTest.cancel(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.currentCollision?.nameCollision).isEqualTo(folder1)
                assertThat(state.pendingFileCollisions).isEqualTo(0)
                assertThat(state.pendingFolderCollisions).isEqualTo(1)
                assertThat(state.hasPendingCollisions).isTrue()
                assertThat(state.outcome).isEqualTo(consumed())
                assertThat(state.dismissEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that the dismiss event is triggered when the folders left after the skipped files are skipped too`() =
        runTest {
            val file1 = uploadCollision(path = "path1", name = "name1")
            val file2 = uploadCollision(path = "path2", name = "name2")
            val folder1 = uploadCollision(path = "folder1", name = "folder1", isFile = false)
            val folder2 = uploadCollision(path = "folder2", name = "folder2", isFile = false)
            stubQueue(listOf(file1, file2, folder1, folder2))

            initUnderTest(multipleArgs(listOf(file1, folder1, file2, folder2)))
            advanceUntilIdle()
            underTest.cancel(applyOnNext = true)
            advanceUntilIdle()
            underTest.cancel(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.hasPendingCollisions).isFalse()
                assertThat(state.outcome).isEqualTo(consumed())
                assertThat(state.dismissEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that the batch upload carries only the files when the folders behind them are left pending`() =
        runTest {
            val file1 = uploadCollision(path = "path1", name = "name1")
            val file2 = uploadCollision(path = "path2", name = "name2")
            val folder1 = uploadCollision(path = "folder1", name = "folder1", isFile = false)
            val folder2 = uploadCollision(path = "folder2", name = "folder2", isFile = false)
            stubQueue(listOf(file1, file2, folder1, folder2))

            initUnderTest(multipleArgs(listOf(file1, folder1, file2, folder2)))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().uploadEvent).isEqualTo(
                    expectedUpload(
                        pathsAndNames = mapOf("path1" to "name1", "path2" to "name2"),
                        choice = null,
                    )
                )
            }

            underTest.consumeUploadEvent()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.currentCollision?.nameCollision).isEqualTo(folder1)
                assertThat(state.pendingFolderCollisions).isEqualTo(1)
                assertThat(state.uploadedFilesCount).isEqualTo(2)
                assertThat(state.dismissEvent).isEqualTo(consumed)
            }

            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()
            underTest.consumeUploadEvent()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().dismissEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that the resolved outcome carries every choice when the skipped files leave folders to merge in the folder upload context`() =
        runTest {
            val file1 = uploadCollision(path = "path1", name = "name1")
            val file2 = uploadCollision(path = "path2", name = "name2")
            val folder1 = uploadCollision(path = "folder1", name = "folder1", isFile = false)
            val folder2 = uploadCollision(path = "folder2", name = "folder2", isFile = false)
            stubQueue(listOf(file1, file2, folder1, folder2))
            whenever(getFileVersionsOption(any())) doReturn false

            initUnderTest(
                multipleArgs(listOf(file1, folder1, file2, folder2), isFolderUploadContext = true)
            )
            advanceUntilIdle()
            underTest.cancel(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.currentCollision?.nameCollision).isEqualTo(folder1)
                assertThat(state.outcome).isEqualTo(consumed())
                assertThat(state.dismissEvent).isEqualTo(consumed)
            }

            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                val event = expectMostRecentItem().outcome
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                val resolved = (event as StateEventWithContentTriggered).content
                assertThat(resolved).isInstanceOf(NameCollisionOutcome.Resolved::class.java)
                assertThat((resolved as NameCollisionOutcome.Resolved).collisions.map { it.nameCollision to it.choice })
                    .containsExactly(
                        file1 to NameCollisionChoice.CANCEL,
                        file2 to NameCollisionChoice.CANCEL,
                        folder1 to NameCollisionChoice.REPLACE_UPDATE_MERGE,
                        folder2 to NameCollisionChoice.REPLACE_UPDATE_MERGE,
                    )
                    .inOrder()
            }
        }

    @Test
    fun `test that the counter is 1 of 2 when a batch of two files and two folders is loaded`() =
        runTest {
            val (file1, file2) = List(2) { copyCollision(isFile = true) }
            val (folder1, folder2) = List(2) { copyCollision(isFile = false) }
            stubQueue(listOf(file1, file2, folder1, folder2))

            initUnderTest(multipleArgs(listOf(file1, folder1, file2, folder2)))
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.currentCollision?.nameCollision).isEqualTo(file1)
                assertThat(state.currentCollisionIndex).isEqualTo(1)
                assertThat(state.currentTypeCollisionCount).isEqualTo(2)
            }
        }

    @Test
    fun `test that the counter advances when the current file is skipped`() = runTest {
        val file1 = uploadCollision(path = "path1", name = "name1")
        val file2 = uploadCollision(path = "path2", name = "name2")
        stubQueue(listOf(file1, file2))

        initUnderTest(multipleArgs(listOf(file1, file2)))
        advanceUntilIdle()
        underTest.cancel(applyOnNext = false)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.currentCollision?.nameCollision).isEqualTo(file2)
            assertThat(state.currentCollisionIndex).isEqualTo(2)
            assertThat(state.currentTypeCollisionCount).isEqualTo(2)
        }
    }

    @Test
    fun `test that the counter restarts at 1 of 2 when the batched files leave two folders pending`() =
        runTest {
            val (file1, file2) = List(2) { copyCollision(isFile = true) }
            val (folder1, folder2) = List(2) { copyCollision(isFile = false) }
            stubQueue(listOf(file1, file2, folder1, folder2))
            whenever(copyCollidedNodesUseCase(listOf(file1, file2), false)) doReturn
                    MoveRequestResult.Copy(2, 0)

            initUnderTest(multipleArgs(listOf(file1, folder1, file2, folder2)))
            advanceUntilIdle()
            underTest.replaceUpdateOrMerge(applyOnNext = true)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.currentCollision?.nameCollision).isEqualTo(folder1)
                assertThat(state.currentCollisionIndex).isEqualTo(1)
                assertThat(state.currentTypeCollisionCount).isEqualTo(2)
            }
        }

    @Test
    fun `test that the counter is 1 of 1 when a single collision is loaded`() = runTest {
        val nameCollision = copyCollision(isFile = true)
        whenever(getNodeNameCollisionResultUseCase(nameCollision)) doReturn
                NodeNameCollisionResult(nameCollision = nameCollision)

        initUnderTest(singleArgs(nameCollision))
        advanceUntilIdle()

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.currentCollisionIndex).isEqualTo(1)
            assertThat(state.currentTypeCollisionCount).isEqualTo(1)
        }
    }

    @Test
    fun `test that the counter is 0 of 0 when the launch source is missing`() = runTest {
        initUnderTest(NameCollisionArgs.MissingLaunch)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.currentCollision).isNull()
            assertThat(state.currentCollisionIndex).isEqualTo(0)
            assertThat(state.currentTypeCollisionCount).isEqualTo(0)
        }
    }

    @Test
    fun `test that the counter is unchanged when the pending collisions are renamed`() = runTest {
        val file1 = uploadCollision(path = "path1", name = "name1", renameName = "name1 (1)")
        val file2 = uploadCollision(path = "path2", name = "name2", renameName = "name2 (1)")
        stubQueue(listOf(file1, file2))
        whenever(updateNodeNameCollisionsResultUseCase(any(), any(), any())) doReturn
                (listOf(NodeNameCollisionResult(nameCollision = file2)) to listOf("name1 (1)"))

        initUnderTest(multipleArgs(listOf(file1, file2)))
        advanceUntilIdle()
        underTest.rename(applyOnNext = false)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.currentCollision?.nameCollision).isEqualTo(file1)
            assertThat(state.currentCollisionIndex).isEqualTo(1)
            assertThat(state.currentTypeCollisionCount).isEqualTo(2)
        }
    }

    companion object {
        private const val COLLISION_HANDLE = 123L
        private const val PARENT_HANDLE = 456L

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
