package mega.privacy.android.app.main.legacycontact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_NONE_SENSITIVE
import mega.privacy.android.app.main.legacycontact.AddContactViewModel.Companion.HIDDEN_NODE_WARNING_TYPE_FOLDER
import mega.privacy.android.app.main.legacycontact.AddContactViewModel.Companion.HIDDEN_NODE_WARNING_TYPE_FOLDERS
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class AddContactViewModelTest {
    private lateinit var underTest: AddContactViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val hasSensitiveDescendantUseCase = mock<HasSensitiveDescendantUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>()

    @BeforeEach
    fun setUp() {
        whenever(monitorAccountDetailUseCase()).thenReturn(flow { mock() })
        runBlocking {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(getContactVerificationWarningUseCase()).thenReturn(false)
        }
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = AddContactViewModel(
            getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getChatCallUseCase = mock(),
            monitorChatCallUpdatesUseCase = mock(),
            hasSensitiveDescendantUseCase = hasSensitiveDescendantUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = mock(),
            getNodeByIdUseCase = getNodeByIdUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            monitorAccountDetailUseCase,
            hasSensitiveDescendantUseCase,
            getFeatureFlagValueUseCase
        )
    }

    @Test
    fun `test that sensitiveItemsCount updated the value is HIDDEN_NODE_NONE_SENSITIVE when getFeatureFlagValueUseCase is false`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                false
            )
            initUnderTest()
            underTest.checkSensitiveItems(listOf(10000L))

            underTest.sensitiveItemsCountFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_NONE_SENSITIVE)
            }
        }

    @Test
    fun `test that sensitiveItemsCount updated the value is HIDDEN_NODE_NONE_SENSITIVE when node is not FolderNode`() =
        runTest {
            val handles = listOf(10000L, 20000L, 30000L)
            val nodeIds = handles.map { NodeId(it) }
            val typedNodes = nodeIds.map { nodeId ->
                mock<TypedFileNode> {
                    on { id }.thenReturn(nodeId)
                }
            }

            typedNodes.onEachIndexed { index, node ->
                whenever(getNodeByIdUseCase(nodeIds[index])).thenReturn(node)
            }
            initUnderTest()
            underTest.checkSensitiveItems(handles)

            underTest.sensitiveItemsCountFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_NONE_SENSITIVE)
            }
        }

    @Test
    fun `test that sensitiveItemsCount updated the value is HIDDEN_NODE_NONE_SENSITIVE when isOutShare is true`() =
        runTest {
            val handles = listOf(10000L, 20000L, 30000L)
            val nodeIds = handles.map { NodeId(it) }
            val typedNodes = nodeIds.map { nodeId ->
                mock<TypedFolderNode> {
                    on { id }.thenReturn(nodeId)
                }
            }

            typedNodes.onEachIndexed { index, node ->
                whenever(node.isOutShare()).thenReturn(true)
                whenever(getNodeByIdUseCase(nodeIds[index])).thenReturn(node)
            }
            initUnderTest()
            underTest.checkSensitiveItems(handles)

            underTest.sensitiveItemsCountFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_NONE_SENSITIVE)
            }
        }

    @ParameterizedTest(name = "when isMarkedSensitive is [0], isSensitiveInherited is [1], hasSensitiveDescendantUseCase returns [2]")
    @MethodSource("provideSensitiveItemsTestData")
    fun `test that sensitiveItemsCount updated the value is HIDDEN_NODE_WARNING_TYPE_FOLDER`(
        isMarkedSensitive: Boolean,
        isSensitiveInherited: Boolean,
        hasSensitiveDescendant: Boolean,
    ) =
        runTest {
            val handles = listOf(10000L)
            val nodeIds = handles.map { NodeId(it) }
            val typedNodes = nodeIds.map { nodeId ->
                mock<TypedFolderNode> {
                    on { id }.thenReturn(nodeId)
                    on { this.isMarkedSensitive }.thenReturn(isMarkedSensitive)
                    on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
                }
            }

            typedNodes.onEachIndexed { index, node ->
                whenever(getNodeByIdUseCase(nodeIds[index])).thenReturn(node)
                whenever(hasSensitiveDescendantUseCase(nodeIds[index])).thenReturn(
                    hasSensitiveDescendant
                )
            }
            initUnderTest()
            underTest.checkSensitiveItems(handles)

            underTest.sensitiveItemsCountFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_WARNING_TYPE_FOLDER)
            }
        }

    private fun provideSensitiveItemsTestData() = listOf(
        Arguments.of(true, false, false),
        Arguments.of(false, true, false),
        Arguments.of(false, false, true),
    )

    @ParameterizedTest(name = "when isMarkedSensitive is [0], isSensitiveInherited is [1], hasSensitiveDescendantUseCase returns [2]")
    @MethodSource("provideSensitiveItemsTestData")
    fun `test that sensitiveItemsCount updated the value is HIDDEN_NODE_WARNING_TYPE_FOLDERS`(
        isMarkedSensitive: Boolean,
        isSensitiveInherited: Boolean,
        hasSensitiveDescendant: Boolean,
    ) =
        runTest {
            val handles = listOf(10000L, 20000L, 30000L)
            val nodeIds = handles.map { NodeId(it) }
            val typedNodes = nodeIds.map { nodeId ->
                mock<TypedFolderNode> {
                    on { id }.thenReturn(nodeId)
                    on { this.isMarkedSensitive }.thenReturn(isMarkedSensitive)
                    on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
                }
            }

            typedNodes.onEachIndexed { index, node ->
                whenever(getNodeByIdUseCase(nodeIds[index])).thenReturn(node)
                whenever(hasSensitiveDescendantUseCase(nodeIds[index])).thenReturn(
                    hasSensitiveDescendant
                )
            }
            initUnderTest()
            underTest.checkSensitiveItems(handles)

            underTest.sensitiveItemsCountFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_WARNING_TYPE_FOLDERS)
            }
        }
}