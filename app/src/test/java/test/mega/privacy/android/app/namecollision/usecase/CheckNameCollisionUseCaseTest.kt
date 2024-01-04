package test.mega.privacy.android.app.namecollision.usecase

import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckNameCollisionUseCaseTest {
    private lateinit var underTest: CheckNameCollisionUseCase

    @BeforeAll
    internal fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()

    private val getNodeUseCase = mock<GetNodeUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = CheckNameCollisionUseCase(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            getNodeUseCase = getNodeUseCase,
            getChatMessageUseCase = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
        RxAndroidPlugins.reset()
    }

    @Nested
    @DisplayName("Inner most check call")
    inner class InnerCheck {
        @Test
        internal fun `test that check with a null parent throws ParentDoesNotExistException`() =
            runTest {
                assertThrows<MegaNodeException.ParentDoesNotExistException>() {
                    underTest.checkAsync(
                        "name",
                        null
                    )
                }
            }

        @Test
        internal fun `test that check with no child throws ChildDoesNotExistsException`() =
            runTest {
                megaApiGateway.stub {
                    onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(null)
                }
                assertThrows<MegaNodeException.ChildDoesNotExistsException>() {
                    underTest.checkAsync(
                        "name",
                        mock()
                    )
                }
            }

        @Test
        internal fun `test that check returns child handle when found`() = runTest {
            val expected = 1234L
            val child = mock<MegaNode> {
                on { handle }.thenReturn(expected)
            }
            megaApiGateway.stub {
                onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(child)
            }
            assertThat(underTest.checkAsync("name", mock()))
                .isEqualTo(expected)
        }
    }


    @Test
    internal fun `test that check with collision type copy returns the correct result`() = runTest {
        val expected = 1234L
        val child = mock<MegaNode> {
            on { handle }.thenReturn(expected)
        }
        val folders = 4
        val files = 20
        megaApiGateway.stub {
            onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(child)
            onBlocking { getNumChildFolders(any()) }.thenReturn(folders)
            onBlocking { getNumChildFiles(any()) }.thenReturn(files)
        }
        underTest.check(
            node = mock { on { name }.thenReturn("name") },
            parentNode = mock(),
            type = NameCollisionType.COPY,
        ).test()
            .assertValue {
                it is NameCollision.Copy
                        && it.childFolderCount == folders
                        && it.childFileCount == files
            }
    }

    @Test
    internal fun `test that check with collision type move returns the correct result`() = runTest {
        val expected = 1234L
        val child = mock<MegaNode> {
            on { handle }.thenReturn(expected)
        }
        val folders = 4
        val files = 20
        megaApiGateway.stub {
            onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(child)
            onBlocking { getNumChildFolders(any()) }.thenReturn(folders)
            onBlocking { getNumChildFiles(any()) }.thenReturn(files)
        }
        underTest.check(
            node = mock { on { name }.thenReturn("name") },
            parentNode = mock(),
            type = NameCollisionType.MOVE,
        ).test()
            .assertValue {
                it is NameCollision.Movement
                        && it.childFolderCount == folders
                        && it.childFileCount == files
            }
    }

    @Test
    internal fun `test that check with collision type upload throws an illegal state exception`() =
        runTest {
            val expected = 1234L
            val child = mock<MegaNode> {
                on { handle }.thenReturn(expected)
            }
            megaApiGateway.stub {
                onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(child)
            }
            underTest.check(
                node = mock { on { name }.thenReturn("name") },
                parentNode = mock(),
                type = NameCollisionType.UPLOAD,
            ).test()
                .assertError(IllegalStateException::class.java)
        }

    @Test
    internal fun `test that checkRestorations returns the correct file and folder count`() =
        runTest {
            val deletedHandle = 1234L
            val deletedNode = mock<MegaNode> {
                on { restoreHandle }.thenReturn(deletedHandle)
                on { name }.thenReturn("name")
            }
            val folders = 4
            val files = 20

            val childHandle = 1234L
            val child = mock<MegaNode> {
                on { handle }.thenReturn(childHandle)
            }
            val parent = mock<MegaNode>()
            megaApiGateway.stub {
                onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(child)
                onBlocking { isInRubbish(any()) }.thenReturn(false)
                onBlocking { getNumChildFolders(any()) }.thenReturn(folders)
                onBlocking { getNumChildFiles(any()) }.thenReturn(files)
                onBlocking { getMegaNodeByHandle(deletedHandle) }.thenReturn(parent)
            }

            underTest.checkRestorations(
                nodes = listOf(deletedNode),
            ).test()
                .assertValue {
                    val actual: NameCollision = it.first.first()
                    actual.childFolderCount == folders && actual.childFileCount == files
                }
        }

    @Test
    internal fun `test that check with node, parentHandle and collision type copy returns the correct result`() =
        runTest {
            val expected = 1234L
            val parentNodeHandle = 12345L
            val parentNode = mock<MegaNode>()
            val child = mock<MegaNode> {
                on { handle }.thenReturn(expected)
            }
            val folders = 4
            val files = 20
            megaApiGateway.stub {
                onBlocking { getChildNode(anyOrNull(), anyOrNull()) }.thenReturn(child)
                onBlocking { getNumChildFolders(any()) }.thenReturn(folders)
                onBlocking { getNumChildFiles(any()) }.thenReturn(files)
                onBlocking { getMegaNodeByHandle(parentNodeHandle) }.thenReturn(parentNode)
            }
            underTest.check(
                node = mock { on { name }.thenReturn("name") },
                parentNode = parentNode,
                type = NameCollisionType.COPY,
            ).test()
                .assertValue {
                    it is NameCollision.Copy
                            && it.childFolderCount == folders
                            && it.childFileCount == files
                }
        }

}