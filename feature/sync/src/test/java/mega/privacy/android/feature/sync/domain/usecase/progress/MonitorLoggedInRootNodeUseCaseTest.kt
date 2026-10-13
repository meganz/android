package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorLoggedInRootNodeUseCaseTest {

    private lateinit var underTest: MonitorLoggedInRootNodeUseCase

    private val monitorFetchNodesFinishUseCase = mock<MonitorFetchNodesFinishUseCase>()
    private val monitorLogoutUseCase = mock<MonitorLogoutUseCase>()
    private val monitorUserCredentialsUseCase = mock<MonitorUserCredentialsUseCase>()
    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()

    private val rootA = NodeId(1L)
    private val rootB = NodeId(2L)
    private val credentialsWithSession = UserCredentials(
        email = "a@b.c",
        session = "s".repeat(64),
        firstName = null,
        lastName = null,
        myHandle = null,
    )

    private lateinit var fetchNodesFinish: MutableSharedFlow<Boolean>
    private lateinit var logout: MutableSharedFlow<Boolean>
    private lateinit var credentials: MutableStateFlow<UserCredentials?>

    @BeforeAll
    fun setUp() {
        underTest = MonitorLoggedInRootNodeUseCase(
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
            monitorLogoutUseCase = monitorLogoutUseCase,
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase,
            getRootNodeIdUseCase = getRootNodeIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorFetchNodesFinishUseCase,
            monitorLogoutUseCase,
            monitorUserCredentialsUseCase,
            getRootNodeIdUseCase,
        )
        fetchNodesFinish = MutableSharedFlow()
        logout = MutableSharedFlow()
        credentials = MutableStateFlow(credentialsWithSession)
        monitorFetchNodesFinishUseCase.stub { on { invoke() }.thenReturn(fetchNodesFinish) }
        monitorLogoutUseCase.stub { on { invoke() }.thenReturn(logout) }
        monitorUserCredentialsUseCase.stub { on { invoke() }.thenReturn(credentials) }
    }

    @Test
    fun `test that invoke emits the root node when the nodes are already loaded`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(rootA)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(rootA)
        }
    }

    @Test
    fun `test that invoke emits null when the nodes are not loaded`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(null)

        underTest().test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that invoke emits the root node when fetch nodes finishes`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(null, rootA)

        underTest().test {
            assertThat(awaitItem()).isNull()
            fetchNodesFinish.emit(true)
            assertThat(awaitItem()).isEqualTo(rootA)
        }
    }

    @Test
    fun `test that invoke subscribes to fetch nodes finish and logout before reading the root node`() =
        runTest {
            var fetchNodesFinishSubscribers = -1
            var logoutSubscribers = -1
            whenever(getRootNodeIdUseCase()).thenAnswer {
                fetchNodesFinishSubscribers = fetchNodesFinish.subscriptionCount.value
                logoutSubscribers = logout.subscriptionCount.value
                rootA
            }
            val dispatcher = LastInFirstOutDispatcher()
            val items = mutableListOf<NodeId?>()

            val collection = launch(dispatcher) { underTest().collect { items += it } }
            dispatcher.runAll()
            collection.cancel()
            dispatcher.runAll()

            assertThat(items).containsExactly(rootA)
            assertThat(fetchNodesFinishSubscribers).isEqualTo(1)
            assertThat(logoutSubscribers).isEqualTo(1)
        }

    @Test
    fun `test that invoke emits null when a logout is broadcast`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(rootA)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(rootA)
            logout.emit(true)
            assertThat(awaitItem()).isNull()
        }

        verify(getRootNodeIdUseCase, times(1)).invoke()
    }

    @Test
    fun `test that invoke ignores a false logout value`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(rootA)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(rootA)
            logout.emit(false)
            expectNoEvents()
        }
    }

    @Test
    fun `test that invoke emits null when the saved session is cleared`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(rootA)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(rootA)
            credentials.value = null
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that invoke emits null when there is no saved session`() = runTest {
        credentials.value = null

        underTest().test {
            assertThat(awaitItem()).isNull()
        }

        verifyNoInteractions(getRootNodeIdUseCase)
    }

    @Test
    fun `test that invoke stays null when fetch nodes finishes without a root node`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(null)

        underTest().test {
            assertThat(awaitItem()).isNull()
            fetchNodesFinish.emit(true)
            expectNoEvents()
        }

        verify(getRootNodeIdUseCase, times(2)).invoke()
    }

    @Test
    fun `test that invoke does not emit again when fetch nodes finishes for the same account`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(rootA)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(rootA)
                fetchNodesFinish.emit(true)
                fetchNodesFinish.emit(true)
                expectNoEvents()
            }
        }

    @Test
    fun `test that invoke emits the new root node when another account fetches nodes`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(rootA, rootB)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(rootA)
            fetchNodesFinish.emit(true)
            assertThat(awaitItem()).isEqualTo(rootB)
        }
    }

    @Test
    fun `test that invoke does not suspend the logout broadcast while the root node is being read`() =
        runTest {
            val deferred = CompletableDeferred<NodeId?>()
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doSuspendableAnswer { deferred.await() }
            }

            underTest().test {
                val emitJob = launch { logout.emit(true) }
                runCurrent()

                assertThat(emitJob.isCompleted).isTrue()
                assertThat(deferred.isCompleted).isFalse()

                deferred.complete(rootA)
                assertThat(awaitItem()).isEqualTo(rootA)
                assertThat(awaitItem()).isNull()
            }
        }

    @Test
    fun `test that invoke does not suspend fetch nodes finish while the root node is being read`() =
        runTest {
            val deferred = CompletableDeferred<NodeId?>()
            var reads = 0
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doSuspendableAnswer {
                    reads++
                    deferred.await()
                }
            }

            underTest().test {
                val emitJob = launch {
                    fetchNodesFinish.emit(true)
                    fetchNodesFinish.emit(true)
                }
                runCurrent()

                assertThat(emitJob.isCompleted).isTrue()
                assertThat(reads).isEqualTo(1)

                deferred.complete(rootA)
                assertThat(awaitItem()).isEqualTo(rootA)
                expectNoEvents()
            }

            assertThat(reads).isEqualTo(2)
        }

    @Test
    fun `test that invoke fails when reading the root node fails`() = runTest {
        val error = RuntimeException("root read failed")
        whenever(getRootNodeIdUseCase()).thenThrow(error)

        underTest().test {
            val actual = awaitError()
            assertThat(actual).isInstanceOf(RuntimeException::class.java)
            assertThat(actual).hasMessageThat().isEqualTo(error.message)
        }
    }

    @Test
    fun `test that invoke does not subscribe to fetch nodes finish or logout while there is no saved session`() =
        runTest {
            credentials.value = null

            underTest().test {
                assertThat(awaitItem()).isNull()
                assertThat(fetchNodesFinish.subscriptionCount.value).isEqualTo(0)
                assertThat(logout.subscriptionCount.value).isEqualTo(0)
            }
        }

    @Test
    fun `test that invoke reads the root node again when a saved session appears`() = runTest {
        credentials.value = null
        whenever(getRootNodeIdUseCase()).thenReturn(rootA)

        underTest().test {
            assertThat(awaitItem()).isNull()
            credentials.value = credentialsWithSession
            assertThat(awaitItem()).isEqualTo(rootA)
        }

        verify(getRootNodeIdUseCase, times(1)).invoke()
    }

    @Test
    fun `test that invoke does not emit the previous root node when the next account saves its session before its nodes are loaded`() =
        runTest {
            var root: NodeId? = rootA
            whenever(getRootNodeIdUseCase()).thenAnswer { root }

            underTest().test {
                assertThat(awaitItem()).isEqualTo(rootA)
                credentials.value = null
                assertThat(awaitItem()).isNull()

                root = null
                credentials.value = credentialsWithSession.copy(email = "b@b.c")
                expectNoEvents()

                root = rootB
                fetchNodesFinish.emit(true)
                assertThat(awaitItem()).isEqualTo(rootB)
                expectNoEvents()
            }
        }

    @Test
    fun `test that invoke does not read the root node again when the credentials change but keep a session`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(rootA)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(rootA)
                credentials.value = credentialsWithSession.copy(firstName = "x")
                expectNoEvents()
            }

            verify(getRootNodeIdUseCase, times(1)).invoke()
        }

    // Runs the newest dispatched task first. A FIFO dispatcher runs the two subscription launches
    // before the resumed root read, so it cannot show subscriptions that start after the read.
    private class LastInFirstOutDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            tasks.addLast(block)
        }

        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeLast().run()
        }
    }
}
