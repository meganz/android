package mega.privacy.android.app.consent

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.consent.model.CookieConsentState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.cookies.GetCookieUrlUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify


@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CookieConsentViewModelTest {
    private lateinit var underTest: CookieConsentViewModel

    private val getCookieUrlUseCase = mock<GetCookieUrlUseCase>()
    private val updateCookieSettingsUseCase = mock<UpdateCookieSettingsUseCase>()
    private val adConsentWrapper = mock<AdConsentWrapper>()

    @BeforeEach
    fun setUp() {
        underTest = CookieConsentViewModel(
            getCookieUrlUseCase = getCookieUrlUseCase,
            updateCookieSettingsUseCase = updateCookieSettingsUseCase,
            adConsentWrapper = adConsentWrapper
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getCookieUrlUseCase,
            updateCookieSettingsUseCase,
            adConsentWrapper,
        )
    }

    @Test
    fun `test that data state contains url`() = runTest {
        val expected = "https://mega.nz/cookie"
        getCookieUrlUseCase.stub {
            onBlocking { invoke() }.thenReturn(expected)
        }

        underTest.state.test {
            val actual = awaitItem() as CookieConsentState.Data
            assertThat(actual.cookiesUrl).isEqualTo(expected)
        }
    }

    @Test
    fun `test that calling accept all calls the accept cookies use case with all cookies`() =
        runTest {
            underTest.acceptAllCookies()

            verify(updateCookieSettingsUseCase).invoke(CookieType.entries.toSet())
            verify(adConsentWrapper).refreshConsent()
        }

    @Test
    fun `test that calling accept essential calls the accept cookies use case with essential cookie type`() =
        runTest {
            underTest.acceptEssentialCookies()

            verify(updateCookieSettingsUseCase).invoke(setOf(CookieType.ESSENTIAL))
            verify(adConsentWrapper).refreshConsent()
        }

}