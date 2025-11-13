package mega.privacy.android.feature.clouddrive.presentation.shares.links

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.filelink.DecryptPasswordProtectedLinkUseCase
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class OpenPasswordLinkViewModelTest {

    private val decryptPasswordProtectedLinkUseCase = mock<DecryptPasswordProtectedLinkUseCase>()
    private val getDecodedUrlRegexPatternTypeUseCase = mock<GetDecodedUrlRegexPatternTypeUseCase>()

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: OpenPasswordLinkViewModel

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = OpenPasswordLinkViewModel(
            decryptPasswordProtectedLinkUseCase,
            getDecodedUrlRegexPatternTypeUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            decryptPasswordProtectedLinkUseCase,
            getDecodedUrlRegexPatternTypeUseCase,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that decryptPasswordProtectedLink triggers file link event when successful file link decryption`() =
        runTest {
            val passwordProtectedLink = "passwordProtectedLink"
            val password = "password"
            val decryptedLink = "decryptedFileLink"
            whenever(
                decryptPasswordProtectedLinkUseCase(
                    passwordProtectedLink,
                    password
                )
            ) doReturn decryptedLink
            whenever(getDecodedUrlRegexPatternTypeUseCase(decryptedLink)) doReturn RegexPatternType.FILE_LINK

            viewModel.decryptPasswordProtectedLink(passwordProtectedLink, password)

            val actual = viewModel.uiState.value
            assertThat(actual.decryptedLinkEvent).isEqualTo(
                triggered(
                    DecryptedLink.FileLink(
                        decryptedLink
                    )
                )
            )
        }

    @Test
    fun `test that decryptPasswordProtectedLink triggers folder link event when successful folder link decryption`() =
        runTest {
            val passwordProtectedLink = "passwordProtectedLink"
            val password = "password"
            val decryptedLink = "decryptedFolderLink"
            whenever(
                decryptPasswordProtectedLinkUseCase(
                    passwordProtectedLink,
                    password
                )
            ) doReturn decryptedLink
            whenever(getDecodedUrlRegexPatternTypeUseCase(decryptedLink)) doReturn RegexPatternType.FOLDER_LINK

            viewModel.decryptPasswordProtectedLink(passwordProtectedLink, password)

            val emission = viewModel.uiState.value
            assertThat(emission.decryptedLinkEvent).isEqualTo(
                triggered(
                    DecryptedLink.FolderLink(
                        decryptedLink
                    )
                )
            )
        }

    @Test
    fun `test that decryptPasswordProtectedLink shows an error when decryption failure`() =
        runTest {
            val passwordProtectedLink = "passwordProtectedLink"
            val password = "password"
            whenever(
                decryptPasswordProtectedLinkUseCase(
                    passwordProtectedLink,
                    password
                )
            ) doReturn null

            viewModel.decryptPasswordProtectedLink(passwordProtectedLink, password)

            val emission = viewModel.uiState.value
            assertThat(emission.errorMessage).isTrue()
        }

    @Test
    fun `test that decryptPasswordProtectedLink shows an error on unknown link type`() = runTest {
        // Given
        val passwordProtectedLink = "passwordProtectedLink"
        val password = "password"
        val decryptedLink = "decryptedUnknownLink"
        whenever(
            decryptPasswordProtectedLinkUseCase(
                passwordProtectedLink,
                password
            )
        ) doReturn decryptedLink
        whenever(getDecodedUrlRegexPatternTypeUseCase(decryptedLink)) doReturn RegexPatternType.ALBUM_LINK

        viewModel.decryptPasswordProtectedLink(passwordProtectedLink, password)

        val emission = viewModel.uiState.value
        assertThat(emission.errorMessage).isTrue()
    }

    @Test
    fun `test that consumeDecryptedLinkEvent consumes the event`() = runTest {
        val passwordProtectedLink = "passwordProtectedLink"
        val password = "password"
        val decryptedLink = "decryptedFileLink"
        whenever(
            decryptPasswordProtectedLinkUseCase(
                passwordProtectedLink,
                password
            )
        ) doReturn decryptedLink
        whenever(getDecodedUrlRegexPatternTypeUseCase(decryptedLink)) doReturn RegexPatternType.FILE_LINK



        viewModel.decryptPasswordProtectedLink(passwordProtectedLink, password)
        assertThat(viewModel.uiState.value.decryptedLinkEvent)
            .isInstanceOf(StateEventWithContentTriggered::class.java)

        viewModel.consumeDecryptedLinkEvent()

        val emission = viewModel.uiState.value
        assertThat(emission.decryptedLinkEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that resetError resets the error message`() = runTest {
        val passwordProtectedLink = "passwordProtectedLink"
        val password = "password"
        whenever(
            decryptPasswordProtectedLinkUseCase(
                passwordProtectedLink,
                password
            )
        ) doReturn null

        viewModel.decryptPasswordProtectedLink(passwordProtectedLink, password)
        assertThat(viewModel.uiState.value.errorMessage).isTrue()

        viewModel.resetError()

        val emission = viewModel.uiState.value
        assertThat(emission.errorMessage).isFalse()
    }
}
