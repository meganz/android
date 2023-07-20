package test.mega.privacy.android.app.presentation.filelink

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.clouddrive.FileLinkViewModel
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class FileLinkViewModelTest {

    private lateinit var underTest: FileLinkViewModel
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val hasCredentials = mock<HasCredentials>()
    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val legacyCopyNodeUseCase = mock<LegacyCopyNodeUseCase>()
    private val checkNameCollisionUseCase = mock<CheckNameCollisionUseCase>()
    private val getNodeByHandle = mock<GetNodeByHandle>()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        underTest = FileLinkViewModel(
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            hasCredentials = hasCredentials,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            legacyCopyNodeUseCase = legacyCopyNodeUseCase,
            checkNameCollisionUseCase = checkNameCollisionUseCase,
            getNodeByHandle = getNodeByHandle
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.shouldLogin).isNull()
            assertThat(initial.url).isNull()
            assertThat(initial.collision).isNull()
            assertThat(initial.copyThrowable).isNull()
            assertThat(initial.copySuccess).isFalse()
            assertThat(initial.snackBarMessageId).isEqualTo(-1)
        }
    }

    @Test
    fun `test that login is not required when root node exists and db credentials are valid`() =
        runTest {
            whenever(hasCredentials()).thenReturn(true)
            whenever(rootNodeExistsUseCase()).thenReturn(true)

            underTest.state.test {
                underTest.checkLoginRequired()
                val newValue = expectMostRecentItem()
                assertThat(newValue.shouldLogin).isFalse()
            }
        }

    @Test
    fun `test that login is not required when db credentials are valid and rootnode does not exist`() =
        runTest {
            whenever(hasCredentials()).thenReturn(true)
            whenever(rootNodeExistsUseCase()).thenReturn(false)

            underTest.state.test {
                underTest.checkLoginRequired()
                val newValue = expectMostRecentItem()
                assertThat(newValue.shouldLogin).isTrue()
            }
        }

    @Test
    fun `test that on importing node with same name collision value is set correctly`() = runTest {
        val parentNodeHandle = 123L
        val nameCollision = mock<NameCollision.Copy>()
        val node = mock<MegaNode>()

        whenever(checkNameCollisionUseCase.check(node, parentNodeHandle, NameCollisionType.COPY))
            .thenReturn(nameCollision)

        underTest.state.test {
            underTest.handleImportNode(node, parentNodeHandle)
            val newValue = expectMostRecentItem()
            assertThat(newValue.collision).isNotNull()
        }
    }

    @Test
    fun `test that on importing node without same name collision value is null`() = runTest {
        val parentNodeHandle = 123L
        val node = mock<MegaNode>()

        whenever(checkNameCollisionUseCase.check(node, parentNodeHandle, NameCollisionType.COPY))
            .thenThrow(MegaNodeException.ChildDoesNotExistsException())

        underTest.state.test {
            underTest.handleImportNode(node, parentNodeHandle)
            val newValue = expectMostRecentItem()
            assertThat(newValue.collision).isNull()
        }
    }

    @Test
    fun `test that when checking name collision throws ParentDoesNotExistException snackbar message is set`() =
        runTest {
            val parentHandle = 123L
            val node = mock<MegaNode>()

            whenever(checkNameCollisionUseCase.check(node, parentHandle, NameCollisionType.COPY))
                .thenThrow(MegaNodeException.ParentDoesNotExistException())

            underTest.state.test {
                underTest.handleImportNode(node, parentHandle)
                val newValue = expectMostRecentItem()
                assertThat(newValue.collision).isNull()
                assertThat(newValue.snackBarMessageId).isNotEqualTo(-1)
            }
        }

    @Test
    fun `test that on importing node without same name then copy is successful`() = runTest {
        val parentNodeHandle = 123L
        val node = mock<MegaNode>()
        val parentNode = mock<MegaNode>()

        whenever(checkNameCollisionUseCase.check(node, parentNodeHandle, NameCollisionType.COPY))
            .thenThrow(MegaNodeException.ChildDoesNotExistsException())
        whenever(getNodeByHandle(parentNodeHandle)).thenReturn(parentNode)
        whenever(legacyCopyNodeUseCase.copyAsync(node, parentNode)).thenReturn(true)

        underTest.handleImportNode(node, parentNodeHandle)
        advanceUntilIdle()

        underTest.state.test {
            val newValue = awaitItem()
            assertThat(newValue.copySuccess).isTrue()
        }
    }
}