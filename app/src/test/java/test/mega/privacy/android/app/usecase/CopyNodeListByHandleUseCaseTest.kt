package test.mega.privacy.android.app.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.usecase.CopyNodeListByHandleUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class CopyNodeListByHandleUseCaseTest {
    private lateinit var underTest: CopyNodeListByHandleUseCase

    private val megaApi = mock<MegaApiGateway>()
    private val megaApiFolder = mock<MegaApiFolderGateway>()

    @BeforeEach
    internal fun setUp() {
        underTest = CopyNodeListByHandleUseCase(
            megaApi = megaApi,
            megaApiFolder = megaApiFolder,
            ioDispatcher = UnconfinedTestDispatcher(),
            accountRepository = mock(),
        )
    }

    @Test
    internal fun `test that exception is thrown when no parent node is found`() = runTest {
        megaApi.stub { onBlocking { getMegaNodeByHandle(any()) }.thenReturn(null) }
        megaApiFolder.stub { onBlocking { getMegaNodeByHandle(any()) }.thenReturn(null) }

        assertThrows<MegaNodeException.ParentDoesNotExistException> { underTest(emptyList(), 1L) }
    }

    @Test
    internal fun `test that node list size is returned`() = runTest {
        megaApi.stub {
            onBlocking { getMegaNodeByHandle(any()) }.thenReturn(mock())
        }
        stubCopyMethod(result = MegaError.API_OK)

        val nodes = List(5) { it.toLong() }
        assertThat(underTest(nodes, 1L).count).isEqualTo(nodes.size)
    }

    @Test
    internal fun `test that if no errors found error count is 0`() = runTest {
        megaApi.stub {
            onBlocking { getMegaNodeByHandle(any()) }.thenReturn(mock())
            onBlocking { isForeignNode(any()) }.thenReturn(false)
        }
        stubCopyMethod(result = MegaError.API_OK)

        val nodes = List(5) { it.toLong() }
        assertThat(underTest(nodes, 1L).errorCount).isEqualTo(0)
    }

    @Test
    internal fun `test that non thrown errors add to the count of errors`() = runTest {
        megaApi.stub {
            onBlocking { getMegaNodeByHandle(any()) }.thenReturn(mock())
        }
        stubCopyMethod(result = MegaError.API_EWRITE)

        val nodes = List(5) { it.toLong() }
        assertThat(underTest(nodes, 1L).errorCount).isEqualTo(nodes.size)
    }

    @Test
    internal fun `test that QuotaExceededMegaException errors are thrown`() = runTest {
        val parentId = 12345L
        val parent = mock<MegaNode> { on { handle }.thenReturn(parentId) }
        megaApi.stub {
            onBlocking { getMegaNodeByHandle(any()) }.thenReturn(mock())
            onBlocking { getMegaNodeByHandle(parentId) }.thenReturn(parent)
            onBlocking { isForeignNode(any()) }.thenReturn(false)
        }
        stubCopyMethod(result = MegaError.API_EOVERQUOTA)

        val nodes = List(5) { it.toLong() }
        assertThrows<QuotaExceededMegaException> { underTest(nodes, parentId) }

    }

    @Test
    internal fun `test that copy is called`() = runTest {
        val parentId = 12345L
        val parent = mock<MegaNode> { on { handle }.thenReturn(parentId) }
        megaApi.stub {
            onBlocking { getMegaNodeByHandle(any()) }.thenReturn(mock())
            onBlocking { getMegaNodeByHandle(parentId) }.thenReturn(parent)
            onBlocking { isForeignNode(any()) }.thenReturn(false)
        }
        stubCopyMethod(result = MegaError.API_OK)

        val nodes = List(5) { it.toLong() }
        underTest(nodes, parentId)


        verify(megaApi, times(nodes.size)).copyNode(
            any(),
            any(),
            anyOrNull(),
            any(),
        )
    }

    @Test
    internal fun `test that cancellation stops execution`() = runTest {
        val parentId = 12345L
        val parent = mock<MegaNode> { on { handle }.thenReturn(parentId) }
        megaApi.stub {
            onBlocking { getMegaNodeByHandle(any()) }.thenReturn(mock())
            onBlocking { getMegaNodeByHandle(parentId) }.thenReturn(parent)
            onBlocking { isForeignNode(any()) }.thenReturn(false)
        }
        stubCopyMethod(result = MegaError.API_OK)

        val nodes = List(5) { it.toLong() }
        launch { underTest(nodes, parentId) }.cancelAndJoin()


        verify(megaApi, never()).copyNode(
            any(),
            any(),
            anyOrNull(),
            any(),
        )
    }


    private fun stubCopyMethod(
        result: Int,
        megaNode: MegaNode = any(),
    ) {
        megaApi.stub {
            on {
                copyNode(
                    nodeToCopy = megaNode,
                    newNodeParent = any(),
                    newNodeName = anyOrNull(),
                    listener = any()
                )
            }.thenAnswer { invocation ->
                (invocation.arguments[3] as? MegaRequestListenerInterface)?.onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(result)
                        on { errorString }.thenReturn("Error string")
                    }
                )
            }
        }
    }


}