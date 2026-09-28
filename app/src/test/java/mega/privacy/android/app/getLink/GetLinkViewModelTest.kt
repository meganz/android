package mega.privacy.android.app.getLink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.InstantExecutorExtension
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_NONE_SENSITIVE
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_WARNING_TYPE_FOLDER
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_WARNING_TYPE_LINKS
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.ShouldShowCopyrightUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class, InstantExecutorExtension::class)
class GetLinkViewModelTest {
    private lateinit var underTest: GetLinkViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val hasSensitiveInheritedUseCase = mock<HasSensitiveInheritedUseCase>()
    private val hasSensitiveDescendantUseCase = mock<HasSensitiveDescendantUseCase>()
    private val shouldShowCopyrightUseCase = mock<ShouldShowCopyrightUseCase>()
    private val exportNodeUseCase = mock<ExportNodeUseCase>()
    private val megaApi = mock<MegaApiAndroid>()

    @BeforeEach
    fun setUp() {
        whenever(monitorAccountDetailUseCase()).thenReturn(flow { mock() })
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = GetLinkViewModel(
            megaApi = megaApi,
            dbH = mock(),
            encryptLinkWithPasswordUseCase = mock(),
            exportNodeUseCase = exportNodeUseCase,
            context = mock(),
            hasSensitiveDescendantUseCase = hasSensitiveDescendantUseCase,
            hasSensitiveInheritedUseCase = hasSensitiveInheritedUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = mock(),
            getNodeByIdUseCase = getNodeByIdUseCase,
            shouldShowCopyrightUseCase = shouldShowCopyrightUseCase,
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
            hasSensitiveInheritedUseCase,
            shouldShowCopyrightUseCase,
            exportNodeUseCase,
            megaApi,
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct value is returned when checking the copyright visibility`(shouldShow: Boolean) =
        runTest {
            whenever(shouldShowCopyrightUseCase()) doReturn shouldShow

            val result = underTest.shouldShowCopyright()

            assertThat(result).isEqualTo(shouldShow)
        }

    @Test
    fun `test that awaitCopyrightConsent returns immediately when the terms were already accepted`() =
        runTest {
            whenever(shouldShowCopyrightUseCase()) doReturn false

            underTest.awaitCopyrightConsent()
        }

    @Test
    fun `test that awaitCopyrightConsent waits until the terms are agreed`() = runTest {
        whenever(shouldShowCopyrightUseCase()) doReturn true

        val consent = async { underTest.awaitCopyrightConsent() }
        advanceUntilIdle()
        assertThat(consent.isCompleted).isFalse()

        underTest.agreeCopyrightTerms()
        advanceUntilIdle()

        assertThat(consent.isCompleted).isTrue()
        consent.await()
    }

    @Test
    fun `test that awaitCopyrightConsent keeps waiting while the terms are not agreed`() = runTest {
        whenever(shouldShowCopyrightUseCase()) doReturn true

        val consent = async { underTest.awaitCopyrightConsent() }
        advanceUntilIdle()

        assertThat(consent.isCompleted).isFalse()
        consent.cancel()
    }

    @Test
    fun `test that export does not call exportNodeUseCase until the copyright terms are agreed`() =
        runTest {
            val nodeHandle = 12345L
            val megaNode = mock<MegaNode> { on { handle } doReturn nodeHandle }
            whenever(shouldShowCopyrightUseCase()) doReturn true
            whenever(megaApi.getNodeByHandle(nodeHandle)) doReturn megaNode
            underTest.initNode(nodeHandle)

            underTest.export()
            advanceUntilIdle()
            verifyNoInteractions(exportNodeUseCase)

            underTest.agreeCopyrightTerms()
            advanceUntilIdle()

            verify(exportNodeUseCase)(
                nodeToExport = NodeId(nodeHandle),
                callerName = "GetLinkViewModel",
            )
        }

    @ParameterizedTest
    @EnumSource(AccountType::class)
    fun `test that isPro returns true only for paid accounts`(accountType: AccountType) = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(
            flowOf(
                AccountDetail(
                    levelDetail = AccountLevelDetail(
                        accountType = accountType,
                        subscriptionStatus = null,
                        subscriptionRenewTime = 0L,
                        accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                        proExpirationTime = 0L,
                        accountPlanDetail = null,
                        accountSubscriptionDetailList = emptyList(),
                    )
                )
            )
        )
        initUnderTest()

        assertThat(underTest.isPro()).isEqualTo(accountType.isPaid)
    }
}
