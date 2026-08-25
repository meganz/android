package mega.privacy.android.feature.sharelink.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.filelink.EncryptLinkWithPasswordUseCase
import mega.privacy.android.domain.usecase.link.SplitLinkAndKeyUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.feature.sharelink.session.LinkPassword
import mega.privacy.android.feature.sharelink.session.ShareLinkSession
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LinkSettingsViewModelTest {

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val exportNodeUseCase = mock<ExportNodeUseCase>()
    private val encryptLinkWithPasswordUseCase = mock<EncryptLinkWithPasswordUseCase>()
    private val getPasswordStrengthUseCase = mock<GetPasswordStrengthUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    // Real instance: a plain state holder, so a mock would only restate what it already does.
    private val session = ShareLinkSession()
    private val splitLinkAndKeyUseCase = SplitLinkAndKeyUseCase()

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

    private suspend fun stubNodeWithExpiry() {
        val node = mock<TypedFileNode> {
            on { exportedData } doReturn ExportedData(PUBLIC_LINK, 0L, EXPIRY_TIME_SECONDS)
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
    }

    private suspend fun stubNodeWithoutLink() {
        val node = mock<TypedFileNode> {
            on { exportedData } doReturn null
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
    }

    private suspend fun stubFolderNode() {
        val node = mock<TypedFolderNode> {
            on { exportedData } doReturn ExportedData(PUBLIC_LINK, 0L)
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
    }

    private suspend fun stubNodeWithExpiry(expirationSeconds: Long) {
        val node = mock<TypedFileNode> {
            on { exportedData } doReturn ExportedData(PUBLIC_LINK, 0L, expirationSeconds)
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
    }

    private fun stubExistingPassword(password: String = OLD_PASSWORD) {
        session.password.value = LinkPassword(password, PUBLIC_LINK)
    }

    private fun stubCachedSeparateKey() {
        session.isKeySeparate.value = true
    }

    private fun createUnderTest(
        subject: ShareLinkSubject = ShareLinkSubject.Nodes(listOf(NODE_HANDLE)),
        session: ShareLinkSession = this.session,
    ) = LinkSettingsViewModel(
        args = LinkSettingsViewModel.Args(subject = subject),
        getNodeByIdUseCase = getNodeByIdUseCase,
        exportNodeUseCase = exportNodeUseCase,
        encryptLinkWithPasswordUseCase = encryptLinkWithPasswordUseCase,
        getPasswordStrengthUseCase = getPasswordStrengthUseCase,
        monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        splitLinkAndKeyUseCase = splitLinkAndKeyUseCase,
        session = session,
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
    fun `test that the first loaded state already has the existing expiry applied`() =
        runTest(extension.testDispatcher) {
            stubNodeWithExpiry()
            val underTest = createUnderTest()

            underTest.uiState.test {
                // The toggle must not flip on after the content is visible: the very first state
                // the screen renders has to carry the expiry already.
                val firstLoaded = awaitUntil { !it.isLoading }
                assertThat(firstLoaded.isExpiryEnabled).isTrue()
                assertThat(firstLoaded.isExpiryAlreadySet).isTrue()
                assertThat(firstLoaded.expiryDate).isEqualTo(EXPIRY_TIME)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState stays loading while the node is still being read`() =
        runTest(extension.testDispatcher) {
            // The bug: the account detail arrives first, and loading used to clear on it alone,
            // showing the content before the node's expiry was known.
            val gate = CompletableDeferred<TypedFileNode>()
            whenever { getNodeByIdUseCase(NodeId(NODE_HANDLE)) } doSuspendableAnswer { gate.await() }
            val underTest = createUnderTest()

            advanceUntilIdle()
            assertThat(underTest.uiState.value.isLoading).isTrue()

            gate.complete(
                mock { on { exportedData } doReturn ExportedData(PUBLIC_LINK, 0L, EXPIRY_TIME_SECONDS) }
            )
            advanceUntilIdle()

            val loaded = underTest.uiState.value
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.isExpiryEnabled).isTrue()
        }

    @Test
    fun `test that a later account change does not overwrite an edit already made`() =
        runTest(extension.testDispatcher) {
            val accounts = MutableStateFlow(AccountDetail())
            whenever(monitorAccountDetailUseCase()).thenReturn(accounts)
            stubNodeWithExpiry()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.onExpiryEnabled(false)
            advanceUntilIdle()

            // The node is read once but the account type keeps arriving; a second emission must
            // not re-seed the expiry the user just turned off.
            val levelDetail = mock<AccountLevelDetail> { on { accountType } doReturn AccountType.PRO_I }
            accounts.value = AccountDetail(levelDetail = levelDetail)
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.isExpiryEnabled).isFalse()
            assertThat(state.expiryDate).isNull()
            assertThat(state.accountType).isEqualTo(AccountType.PRO_I)
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
    fun `test that isFolder is false when the node is a file`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().isFolder).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isFolder is true when the node is a folder`() =
        runTest(extension.testDispatcher) {
            stubFolderNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().isFolder).isTrue()
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
    fun `test that Save stays disabled when the password entered is too weak`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(VERY_WEAK_PASSWORD))
                .thenReturn(PasswordStrength.VERY_WEAK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(VERY_WEAK_PASSWORD)
                val state = awaitUntil { it.passwordStrength == PasswordStrength.VERY_WEAK }
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that Save enables when the password entered is weak but not the weakest grade`() =
        runTest(extension.testDispatcher) {
            // Legacy parity: the old password screen refused PASSWORD_STRENGTH_VERYWEAK alone, so
            // everything the SDK grades above it still saves.
            stubNode()
            whenever(getPasswordStrengthUseCase(WEAK_PASSWORD)).thenReturn(PasswordStrength.WEAK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(WEAK_PASSWORD)
                val state = awaitUntil { it.passwordStrength == PasswordStrength.WEAK }
                assertThat(state.isSaveEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that Save stays disabled when the password entered cannot be graded`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(VERY_WEAK_PASSWORD))
                .thenReturn(PasswordStrength.INVALID)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(VERY_WEAK_PASSWORD)
                val state = awaitUntil { it.passwordStrength == PasswordStrength.INVALID }
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave applies nothing when the password entered is too weak`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(VERY_WEAK_PASSWORD))
                .thenReturn(PasswordStrength.VERY_WEAK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.onPasswordEnabled(true)
            underTest.onPasswordChanged(VERY_WEAK_PASSWORD)
            advanceUntilIdle()
            underTest.onSave()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.savedEvent).isEqualTo(consumed)
            verifyNoInteractions(encryptLinkWithPasswordUseCase)
            assertThat(session.password.value).isNull()
        }

    @Test
    fun `test that a password the link already carries does not block an unrelated change`() =
        runTest(extension.testDispatcher) {
            // Only a password being set is graded. One the link already has predates the minimum
            // and must not hold an expiry change hostage, with no way to clear the block.
            stubNode()
            stubExistingPassword(VERY_WEAK_PASSWORD)
            whenever(getPasswordStrengthUseCase(VERY_WEAK_PASSWORD))
                .thenReturn(PasswordStrength.VERY_WEAK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onExpiryEnabled(true)
                underTest.onExpiryDateChanged(EXPIRY_TIME)
                val state = awaitUntil { it.expiryDate == EXPIRY_TIME }
                assertThat(state.isSaveEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that grading waits for typing to pause`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.onPasswordEnabled(true)
            underTest.onPasswordChanged(PASSWORD)

            advanceTimeBy(HALF_OF_STRENGTH_DEBOUNCE)
            verifyNoInteractions(getPasswordStrengthUseCase)

            advanceUntilIdle()
            verify(getPasswordStrengthUseCase).invoke(PASSWORD)
        }

    @Test
    fun `test that a burst of keystrokes is graded once`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.onPasswordEnabled(true)
            PASSWORD.indices.forEach { index ->
                underTest.onPasswordChanged(PASSWORD.take(index + 1))
            }
            advanceUntilIdle()

            // One grading for the whole word, not one per character.
            verify(getPasswordStrengthUseCase).invoke(PASSWORD)
            verifyNoMoreInteractions(getPasswordStrengthUseCase)
            assertThat(underTest.uiState.value.passwordStrength)
                .isEqualTo(PasswordStrength.STRONG)
        }

    @Test
    fun `test that a grading already running is cancelled when the password changes again`() =
        runTest(extension.testDispatcher) {
            // Past the debounce, so the SDK call is genuinely in flight when the next change lands.
            stubNode()
            val slowGrading = CompletableDeferred<PasswordStrength>()
            whenever { getPasswordStrengthUseCase(VERY_WEAK_PASSWORD) } doSuspendableAnswer {
                slowGrading.await()
            }
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.onPasswordEnabled(true)
            underTest.onPasswordChanged(VERY_WEAK_PASSWORD)
            advanceUntilIdle()

            underTest.onPasswordChanged(PASSWORD)
            advanceUntilIdle()
            slowGrading.complete(PasswordStrength.VERY_WEAK)
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.passwordStrength).isEqualTo(PasswordStrength.STRONG)
            assertThat(state.isSaveEnabled).isTrue()
        }

    @Test
    fun `test that onSave grades the password being saved rather than the one last shown`() =
        runTest(extension.testDispatcher) {
            // Inside the debounce window the label still reads Strong for a password that has
            // already been replaced with a very weak one. Save must not go by that.
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(getPasswordStrengthUseCase(VERY_WEAK_PASSWORD))
                .thenReturn(PasswordStrength.VERY_WEAK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.onPasswordEnabled(true)
            underTest.onPasswordChanged(PASSWORD)
            advanceUntilIdle()
            assertThat(underTest.uiState.value.isSaveEnabled).isTrue()

            underTest.onPasswordChanged(VERY_WEAK_PASSWORD)
            underTest.onSave()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.savedEvent).isEqualTo(consumed)
            assertThat(state.isSaving).isFalse()
            assertThat(state.passwordStrength).isEqualTo(PasswordStrength.VERY_WEAK)
            assertThat(state.isSaveEnabled).isFalse()
            verifyNoInteractions(encryptLinkWithPasswordUseCase)
        }

    @Test
    fun `test that hasUnsavedChanges is true once an option changes even if not yet saveable`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onExpiryEnabled(true)
                val state = awaitItem()
                assertThat(state.hasUnsavedChanges).isTrue()
                assertThat(state.isSaveEnabled).isFalse()
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

            verify(exportNodeUseCase).invoke(NodeId(NODE_HANDLE), EXPIRY_TIME_SECONDS, CALLER_NAME)
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
    fun `test that onSave carries the encrypted link as savedLink when a password is set`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD))
                .thenReturn(ENCRYPTED_LINK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(PASSWORD)
                underTest.onSave()
                val state = awaitUntil { it.savedEvent == triggered }
                assertThat(state.savedLink).isEqualTo(ENCRYPTED_LINK)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave carries the public link as savedLink when only the expiry changes`() =
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
                val state = awaitUntil { it.savedEvent == triggered }
                assertThat(state.savedLink).isEqualTo(PUBLIC_LINK)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave carries the key-less link as savedLink when the key is sent separately`() =
        runTest(extension.testDispatcher) {
            val node = mock<TypedFileNode> {
                on { exportedData } doReturn ExportedData(LINK_WITH_KEY, 0L)
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                underTest.onSave()
                val state = awaitUntil { it.savedEvent == triggered }
                assertThat(state.savedLink).isEqualTo(PUBLIC_LINK)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave carries the key-less link as savedLink when a password is removed and the key is sent separately`() =
        runTest(extension.testDispatcher) {
            // Regression: removing a password used to return the full link and shadow the
            // separate-key change made in the same save, so the decryption key landed on the
            // clipboard.
            val node = mock<TypedFileNode> {
                on { exportedData } doReturn ExportedData(LINK_WITH_KEY, 0L)
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            stubExistingPassword()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(false)
                underTest.onSeparateKeyEnabled(true)
                underTest.onSave()
                val state = awaitUntil { it.savedEvent == triggered }
                assertThat(state.savedLink).isEqualTo(PUBLIC_LINK)
                assertThat(state.savedLink).doesNotContain("key123")
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(session.password.value).isNull()
        }

    @Test
    fun `test that onSave still clears the password cache when the key is sent separately`() =
        runTest(extension.testDispatcher) {
            // The reordering must not cost the removal itself: the cache write and the returned
            // link are now decided separately, so both still have to happen.
            val node = mock<TypedFileNode> {
                on { exportedData } doReturn ExportedData(LINK_WITH_KEY, 0L)
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            stubExistingPassword()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            // Enabling the separate key turns the password off on its own, so the removal applies
            // even without an explicit toggle.
            assertThat(session.password.value).isNull()
        }

    @Test
    fun `test that onSave applies nothing when the expiry export fails`() =
        runTest(extension.testDispatcher) {
            // Regression: the caches used to be written before the export, so a failed export left
            // the Share link screen showing changes the user had just been told did not apply.
            stubNode()
            stubCachedSeparateKey()
            whenever(exportNodeUseCase(any(), anyOrNull(), any()))
                .thenAnswer { throw RuntimeException("export failed") }
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(false)
                underTest.onExpiryEnabled(true)
                underTest.onExpiryDateChanged(EXPIRY_TIME)
                underTest.onSave()
                awaitUntil { it.errorEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            // Still what it was before the save: turning it off was never committed.
            assertThat(session.isKeySeparate.value).isTrue()
            assertThat(session.password.value).isNull()
        }

    @Test
    fun `test that onSave applies nothing when encrypting the password fails`() =
        runTest(extension.testDispatcher) {
            // Encrypting runs first precisely so its failure costs nothing: no export, no caches.
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD))
                .thenAnswer { throw RuntimeException("encrypt failed") }
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onExpiryEnabled(true)
                underTest.onExpiryDateChanged(EXPIRY_TIME)
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(PASSWORD)
                underTest.onSave()
                awaitUntil { it.errorEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            verifyNoInteractions(exportNodeUseCase)
            assertThat(session.password.value).isNull()
            assertThat(session.isKeySeparate.value).isFalse()
        }

    @Test
    fun `test that re-enabling the expiry restores the date the link already had`() =
        runTest(extension.testDispatcher) {
            // Regression: toggling off nulled the date and toggling back on restored that null,
            // leaving the row on with an empty field and Save disabled for no visible reason.
            stubNodeWithExpiry()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitUntil { !it.isLoading }
                underTest.onExpiryEnabled(false)
                assertThat(awaitUntil { !it.isExpiryEnabled }.expiryDate).isNull()

                underTest.onExpiryEnabled(true)
                val reEnabled = awaitUntil { it.isExpiryEnabled }
                assertThat(reEnabled.expiryDate).isEqualTo(EXPIRY_TIME)
                assertThat(reEnabled.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave falls back to the plain link when the encrypted link comes back empty`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD)).thenReturn("")
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(PASSWORD)
                underTest.onSave()
                val state = awaitUntil { it.savedEvent == triggered }
                // Never the empty string: that would be copied to the clipboard as a success.
                assertThat(state.savedLink).isEqualTo(PUBLIC_LINK)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(session.password.value).isEqualTo(LinkPassword(PASSWORD, null))
        }

    @Test
    fun `test that an album save carries the key-less link so the stale clipboard is replaced`() =
        runTest(extension.testDispatcher) {
            // Regression: an album is not a node, so no link was ever read for it and the save
            // copied nothing. The clipboard kept the link the Share link screen had put there on
            // arrival — key included — so a user who had just separated the link and key pasted
            // the key anyway.
            session.publicLink = ALBUM_LINK_WITH_KEY
            val underTest = createUnderTest(ShareLinkSubject.Album(ALBUM_ID))
            advanceUntilIdle()

            underTest.uiState.test {
                awaitUntil { !it.isLoading }
                underTest.onSeparateKeyEnabled(true)
                underTest.onSave()
                val state = awaitUntil { it.savedEvent == triggered }
                assertThat(state.savedLink).isEqualTo(ALBUM_LINK)
                assertThat(state.savedLink).doesNotContain("albumKey")
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(session.isKeySeparate.value).isTrue()
            verifyNoInteractions(exportNodeUseCase)
        }

    @Test
    fun `test that an album save carries the full link again when the key is no longer separate`() =
        runTest(extension.testDispatcher) {
            session.publicLink = ALBUM_LINK_WITH_KEY
            session.isKeySeparate.value = true
            val underTest = createUnderTest(ShareLinkSubject.Album(ALBUM_ID))
            advanceUntilIdle()

            underTest.uiState.test {
                awaitUntil { !it.isLoading }
                underTest.onSeparateKeyEnabled(false)
                underTest.onSave()
                val state = awaitUntil { it.savedEvent == triggered }
                assertThat(state.savedLink).isEqualTo(ALBUM_LINK_WITH_KEY)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that a failed node read keeps the link the Share link screen resolved`() =
        runTest(extension.testDispatcher) {
            // A transient node read failure used to null the link, turning the next save silent.
            session.publicLink = LINK_WITH_KEY
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE)))
                .thenAnswer { throw RuntimeException("node read failed") }
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitUntil { !it.isLoading }
                underTest.onSeparateKeyEnabled(true)
                underTest.onSave()
                val state = awaitUntil { it.savedEvent == triggered }
                assertThat(state.savedLink).isEqualTo(PUBLIC_LINK)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSavedEventConsumed clears savedLink`() =
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
                underTest.onSavedEventConsumed()
                val state = awaitUntil { it.savedEvent != triggered }
                assertThat(state.savedLink).isNull()
                cancelAndIgnoreRemainingEvents()
            }
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

    @Test
    fun `test that opening with an existing password pre-fills it without marking it dirty`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubExistingPassword()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isPasswordEnabled).isTrue()
                assertThat(state.isPasswordAlreadySet).isTrue()
                assertThat(state.password).isEqualTo(OLD_PASSWORD)
                assertThat(state.initialPassword).isEqualTo(OLD_PASSWORD)
                assertThat(state.hasUnsavedChanges).isFalse()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that a password saved in one session is not seen by the next session`() =
        runTest(extension.testDispatcher) {
            // Removing a link and creating a new one reuses the node handle, so a password kept
            // beyond the flow would re-attach itself to a link it was never set on.
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD)).thenReturn(ENCRYPTED_LINK)
            val firstVisit = createUnderTest(session = ShareLinkSession())
            advanceUntilIdle()
            firstVisit.onPasswordEnabled(true)
            firstVisit.onPasswordChanged(PASSWORD)
            advanceUntilIdle()
            firstVisit.onSave()
            advanceUntilIdle()

            val nextVisit = createUnderTest(session = ShareLinkSession())
            advanceUntilIdle()

            val state = nextVisit.uiState.value
            assertThat(state.isPasswordEnabled).isFalse()
            assertThat(state.isPasswordAlreadySet).isFalse()
            assertThat(state.password).isNull()
        }

    @Test
    fun `test that a password saved stays visible for the rest of the same session`() =
        runTest(extension.testDispatcher) {
            // The other half of the contract: the flow keeps the password while the user is still
            // in it, so returning to Link settings still pre-fills what they set.
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD)).thenReturn(ENCRYPTED_LINK)
            val session = ShareLinkSession()
            val firstOpen = createUnderTest(session = session)
            advanceUntilIdle()
            firstOpen.onPasswordEnabled(true)
            firstOpen.onPasswordChanged(PASSWORD)
            advanceUntilIdle()
            firstOpen.onSave()
            advanceUntilIdle()

            val reopened = createUnderTest(session = session)
            advanceUntilIdle()

            val state = reopened.uiState.value
            assertThat(state.isPasswordAlreadySet).isTrue()
            assertThat(state.password).isEqualTo(PASSWORD)
        }

    @Test
    fun `test that a separate key choice made in one session is not seen by the next session`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val firstVisit = createUnderTest(session = ShareLinkSession())
            advanceUntilIdle()
            firstVisit.onSeparateKeyEnabled(true)
            firstVisit.onSave()
            advanceUntilIdle()

            val nextVisit = createUnderTest(session = ShareLinkSession())
            advanceUntilIdle()

            assertThat(nextVisit.uiState.value.isSeparateKeyEnabled).isFalse()
        }

    @Test
    fun `test that removing an existing password enables Save and clears the cached password`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubExistingPassword()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(false)
                val dirty = awaitUntil { !it.isPasswordEnabled }
                assertThat(dirty.hasUnsavedChanges).isTrue()
                assertThat(dirty.isSaveEnabled).isTrue()

                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(session.password.value).isNull()
            verifyNoInteractions(encryptLinkWithPasswordUseCase)
        }

    @Test
    fun `test that changing an existing password onSave stores the new encrypted link in the cache`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubExistingPassword()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD)).thenReturn(ENCRYPTED_LINK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordChanged(PASSWORD)
                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            verify(encryptLinkWithPasswordUseCase).invoke(PUBLIC_LINK, PASSWORD)
            assertThat(session.password.value).isEqualTo(LinkPassword(PASSWORD, ENCRYPTED_LINK))
        }

    @Test
    fun `test that toggling password off clears the entered password and strength`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(PASSWORD)
                awaitUntil { it.passwordStrength == PasswordStrength.STRONG }

                underTest.onPasswordEnabled(false)
                val state = awaitUntil { !it.isPasswordEnabled }
                assertThat(state.password).isNull()
                assertThat(state.passwordStrength).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that clearing the password resets the strength`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(PASSWORD)
                awaitUntil { it.passwordStrength == PasswordStrength.STRONG }

                underTest.onPasswordChanged("")
                val state = awaitUntil { it.password == "" && it.passwordStrength == null }
                assertThat(state.password).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that setting a new password onSave stores the encrypted link in the cache`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD)).thenReturn(ENCRYPTED_LINK)
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

            assertThat(session.password.value).isEqualTo(LinkPassword(PASSWORD, ENCRYPTED_LINK))
        }

    @Test
    fun `test that setting a password onSave stores a null encrypted link when the node has no public link`() =
        runTest(extension.testDispatcher) {
            stubNodeWithoutLink()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
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

            assertThat(session.password.value).isEqualTo(LinkPassword(PASSWORD, null))
            verifyNoInteractions(encryptLinkWithPasswordUseCase)
        }

    @Test
    fun `test that changing an existing password back to the original keeps Save disabled`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubExistingPassword()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(getPasswordStrengthUseCase(OLD_PASSWORD)).thenReturn(PasswordStrength.MEDIUM)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordChanged(PASSWORD)
                awaitUntil { it.isSaveEnabled }

                underTest.onPasswordChanged(OLD_PASSWORD)
                val state = awaitUntil { it.password == OLD_PASSWORD }
                assertThat(state.hasUnsavedChanges).isFalse()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that opening with an existing expiry pre-fills it without marking it dirty`() =
        runTest(extension.testDispatcher) {
            stubNodeWithExpiry(EXPIRY_TIME_SECONDS)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitUntil { it.isExpiryAlreadySet }
                assertThat(state.isExpiryEnabled).isTrue()
                assertThat(state.expiryDate).isEqualTo(EXPIRY_TIME)
                assertThat(state.initialExpiryDate).isEqualTo(EXPIRY_TIME)
                assertThat(state.hasUnsavedChanges).isFalse()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that removing an existing expiry enables Save and re-exports without an expiry`() =
        runTest(extension.testDispatcher) {
            stubNodeWithExpiry(EXPIRY_TIME_SECONDS)
            whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenReturn(PUBLIC_LINK)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitUntil { it.isExpiryAlreadySet }
                underTest.onExpiryEnabled(false)
                val dirty = awaitUntil { !it.isExpiryEnabled }
                assertThat(dirty.isSaveEnabled).isTrue()

                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            verify(exportNodeUseCase).invoke(NodeId(NODE_HANDLE), null, CALLER_NAME)
        }

    @Test
    fun `test that changing an existing expiry re-exports with the new date in seconds`() =
        runTest(extension.testDispatcher) {
            stubNodeWithExpiry(EXPIRY_TIME_SECONDS)
            whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenReturn(PUBLIC_LINK)
            val underTest = createUnderTest()
            advanceUntilIdle()
            val newMillis = EXPIRY_TIME + MILLIS_PER_DAY

            underTest.uiState.test {
                awaitUntil { it.isExpiryAlreadySet }
                underTest.onExpiryDateChanged(newMillis)
                awaitUntil { it.expiryDate == newMillis && it.isSaveEnabled }

                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            verify(exportNodeUseCase)
                .invoke(NodeId(NODE_HANDLE), newMillis.milliseconds.inWholeSeconds, CALLER_NAME)
        }

    @Test
    fun `test that changing an existing expiry back to the original keeps Save disabled`() =
        runTest(extension.testDispatcher) {
            stubNodeWithExpiry(EXPIRY_TIME_SECONDS)
            val underTest = createUnderTest()
            advanceUntilIdle()
            val newMillis = EXPIRY_TIME + MILLIS_PER_DAY

            underTest.uiState.test {
                awaitUntil { it.isExpiryAlreadySet }
                underTest.onExpiryDateChanged(newMillis)
                awaitUntil { it.isSaveEnabled }

                underTest.onExpiryDateChanged(EXPIRY_TIME)
                val state = awaitUntil { it.expiryDate == EXPIRY_TIME }
                assertThat(state.hasUnsavedChanges).isFalse()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that disabling expiry clears the chosen date`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onExpiryEnabled(true)
                underTest.onExpiryDateChanged(EXPIRY_TIME)
                awaitUntil { it.expiryDate == EXPIRY_TIME }

                underTest.onExpiryEnabled(false)
                val state = awaitUntil { !it.isExpiryEnabled }
                assertThat(state.expiryDate).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave does not export the node when only the password changed`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD)).thenReturn(ENCRYPTED_LINK)
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

            verifyNoInteractions(exportNodeUseCase)
        }

    @Test
    fun `test that onSave does not touch the password cache when only the expiry changed`() =
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

            assertThat(session.password.value).isNull()
            verifyNoInteractions(encryptLinkWithPasswordUseCase)
        }

    @Test
    fun `test that opening with a cached separate-key preference pre-fills it without marking it dirty`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubCachedSeparateKey()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isSeparateKeyEnabled).isTrue()
                assertThat(state.initialSeparateKeyEnabled).isTrue()
                assertThat(state.hasUnsavedChanges).isFalse()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that toggling separate key back to the initial keeps Save disabled`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                awaitUntil { it.isSaveEnabled }

                underTest.onSeparateKeyEnabled(false)
                val state = awaitUntil { !it.isSeparateKeyEnabled }
                assertThat(state.hasUnsavedChanges).isFalse()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave persists the enabled separate-key preference and does not export`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(session.isKeySeparate.value).isTrue()
            verifyNoInteractions(exportNodeUseCase)
        }

    @Test
    fun `test that enabling separate key clears the entered password and strength`() =
        runTest(extension.testDispatcher) {
            stubNode()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(PASSWORD)
                awaitUntil { it.passwordStrength == PasswordStrength.STRONG }

                underTest.onSeparateKeyEnabled(true)
                val state = awaitUntil { it.isSeparateKeyEnabled }
                assertThat(state.isPasswordEnabled).isFalse()
                assertThat(state.password).isNull()
                assertThat(state.passwordStrength).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that enabling separate key turns off an already set password and enables Save`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubExistingPassword()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                val state = awaitUntil { it.isSeparateKeyEnabled }
                assertThat(state.isPasswordEnabled).isFalse()
                assertThat(state.password).isNull()
                assertThat(state.isSaveEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave removes the existing password from the cache when separate key is enabled`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubExistingPassword()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(session.isKeySeparate.value).isTrue()
            assertThat(session.password.value).isNull()
            verifyNoInteractions(encryptLinkWithPasswordUseCase)
        }

    @Test
    fun `test that enabling password turns off the separate key`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubCachedSeparateKey()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onPasswordEnabled(true)
                val state = awaitUntil { it.isPasswordEnabled }
                assertThat(state.isSeparateKeyEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onSave clears the cached separate key when password is enabled`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubCachedSeparateKey()
            whenever(getPasswordStrengthUseCase(PASSWORD)).thenReturn(PasswordStrength.STRONG)
            whenever(encryptLinkWithPasswordUseCase(PUBLIC_LINK, PASSWORD)).thenReturn(ENCRYPTED_LINK)
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

            assertThat(session.isKeySeparate.value).isFalse()
            assertThat(session.password.value).isEqualTo(LinkPassword(PASSWORD, ENCRYPTED_LINK))
        }

    @Test
    fun `test that separate key and password are never both enabled whichever is toggled last`() =
        runTest(extension.testDispatcher) {
            stubNode()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                awaitUntil { it.isSeparateKeyEnabled }

                underTest.onPasswordEnabled(true)
                assertThat(awaitUntil { it.isPasswordEnabled }.isSeparateKeyEnabled).isFalse()

                underTest.onSeparateKeyEnabled(true)
                assertThat(awaitUntil { it.isSeparateKeyEnabled }.isPasswordEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that toggling separate key back off does not restore the password it cleared`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubExistingPassword()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(true)
                awaitUntil { it.isSeparateKeyEnabled }

                underTest.onSeparateKeyEnabled(false)
                val state = awaitUntil { !it.isSeparateKeyEnabled }
                assertThat(state.isPasswordEnabled).isFalse()
                assertThat(state.password).isNull()
                assertThat(state.isSaveEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that disabling a cached separate key onSave clears it in the cache`() =
        runTest(extension.testDispatcher) {
            stubNode()
            stubCachedSeparateKey()
            val underTest = createUnderTest()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitItem()
                underTest.onSeparateKeyEnabled(false)
                awaitUntil { it.isSaveEnabled }
                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(session.isKeySeparate.value).isFalse()
        }

    @Test
    fun `test that an album never reads the node or the password cache`() =
        runTest(extension.testDispatcher) {
            val underTest = createUnderTest(ShareLinkSubject.Album(ALBUM_ID))

            underTest.uiState.test {
                val state = awaitUntil { !it.isLoading }
                assertThat(state.isAlbum).isTrue()
                assertThat(state.isPasswordAlreadySet).isFalse()
                cancelAndIgnoreRemainingEvents()
            }

            verifyNoInteractions(getNodeByIdUseCase)
            assertThat(session.password.value).isNull()
        }

    @Test
    fun `test that the album separate key preference is seeded from the session`() =
        runTest(extension.testDispatcher) {
            session.isKeySeparate.value = true

            val underTest = createUnderTest(ShareLinkSubject.Album(ALBUM_ID))

            underTest.uiState.test {
                val state = awaitUntil { !it.isLoading }
                assertThat(state.isSeparateKeyEnabled).isTrue()
                assertThat(state.initialSeparateKeyEnabled).isTrue()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that an album ignores expiry and password changes and keeps Save disabled`() =
        runTest(extension.testDispatcher) {
            val underTest = createUnderTest(ShareLinkSubject.Album(ALBUM_ID))

            underTest.uiState.test {
                awaitUntil { !it.isLoading }

                underTest.onExpiryEnabled(true)
                underTest.onExpiryDateChanged(EXPIRY_TIME)
                underTest.onPasswordEnabled(true)
                underTest.onPasswordChanged(PASSWORD)
                expectNoEvents()

                val state = underTest.uiState.value
                assertThat(state.isExpiryEnabled).isFalse()
                assertThat(state.expiryDate).isNull()
                assertThat(state.isPasswordEnabled).isFalse()
                assertThat(state.password).isNull()
                assertThat(state.isSaveEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that saving an album writes only the separate key preference`() =
        runTest(extension.testDispatcher) {
            val underTest = createUnderTest(ShareLinkSubject.Album(ALBUM_ID))

            underTest.uiState.test {
                awaitUntil { !it.isLoading }
                underTest.onSeparateKeyEnabled(true)
                awaitUntil { it.isSaveEnabled }
                underTest.onSave()
                awaitUntil { it.savedEvent == triggered }
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(session.isKeySeparate.value).isTrue()
            verifyNoInteractions(exportNodeUseCase)
            verifyNoInteractions(encryptLinkWithPasswordUseCase)
        }

    private companion object {
        const val NODE_HANDLE = 123L
        const val ALBUM_ID = 987L
        const val PUBLIC_LINK = "https://mega.nz/file/abc"

        // The same link with its decryption key still attached, so the separate-key save has
        // something real to split.
        const val LINK_WITH_KEY = "$PUBLIC_LINK#key123"

        // An album link is a collection URL; the key sits after the '#' as it does for a node.
        const val ALBUM_LINK = "https://mega.nz/collection/xyz789"
        const val ALBUM_LINK_WITH_KEY = "$ALBUM_LINK#albumKey"
        const val ENCRYPTED_LINK = "https://mega.nz/#P!encrypted"
        const val PASSWORD = "Str0ngP@ss"
        const val OLD_PASSWORD = "0ldP@ssw0rd"

        // The SDK grades anything shorter than eight characters as VERY_WEAK, whatever it contains.
        // Half of LinkSettingsViewModel.STRENGTH_DEBOUNCE: far enough in to show that grading
        // has not started yet, without depending on the exact value.
        val HALF_OF_STRENGTH_DEBOUNCE = 150.milliseconds

        const val VERY_WEAK_PASSWORD = "abc"
        const val WEAK_PASSWORD = "password12"
        const val CALLER_NAME = "LinkSettingsViewModel"

        // A fixed, far-future instant (~2027) used as the expiry across the expiry tests.
        val EXPIRY_TIME_SECONDS = 1_800_000_000L
        val EXPIRY_TIME = EXPIRY_TIME_SECONDS.seconds.inWholeMilliseconds
        val MILLIS_PER_DAY = 1.days.inWholeMilliseconds

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
