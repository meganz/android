package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.consent.AdConsentWrapper
import mega.privacy.android.app.consent.CookieDialog
import mega.privacy.android.app.consent.initialiser.ConsentInitialiser
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import mega.privacy.android.domain.usecase.setting.ShouldShowGenericCookieDialogUseCase
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class ConsentInitialiserTest {
    private lateinit var underTest: ConsentInitialiser

    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()
    private val appDialogsEventQueue = mock<AppDialogsEventQueue>()
    private val shouldShowGenericCookieDialogUseCase = mock<ShouldShowGenericCookieDialogUseCase>()
    private val monitorMiscLoadedUseCase = mock<MonitorMiscLoadedUseCase>()
    private val adConsentWrapper = mock<AdConsentWrapper>()

    @BeforeEach
    fun setUp() {
        reset(
            getCookieSettingsUseCase,
            appDialogsEventQueue,
            shouldShowGenericCookieDialogUseCase,
            monitorMiscLoadedUseCase,
            adConsentWrapper,
        )

        underTest = ConsentInitialiser(
            getCookieSettingsUseCase = getCookieSettingsUseCase,
            appDialogEventQueue = appDialogsEventQueue,
            shouldShowGenericCookieDialogUseCase = shouldShowGenericCookieDialogUseCase,
            monitorMiscLoadedUseCase = monitorMiscLoadedUseCase,
            adConsentWrapper = adConsentWrapper
        )
    }

    @Test
    fun `test that cookie dialog event is emitted if required`() = runTest {
        monitorMiscLoadedUseCase.stub {
            on { invoke() } doReturn flow {
                emit(true)
                awaitCancellation()
            }
        }
        getCookieSettingsUseCase.stub { onBlocking { invoke() } doReturn emptySet() }
        shouldShowGenericCookieDialogUseCase.stub { onBlocking { invoke(any()) } doReturn true }

        underTest.invoke("test-session", true)

        verify(appDialogsEventQueue).emit(AppDialogEvent(CookieDialog))
    }

    @Test
    fun `test that ad consent is refreshed if no cookie dialog`() =
        runTest {
            monitorMiscLoadedUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(true)
                    awaitCancellation()
                }
            }


            getCookieSettingsUseCase.stub { onBlocking { invoke() } doReturn emptySet() }
            shouldShowGenericCookieDialogUseCase.stub { onBlocking { invoke(any()) } doReturn false }

            underTest.invoke("test-session", true)

            verify(adConsentWrapper).refreshConsent()
        }


    @Test
    fun `test that no values are emitted if misc flags are not set`() = runTest {
        getCookieSettingsUseCase.stub { onBlocking { invoke() } doReturn emptySet() }
        shouldShowGenericCookieDialogUseCase.stub { onBlocking { invoke(any()) } doReturn true }

        monitorMiscLoadedUseCase.stub {
            on { invoke() } doReturn flow {
                emit(false)
                awaitCancellation()
            }
        }

        underTest.invoke("test-session", true)

        verifyNoInteractions(
            appDialogsEventQueue,
            getCookieSettingsUseCase,
            shouldShowGenericCookieDialogUseCase,
            adConsentWrapper,
        )
    }
}