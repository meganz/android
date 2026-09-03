package mega.privacy.android.app.appstate.global.initialisation.appcreate

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.listeners.global.GlobalListener
import mega.privacy.android.domain.usecase.global.InitialiseGlobalListenersUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SdkSetupInitialiserTest {

    private val megaApi = mock<MegaApiAndroid>()
    private val globalListener = mock<GlobalListener>()
    private val initialiseGlobalListenersUseCase = mock<InitialiseGlobalListenersUseCase>()
    private val activityManager = mock<ActivityManager>()
    private val context = mock<Context> {
        on { getSystemService(Context.ACTIVITY_SERVICE) }.thenReturn(activityManager)
    }

    private fun underTest(appScope: CoroutineScope) = SdkSetupInitialiser(
        megaApiLazy = { megaApi },
        globalListener = globalListener,
        initialiseGlobalListenersUseCase = initialiseGlobalListenersUseCase,
        appScope = appScope,
        context = context,
    )

    @Test
    fun `test that invoke configures ssl retries and transfer methods synchronously`() = runTest {
        underTest(this).invoke()

        verify(megaApi).retrySSLerrors(true)
        verify(megaApi).downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        verify(megaApi).uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
    }

    @Test
    fun `test that invoke adds the global listener synchronously`() = runTest {
        underTest(this).invoke()

        verify(megaApi).addGlobalListener(globalListener)
    }

    @Test
    fun `test that invoke initialises the global request listeners in the application scope`() =
        runTest {
            underTest(this).invoke()
            advanceUntilIdle()

            verify(initialiseGlobalListenersUseCase).invoke()
        }

    @Test
    fun `test that invoke applies buffer size, language and resource limit in the application scope`() =
        runTest {
            whenever(megaApi.platformGetRLimitNumFile()).thenReturn(0)

            underTest(this).invoke()
            advanceUntilIdle()

            verify(megaApi).httpServerSetMaxBufferSize(any())
            verify(megaApi, atLeastOnce()).setLanguage(anyOrNull())
            verify(megaApi).platformSetRLimitNumFile(eq(20000))
        }
}
