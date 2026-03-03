package mega.privacy.android.feature.clouddrive.presentation.folderlink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FolderLinkViewModelTest {

    private val loginToFolderUseCase: LoginToFolderUseCase = mock()
    private val hasCredentialsUseCase: HasCredentialsUseCase = mock()

    private lateinit var underTest: FolderLinkViewModel

    private fun initViewModel(
        args: FolderLinkViewModel.Args = FolderLinkViewModel.Args(
            uriString = null,
            nodeHandle = null
        ),
    ) {
        underTest = FolderLinkViewModel(
            loginToFolderUseCase = loginToFolderUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase,
            args = args,
        )
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(loginToFolderUseCase, hasCredentialsUseCase)
    }

    @Test
    fun `test that init does not call loginToFolderUseCase when uriString is null`() =
        runTest {
            initViewModel(FolderLinkViewModel.Args(uriString = null, nodeHandle = null))

            underTest.uiState.test {
                assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Loading)
            }
            verifyNoInteractions(loginToFolderUseCase)
        }

    @Test
    fun `test that init emits Loaded with hasDbCredentials when login succeeds and hasCredentials is true`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(hasCredentialsUseCase()).thenReturn(true)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                val loaded = awaitItem().contentState as FolderLinkContentState.Loaded
                assertThat(loaded.hasDbCredentials).isTrue()
            }
        }

    @Test
    fun `test that init emits Loaded without hasDbCredentials when login succeeds and hasCredentials is false`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                val loaded = awaitItem().contentState as FolderLinkContentState.Loaded
                assertThat(loaded.hasDbCredentials).isFalse()
            }
        }

    @Test
    fun `test that init emits DecryptionKeyRequired when login returns API_INCOMPLETE`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                val state = awaitItem().contentState as FolderLinkContentState.DecryptionKeyRequired
                assertThat(state.url).isEqualTo(url)
                assertThat(state.isKeyIncorrect).isFalse()
            }
        }

    @Test
    fun `test that init emits DecryptionKeyRequired with isKeyIncorrect when login returns INCORRECT_KEY`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.INCORRECT_KEY)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                val state = awaitItem().contentState as FolderLinkContentState.DecryptionKeyRequired
                assertThat(state.url).isEqualTo(url)
                assertThat(state.isKeyIncorrect).isTrue()
            }
        }

    @Test
    fun `test that init emits Unavailable when login returns ERROR`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.ERROR)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Unavailable)
            }
        }

    @Test
    fun `test that init emits Unavailable when loginToFolderUseCase throws`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenThrow(RuntimeException())
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Unavailable)
            }
        }

    @Test
    fun `test that processAction updates state to Unavailable when DecryptionKeyDialogDismissed`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                awaitItem()
                underTest.processAction(FolderLinkAction.DecryptionKeyDialogDismissed)
                assertThat(awaitItem().contentState).isEqualTo(FolderLinkContentState.Unavailable)
            }
        }

    @Test
    fun `test that processAction appends key and retries login when DecryptionKeyEntered and state is DecryptionKeyRequired`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            whenever(loginToFolderUseCase("$url#key")).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                awaitItem()
                underTest.processAction(FolderLinkAction.DecryptionKeyEntered("key"))
                assertThat(awaitItem().contentState).isEqualTo(
                    FolderLinkContentState.Loaded(hasDbCredentials = false)
                )
            }
            verify(loginToFolderUseCase).invoke(url)
            verify(loginToFolderUseCase).invoke("$url#key")
        }

    @Test
    fun `test that processAction appends key with exclamation when DecryptionKeyEntered and link has old format`() =
        runTest {
            val url = "https://mega.nz/#F!abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            whenever(loginToFolderUseCase("$url!key")).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                awaitItem()
                underTest.processAction(FolderLinkAction.DecryptionKeyEntered("key"))
                assertThat(awaitItem().contentState).isEqualTo(
                    FolderLinkContentState.Loaded(hasDbCredentials = false)
                )
            }
            verify(loginToFolderUseCase).invoke("$url!key")
        }

    @Test
    fun `test that processAction trims key when DecryptionKeyEntered`() =
        runTest {
            val url = "https://mega.nz/folder/abc"
            whenever(loginToFolderUseCase(url)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            whenever(loginToFolderUseCase("$url#key")).thenReturn(FolderLoginStatus.SUCCESS)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            initViewModel(FolderLinkViewModel.Args(uriString = url, nodeHandle = null))

            underTest.uiState.test {
                awaitItem()
                underTest.processAction(FolderLinkAction.DecryptionKeyEntered("  key  "))
                assertThat(awaitItem().contentState).isEqualTo(
                    FolderLinkContentState.Loaded(hasDbCredentials = false)
                )
            }
            verify(loginToFolderUseCase).invoke("$url#key")
        }
}