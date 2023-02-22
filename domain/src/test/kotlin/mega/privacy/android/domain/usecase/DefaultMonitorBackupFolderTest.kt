package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorBackupFolderTest {
    private lateinit var underTest: DefaultMonitorBackupFolder

    private val nodeRepository = mock<NodeRepository>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        underTest = DefaultMonitorBackupFolder(
            nodeRepository = nodeRepository,
            monitorUserUpdates = monitorUserUpdates,
            dispatcher = testDispatcher
        )
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that current backup folder id is returned`() = runTest {
        val expected = Result.success(NodeId(1L))
        nodeRepository.stub {
            onBlocking { getBackupFolderId() }.thenReturn(expected.getOrThrow())
        }
        whenever(monitorUserUpdates()).thenReturn(emptyFlow())

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when user update of type backup folder is received, the new id is fetched`() =
        runTest {
            val updates = List(5) { UserChanges.MyBackupsFolder }
            val expected = Result.success(NodeId(1L))
            val expectedUpdates =
                List(updates.size) { index -> Result.success(NodeId(index.toLong())) }

            nodeRepository.stub {
                onBlocking { getBackupFolderId() }.thenReturn(
                    expected.getOrThrow(),
                    *expectedUpdates.map { it.getOrThrow() }.toTypedArray()
                )
            }
            whenever(monitorUserUpdates()).thenReturn(updates.asFlow())
            underTest().test {
                assertThat(awaitItem()).isEqualTo(expected)
                expectedUpdates.forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
                expectNoEvents()
                cancelAndIgnoreRemainingEvents() //state flow never completes
            }

        }

    @Test
    fun `test that if non backup folder user updates are emitted, no new id is fetched`() =
        runTest {
            val updates = UserChanges.values().filterNot { it == UserChanges.MyBackupsFolder }
            val expected = Result.success(NodeId(1L))
            val notExpected = NodeId(2L)
            nodeRepository.stub {
                onBlocking { getBackupFolderId() }.thenReturn(expected.getOrThrow(), notExpected)
            }
            whenever(monitorUserUpdates()).thenReturn(updates.asFlow())

            underTest().test {
                assertThat(awaitItem()).isEqualTo(expected)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents() //state flow never completes
            }
        }

    @Test
    fun `test that an error from the repository returns a failed result`() = runTest {

        nodeRepository.stub {
            onBlocking { getBackupFolderId() }.thenAnswer {
                throw Throwable()
            }
        }
        whenever(monitorUserUpdates()).thenReturn(emptyFlow())
        underTest().test {
            assertThat(awaitItem().isFailure).isTrue()
            cancelAndConsumeRemainingEvents()
        }

    }

    @Test
    fun `test that a new subscriber gets the latest value`() = runTest {
        val updates = List(5) { UserChanges.MyBackupsFolder }
        val expected = Result.success(NodeId(1L))
        val expectedUpdates =
            List(updates.size) { index -> Result.success(NodeId((index * 10).toLong())) }

        nodeRepository.stub {
            onBlocking { getBackupFolderId() }.thenReturn(
                expected.getOrThrow(),
                *expectedUpdates.map { it.getOrThrow() }.toTypedArray()
            )
        }
        whenever(monitorUserUpdates()).thenReturn(updates.asFlow())

        launch {
            underTest().take(expectedUpdates.size + 1).collect()
        }.invokeOnCompletion {
            launch {
                underTest().test {
                    assertThat(awaitItem()).isEqualTo(expectedUpdates.last())
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

}