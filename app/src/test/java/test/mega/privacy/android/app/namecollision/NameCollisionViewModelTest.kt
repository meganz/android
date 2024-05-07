package test.mega.privacy.android.app.namecollision

import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.namecollision.NameCollisionViewModel
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.node.CopyCollidedNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyCollidedNodesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.TestSchedulerExtension
import test.mega.privacy.android.app.extensions.withCoroutineExceptions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [InstantExecutorExtension::class, TestSchedulerExtension::class])
internal class NameCollisionViewModelTest {
    private lateinit var underTest: NameCollisionViewModel

    private val getFileVersionsOption = mock<GetFileVersionsOption>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()
    private val copyCollidedNodesUseCase = mock<CopyCollidedNodesUseCase>()
    private val copyCollidedNodeUseCase = mock<CopyCollidedNodeUseCase>()
    private val copyRequestMessageMapper = mock<CopyRequestMessageMapper>()

    private fun initUnderTest() {
        underTest = NameCollisionViewModel(
            getFileVersionsOption = getFileVersionsOption,
            getNameCollisionResultUseCase = mock(),
            uploadUseCase = mock(),
            copyCollidedNodesUseCase = copyCollidedNodesUseCase,
            copyCollidedNodeUseCase = copyCollidedNodeUseCase,
            monitorUserUpdates = monitorUserUpdates,
            getNodeUseCase = mock(),
            setCopyLatestTargetPathUseCase = mock(),
            setMoveLatestTargetPathUseCase = mock(),
            copyRequestMessageMapper = copyRequestMessageMapper,
            moveRequestMessageMapper = mock(),
            getNodeByFingerprintAndParentNodeUseCase = mock(),
            moveCollidedNodeUseCase = mock(),
            moveCollidedNodesUseCase = mock(),
        )
    }


    @BeforeEach
    fun resetMocks() {
        reset(
            copyCollidedNodeUseCase,
            copyCollidedNodesUseCase,
            getFileVersionsOption,
            monitorUserUpdates
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

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}