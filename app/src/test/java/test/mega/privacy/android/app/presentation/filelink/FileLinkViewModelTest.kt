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
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
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
    private val getPublicNodeUseCase = mock<GetPublicNodeUseCase>()

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
            getNodeByHandle = getNodeByHandle,
            getPublicNodeUseCase = getPublicNodeUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.shouldLogin).isNull()
            assertThat(initial.hasDbCredentials).isFalse()
            assertThat(initial.url).isEmpty()
            assertThat(initial.title).isEmpty()
            assertThat(initial.sizeInBytes).isEqualTo(0)
            assertThat(initial.previewPath).isNull()
            assertThat(initial.iconResource).isNull()
            assertThat(initial.askForDecryptionDialog).isFalse()
            assertThat(initial.collision).isNull()
            assertThat(initial.collisionCheckThrowable).isNull()
            assertThat(initial.copyThrowable).isNull()
            assertThat(initial.copySuccess).isFalse()
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
                assertThat(newValue.hasDbCredentials).isTrue()
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
                assertThat(newValue.hasDbCredentials).isTrue()
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
    fun `test that on calling resetCollision then collision value is reset`() = runTest {
        val parentNodeHandle = 123L
        val nameCollision = mock<NameCollision.Copy>()
        val node = mock<MegaNode>()

        whenever(checkNameCollisionUseCase.check(node, parentNodeHandle, NameCollisionType.COPY))
            .thenReturn(nameCollision)

        underTest.state.test {
            underTest.handleImportNode(node, parentNodeHandle)
            var newValue = expectMostRecentItem()
            assertThat(newValue.collision).isNotNull()
            underTest.resetCollision()
            newValue = expectMostRecentItem()
            assertThat(newValue.collision).isNull()
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
    fun `test that when checking name collision throws ParentDoesNotExistException collisionCheckThrowable is set`() =
        runTest {
            val parentHandle = 123L
            val node = mock<MegaNode>()

            whenever(checkNameCollisionUseCase.check(node, parentHandle, NameCollisionType.COPY))
                .thenThrow(MegaNodeException.ParentDoesNotExistException())

            underTest.state.test {
                underTest.handleImportNode(node, parentHandle)
                val newValue = expectMostRecentItem()
                assertThat(newValue.collision).isNull()
                assertThat(newValue.collisionCheckThrowable).isNotNull()
            }
        }

    @Test
    fun `test that on calling resetCollisionError then collisionCheckThrowable is reset`() =
        runTest {
            val parentHandle = 123L
            val node = mock<MegaNode>()

            whenever(checkNameCollisionUseCase.check(node, parentHandle, NameCollisionType.COPY))
                .thenThrow(MegaNodeException.ParentDoesNotExistException())

            underTest.state.test {
                underTest.handleImportNode(node, parentHandle)
                var newValue = expectMostRecentItem()
                assertThat(newValue.collisionCheckThrowable).isNotNull()
                underTest.resetCollisionError()
                newValue = expectMostRecentItem()
                assertThat(newValue.collisionCheckThrowable).isNull()
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

    @Test
    fun `test that on getting valid public node correct values are set`() = runTest {
        val url = "https://mega.co.nz/abc"
        val expectedPath = "data/cache/xyz.jpg"
        val title = "abc"
        val fileSize = 100000L
        val publicNode = mock<TypedFileNode> {
            on { previewPath }.thenReturn(expectedPath)
            on { name }.thenReturn(title)
            on { size }.thenReturn(fileSize)
        }

        whenever(getPublicNodeUseCase(any())).thenReturn(publicNode)
        underTest.state.test {
            underTest.getPublicNode(url)
            val result = expectMostRecentItem()
            assertThat(result.title).isEqualTo(title)
            assertThat(result.sizeInBytes).isEqualTo(fileSize)
            assertThat(result.previewPath).isEqualTo(expectedPath)
            assertThat(result.iconResource).isEqualTo(null)
        }
    }

    @Test
    fun `test that on getting DecryptionKeyRequired exception askForDecryptionDialog values is set`() =
        runTest {
            val url = "https://mega.co.nz/abc"

            whenever(getPublicNodeUseCase(any())).thenThrow(PublicNodeException.DecryptionKeyRequired())
            underTest.state.test {
                underTest.getPublicNode(url)
                val result = expectMostRecentItem()
                assertThat(result.askForDecryptionDialog).isEqualTo(true)
            }
        }

    @Test
    fun `test that on getting InvalidDecryptionKey exception when fetching from decrypt dialog askForDecryptionDialog value is set`() =
        runTest {
            val url = "https://mega.co.nz/abc"

            whenever(getPublicNodeUseCase(any())).thenThrow(PublicNodeException.InvalidDecryptionKey())
            underTest.state.test {
                underTest.getPublicNode(url, true)
                val result = expectMostRecentItem()
                assertThat(result.askForDecryptionDialog).isEqualTo(true)
            }
        }

    @Test
    fun `test that on getting InvalidDecryptionKey exception when not fetching from decrypt dialog then error dialog value is set`() =
        runTest {
            val url = "https://mega.co.nz/abc"

            whenever(getPublicNodeUseCase(any())).thenThrow(PublicNodeException.InvalidDecryptionKey())
            underTest.state.test {
                underTest.getPublicNode(url, false)
                val result = expectMostRecentItem()
                assertThat(result.askForDecryptionDialog).isEqualTo(false)
                assertThat(result.fetchPublicNodeError).isNotNull()
            }
        }

    @Test
    fun `test that on getting GenericError exception error dialog values are set`() = runTest {
        val url = "https://mega.co.nz/abc"

        whenever(getPublicNodeUseCase(any())).thenThrow(PublicNodeException.GenericError())
        underTest.state.test {
            underTest.getPublicNode(url)
            val result = expectMostRecentItem()
            assertThat(result.fetchPublicNodeError).isNotNull()
        }
    }

    @Test
    fun `test that askForDecryptionDialog should be reset to false when resetAskForDecryptionKeyDialog is invoked`() =
        runTest {
            underTest.state.test {
                underTest.resetAskForDecryptionKeyDialog()
                val newValue = expectMostRecentItem()
                assertThat(newValue.askForDecryptionDialog).isEqualTo(false)
            }
        }
}