package mega.privacy.android.app.getLink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_NONE_SENSITIVE
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_WARNING_TYPE_FOLDER
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_WARNING_TYPE_LINKS
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class GetLinkViewModelTest {
    private lateinit var underTest: GetLinkViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val hasSensitiveInheritedUseCase = mock<HasSensitiveInheritedUseCase>()
    private val hasSensitiveDescendantUseCase = mock<HasSensitiveDescendantUseCase>()

    @BeforeEach
    fun setUp() {
        whenever(monitorAccountDetailUseCase()).thenReturn(flow { mock() })
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = GetLinkViewModel(
            megaApi = mock(),
            dbH = mock(),
            encryptLinkWithPasswordUseCase = mock(),
            exportNodeUseCase = mock(),
            context = mock(),
            hasSensitiveDescendantUseCase = hasSensitiveDescendantUseCase,
            hasSensitiveInheritedUseCase = hasSensitiveInheritedUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = mock(),
            getNodeByIdUseCase = getNodeByIdUseCase,
            get1On1ChatIdUseCase = mock(),
            sendTextMessageUseCase = mock(),
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            monitorAccountDetailUseCase,
            hasSensitiveDescendantUseCase,
            hasSensitiveInheritedUseCase
        )
    }

    @Test
    fun `test that hasSensitiveItems updated the value is HIDDEN_NODE_NONE_SENSITIVE when exportedData is not null`() =
        runTest {
            val mockHandle = 12345L
            val nodeId = NodeId(mockHandle)
            val mockTypedNode = mock<TypedFileNode> {
                on { id }.thenReturn(nodeId)
                on { exportedData }.thenReturn(mock())
            }
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(mockTypedNode)
            initUnderTest()

            underTest.checkSensitiveItem(mockHandle)

            underTest.hasSensitiveItemsFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_NONE_SENSITIVE)
            }
        }

    @Test
    fun `test that hasSensitiveItems updated the value is HIDDEN_NODE_WARNING_TYPE_LINKS when isMarkedSensitive is true`() =
        runTest {
            val mockHandle = 12345L
            val nodeId = NodeId(mockHandle)
            val mockTypedNode = mock<TypedFileNode> {
                on { id }.thenReturn(nodeId)
                on { isMarkedSensitive }.thenReturn(true)
            }
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(mockTypedNode)
            initUnderTest()

            underTest.checkSensitiveItem(mockHandle)

            underTest.hasSensitiveItemsFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_WARNING_TYPE_LINKS)
            }
        }

    @Test
    fun `test that hasSensitiveItems updated the value is HIDDEN_NODE_WARNING_TYPE_LINKS when hasSensitiveInheritedUseCase returns true`() =
        runTest {
            val mockHandle = 12345L
            val nodeId = NodeId(mockHandle)
            val mockTypedNode = mock<TypedFileNode> {
                on { id }.thenReturn(nodeId)
            }
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(mockTypedNode)
            whenever(hasSensitiveInheritedUseCase(nodeId)).thenReturn(true)
            initUnderTest()

            underTest.checkSensitiveItem(mockHandle)

            underTest.hasSensitiveItemsFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_WARNING_TYPE_LINKS)
            }
        }

    @Test
    fun `test that hasSensitiveItems updated the value is HIDDEN_NODE_WARNING_TYPE_FOLDER`() =
        runTest {
            val mockHandle = 12345L
            val nodeId = NodeId(mockHandle)
            val mockTypedNode = mock<TypedFolderNode> {
                on { id }.thenReturn(nodeId)
            }
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(mockTypedNode)
            whenever(hasSensitiveInheritedUseCase(nodeId)).thenReturn(false)
            whenever(hasSensitiveDescendantUseCase(nodeId)).thenReturn(true)
            initUnderTest()

            underTest.checkSensitiveItem(mockHandle)

            underTest.hasSensitiveItemsFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_WARNING_TYPE_FOLDER)
            }
        }
}