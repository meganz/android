package mega.privacy.android.app.deeplinks

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeepLinksViewModelTest {

    private lateinit var underTest: DeepLinksViewModel

    private val deepLinkHandler1 = mock<DeepLinkHandler>()
    private val deepLinkHandler2 = mock<DeepLinkHandler>()
    private val deepLinkHandlers = listOf(deepLinkHandler1, deepLinkHandler2)
    private val getDecodedUrlRegexPatternTypeUseCase = mock<GetDecodedUrlRegexPatternTypeUseCase>()
    private val getAccountCredentials = mock<GetAccountCredentialsUseCase>()

    private val linkString = "https://whatever"
    private val uri = mock<Uri> {
        on { toString() } doReturn linkString
    }
    private val regexPatternType = RegexPatternType.CONTACT_LINK
    private val navKey = mock<NavKey>()
    private val navKeys = listOf(navKey)
    private val argsWithUri = DeepLinksViewModel.Args(uri)
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val argsWithUriAndRegex = DeepLinksViewModel.Args(uri, regexPatternType)

    private fun initViewModel(
        args: DeepLinksViewModel.Args,
    ) {
        underTest = DeepLinksViewModel(
            deepLinkHandlers = deepLinkHandlers,
            getDecodedUrlRegexPatternTypeUseCase = getDecodedUrlRegexPatternTypeUseCase,
            getAccountCredentials = getAccountCredentials,
            snackbarEventQueue = snackbarEventQueue,
            args = args,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            deepLinkHandler1,
            deepLinkHandler2,
            getDecodedUrlRegexPatternTypeUseCase,
            getAccountCredentials,
            snackbarEventQueue,
        )
    }

    @Test
    fun `test that state is updated with nav keys if deep link is handled when args only contains the uri`() =
        runTest {
            whenever(getDecodedUrlRegexPatternTypeUseCase(linkString)) doReturn regexPatternType
            whenever(getAccountCredentials()) doReturn null
            whenever(
                deepLinkHandler1.getNavKeysInternal(uri, regexPatternType, false)
            ) doReturn null
            whenever(
                deepLinkHandler2.getNavKeysInternal(uri, regexPatternType, false)
            ) doReturn navKeys

            initViewModel(argsWithUri)

            underTest.uiState.map { it.navKeys }.test {
                assertThat(awaitItem()).isEqualTo(navKeys)
            }

            verifyNoInteractions(snackbarEventQueue)
        }

    @Test
    fun `test that state is updated with empty nav keys and snackbarEventQueue is invoked if deep link is not handled when args only contains the uri`() =
        runTest {
            whenever(getDecodedUrlRegexPatternTypeUseCase(linkString)) doReturn regexPatternType
            whenever(getAccountCredentials()) doReturn null
            whenever(
                deepLinkHandler1.getNavKeysInternal(uri, regexPatternType, false)
            ) doReturn null
            whenever(
                deepLinkHandler2.getNavKeysInternal(uri, regexPatternType, false)
            ) doReturn null

            initViewModel(argsWithUri)

            underTest.uiState.map { it.navKeys }.test {
                assertThat(awaitItem()).isEmpty()
            }

            verify(snackbarEventQueue).queueMessage(R.string.open_link_not_valid_link)
        }

    @Test
    fun `test that state is updated with nav keys if deep link is handled when args contains the uri and the regexPatternType`() =
        runTest {
            whenever(
                deepLinkHandler1.getNavKeysInternal(uri, regexPatternType, true)
            ) doReturn null
            whenever(
                deepLinkHandler2.getNavKeysInternal(uri, regexPatternType, true)
            ) doReturn navKeys

            initViewModel(argsWithUriAndRegex)

            underTest.uiState.map { it.navKeys }.test {
                assertThat(awaitItem()).isEqualTo(navKeys)
            }

            verifyNoInteractions(getDecodedUrlRegexPatternTypeUseCase)
            verifyNoInteractions(getAccountCredentials)
            verifyNoInteractions(snackbarEventQueue)
        }

    @Test
    fun `test that state is updated with empty nav keys and snackbarEventQueue is invoked if deep link is not handled when args contains the uri and the regexPatternType`() =
        runTest {
            whenever(
                deepLinkHandler1.getNavKeysInternal(uri, regexPatternType, true)
            ) doReturn null
            whenever(
                deepLinkHandler2.getNavKeysInternal(uri, regexPatternType, true)
            ) doReturn null

            initViewModel(argsWithUriAndRegex)

            underTest.uiState.map { it.navKeys }.test {
                assertThat(awaitItem()).isEmpty()
            }

            verifyNoInteractions(getDecodedUrlRegexPatternTypeUseCase)
            verifyNoInteractions(getAccountCredentials)
            verify(snackbarEventQueue).queueMessage(R.string.open_link_not_valid_link)
        }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}

private fun StringBuilder.randomString() {
    TODO("Not yet implemented")
}
