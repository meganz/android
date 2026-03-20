package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.home.ShouldDisplayNewFeatureUseCase
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import mega.privacy.android.navigation.destination.WhatsNewNavKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class WhatsNewInitializerTest {
    private lateinit var underTest: WhatsNewInitializer

    private val appDialogsEventQueue = mock<AppDialogsEventQueue>()
    private val shouldDisplayNewFeatureUseCase = mock<ShouldDisplayNewFeatureUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            appDialogsEventQueue,
            shouldDisplayNewFeatureUseCase,
            getFeatureFlagValueUseCase,
        )

        underTest = WhatsNewInitializer(
            appDialogsEventQueue = appDialogsEventQueue,
            shouldDisplayNewFeatureUseCase = shouldDisplayNewFeatureUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase
        )
    }

    @Test
    fun `test that whats new event is emitted when feature flag is enabled and should display returns true`() =
        runTest {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(ApiFeatures.WhatsNewFeatureDialog) } doReturn true
            }
            shouldDisplayNewFeatureUseCase.stub {
                onBlocking { invoke() } doReturn true
            }

            underTest.invoke("test-session", false)

            verify(appDialogsEventQueue).emit(
                argThat<AppDialogEvent> { event -> event.dialogDestination == WhatsNewNavKey },
                any()
            )
        }

    @Test
    fun `test that whats new event is not emitted when should display returns false`() = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.WhatsNewFeatureDialog) } doReturn true
        }
        shouldDisplayNewFeatureUseCase.stub {
            onBlocking { invoke() } doReturn false
        }

        underTest.invoke("test-session", false)

        verifyNoInteractions(appDialogsEventQueue)
    }

    @Test
    fun `test that whats new event is not emitted when feature flag is disabled`() = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.WhatsNewFeatureDialog) } doReturn false
        }
        shouldDisplayNewFeatureUseCase.stub {
            onBlocking { invoke() } doReturn true
        }

        underTest.invoke("test-session", false)

        verifyNoInteractions(appDialogsEventQueue)
    }

    @Test
    fun `test that whats new event is not emitted when should display throws exception`() =
        runTest {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(ApiFeatures.WhatsNewFeatureDialog) } doReturn true
            }
            shouldDisplayNewFeatureUseCase.stub {
                onBlocking { invoke() }.thenThrow(RuntimeException("Test error"))
            }

            underTest.invoke("test-session", false)

            verifyNoInteractions(appDialogsEventQueue)
        }
}
