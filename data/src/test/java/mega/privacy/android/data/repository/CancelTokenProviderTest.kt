package mega.privacy.android.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import nz.mega.sdk.MegaCancelToken
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CancelTokenProviderTest {
    private val megaApiGateway: MegaApiGateway = mock()
    private val megaCancelToken = mock<MegaCancelToken>()

    private lateinit var underTest: CancelTokenProvider

    @BeforeAll
    fun setup() {
        underTest = CancelTokenProvider(megaApiGateway)
    }

    @BeforeEach
    fun reset() = runTest {
        reset(megaApiGateway, megaCancelToken)
        underTest.cancelCurrentToken()
    }

    @Test
    fun `test that api gateway is fetched to get the cancel token`() = runTest {
        underTest.getOrCreateCancelToken()
        verify(megaApiGateway).createCancelToken()

    }

    @Test
    fun `test that api gateway is not fetched again if the token is not cancelled between calls`() =
        runTest {
            stubTokenCreation()
            underTest.getOrCreateCancelToken()
            underTest.getOrCreateCancelToken()
            verify(megaApiGateway).createCancelToken()
        }

    @Test
    fun `test that api gateway is fetched again if the token is cancelled between calls`() =
        runTest {
            stubTokenCreation()
            underTest.getOrCreateCancelToken()
            underTest.cancelCurrentToken()
            underTest.getOrCreateCancelToken()
            verify(megaApiGateway, times(2)).createCancelToken()
        }

    @Test
    fun `test that api gateway is fetched again if the token is invalidated between calls`() =
        runTest {
            stubTokenCreation()
            underTest.getOrCreateCancelToken()
            underTest.invalidateCurrentToken()
            underTest.getOrCreateCancelToken()
            verify(megaApiGateway, times(2)).createCancelToken()
        }

    @Test
    fun `test that api gateway is fetched again if cancel and create new token is called`() =
        runTest {
            stubTokenCreation()
            underTest.getOrCreateCancelToken()
            underTest.cancelAndCreateNewToken()
            verify(megaApiGateway, times(2)).createCancelToken()
        }

    @Test
    fun `test that current token is cancelled when cancel current token is called`() = runTest {
        stubTokenCreation()
        underTest.getOrCreateCancelToken()
        underTest.cancelCurrentToken()
        verify(megaCancelToken).cancel()
    }

    @Test
    fun `test that current token is not cancelled twice when cancel current token is called twice`() =
        runTest {
            stubTokenCreation()
            underTest.getOrCreateCancelToken()
            underTest.cancelCurrentToken()
            underTest.cancelCurrentToken()
            verify(megaCancelToken).cancel()
        }

    private fun stubTokenCreation() {
        whenever(megaApiGateway.createCancelToken()).thenReturn(megaCancelToken)
    }


}