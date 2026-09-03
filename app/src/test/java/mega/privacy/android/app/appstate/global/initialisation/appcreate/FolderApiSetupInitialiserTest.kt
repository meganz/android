package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.test.runTest
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FolderApiSetupInitialiserTest {

    private val megaApi = mock<MegaApiAndroid>()
    private val megaApiFolder = mock<MegaApiAndroid>()
    private val underTest = FolderApiSetupInitialiser(
        megaApiLazy = { megaApi },
        megaApiFolderLazy = { megaApiFolder },
    )

    @Test
    fun `test that invoke mirrors the core api configuration on the folder api`() = runTest {
        whenever(megaApi.httpServerGetMaxBufferSize()).thenReturn(1234)

        underTest()

        verify(megaApiFolder).retrySSLerrors(true)
        verify(megaApiFolder).downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        verify(megaApiFolder).uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        verify(megaApiFolder).httpServerSetMaxBufferSize(1234)
    }

    @Test
    fun `test that invoke sets the account auth token when logged in`() = runTest {
        whenever(megaApi.isLoggedIn).thenReturn(1)
        whenever(megaApi.accountAuth).thenReturn("auth-token")

        underTest()

        verify(megaApiFolder).accountAuth = "auth-token"
    }

    @Test
    fun `test that invoke does not set the account auth token when logged out`() = runTest {
        whenever(megaApi.isLoggedIn).thenReturn(0)

        underTest()

        verify(megaApiFolder, never()).accountAuth = anyOrNull()
    }
}
