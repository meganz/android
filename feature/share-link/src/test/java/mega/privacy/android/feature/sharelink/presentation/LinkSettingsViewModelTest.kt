package mega.privacy.android.feature.sharelink.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.filelink.EncryptLinkWithPasswordUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class LinkSettingsViewModelTest {

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val exportNodeUseCase = mock<ExportNodeUseCase>()
    private val encryptLinkWithPasswordUseCase = mock<EncryptLinkWithPasswordUseCase>()
    private val getPasswordStrengthUseCase = mock<GetPasswordStrengthUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()

    @BeforeEach
    fun setUp() {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(AccountDetail()))
    }

    @AfterEach
    fun tearDown() {
        reset(
            getNodeByIdUseCase,
            exportNodeUseCase,
            encryptLinkWithPasswordUseCase,
            getPasswordStrengthUseCase,
            monitorAccountDetailUseCase,
        )
    }

    private suspend fun stubNode() {
        val node = mock<TypedFileNode> {
            on { exportedData } doReturn ExportedData(PUBLIC_LINK, 0L)
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
    }

    private fun createUnderTest() = LinkSettingsViewModel(
        args = LinkSettingsViewModel.Args(handles = listOf(NODE_HANDLE)),
        getNodeByIdUseCase = getNodeByIdUseCase,
        exportNodeUseCase = exportNodeUseCase,
        encryptLinkWithPasswordUseCase = encryptLinkWithPasswordUseCase,
        getPasswordStrengthUseCase = getPasswordStrengthUseCase,
        monitorAccountDetailUseCase = monitorAccountDetailUseCase,
    )

    private suspend fun ReceiveTurbine<LinkSettingsUiState>.awaitUntil(
        predicate: (LinkSettingsUiState) -> Boolean,
    ): LinkSettingsUiState {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun `test that uiState is loading until the account detail arrives`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()

            underTest.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()
                assertThat(awaitItem().isLoading).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that all options are disabled and Save disabled once loaded`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isSeparateKeyEnabled).isFalse()
                assertThat(state.isExpiryEnabled).isFalse()
                assertThat(state.isPasswordEnabled).isFalse()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSeparateKeyEnabled enables the option and Save`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                val state = awaitItem()
                assertThat(state.isSeparateKeyEnabled).isTrue()
                assertThat(state.isSaveEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that enabling expiry keeps Save disabled until a date is chosen`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onExpiryEnabled(true)
                assertThat(awaitItem().isSaveEnabled).isFalse()

                underTest.onExpiryDateChanged(EXPIRY_TIME)
                val state = awaitItem()
                assertThat(state.expiryDate).isEqualTo(EXPIRY_TIME)
                assertThat(state.isSaveEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that enabling password keeps Save disabled until a password is entered`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                assertThat(awaitItem().isSaveEnabled).isFalse()

                underTest.onPasswordChanged(PASSWORD)
                val state = awaitUntil { it.passwordStrength == PasswordStrength.STRONG }
                assertThat(state.isSaveEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState carries the account type from monitorAccountDetailUseCase`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val levelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_I
            }
            whenever(monitorAccountDetailUseCase())
                .thenReturn(flowOf(AccountDetail(levelDetail = levelDetail)))
            val underTest = createUnderTest()

            underTest.uiState.test {
                val state = awaitUntil { it.accountType != null }
                assertThat(state.accountType).isEqualTo(AccountType.PRO_I)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave exports the node with the chosen expiry date and triggers the saved event`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenReturn(PUBLIC_LINK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onExpiryEnabled(true)
                underTest.onExpiryDateChanged(EXPIRY_TIME)
                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            verify(exportNodeUseCase).invoke(NodeId(NODE_HANDLE), EXPIRY_TIME, CALLER_NAME)
        }

    @Test
    fun `test that onSave encrypts the link with the entered password`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD)).thenReturn("encrypted")
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(PASSWORD)
                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            verify(encryptLinkWithPasswordUseCase).invoke(PUBLIC_LINK, PASSWORD)
        }

    @Test
    fun `test that onSave triggers the error event when applying changes fails`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(exportNodeUseCase(any(), anyOrNull(), any()))
                .thenAnswer { throw RuntimeException("boom") }
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onExpiryEnabled(true)
                underTest.onExpiryDateChanged(EXPIRY_TIME)
                underTest.onSave()
                val state = awaitUntil { it.errorEvent == triggered }
                assertThat(state.isSaving).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave does nothing when nothing has changed`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.onSave()
            advanceUntilIdle()

            verifyNoInteractions(exportNodeUseCase, encryptLinkWithPasswordUseCase)
        }

    private companion object {
        const val NODE_HANDLE = 123L
        const val PUBLIC_LINK = "https://mega.nz/file/abc"
        const val PASSWORD = "Str0ngP@ss"
        const val EXPIRY_TIME = 1_800_000_000L
        const val CALLER_NAME = "LinkSettingsViewModel"

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
