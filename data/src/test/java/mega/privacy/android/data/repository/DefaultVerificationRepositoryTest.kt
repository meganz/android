package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.repository.VerificationRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringListMap
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultVerificationRepositoryTest {

    private lateinit var underTest: VerificationRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    @Before
    fun setUp() {
        underTest = DefaultVerificationRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            mock(),
        )
    }

    @Test
    fun `test that get Country calling returns a successful result`() = runTest {
        val countryCode = "AD"
        val callingCode = "376"
        val keyList = mock<MegaStringList> {
            on { size() }.thenReturn(1)
        }
        whenever(keyList[0]).thenReturn(countryCode)

        val listMap = mock<MegaStringListMap>() {
            on { size() }.thenReturn(1)
            on { keys }.thenReturn(keyList)
        }

        val mockDialCodes = mock<MegaStringList> {
            on { size() }.thenReturn(1)
        }
        whenever(mockDialCodes[0]).thenReturn(callingCode)

        whenever(listMap[countryCode]).thenReturn(mockDialCodes)

        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        val megaRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_COUNTRY_CALLING_CODES)
            on { megaStringListMap }.thenReturn(listMap)
        }

        whenever(megaApiGateway.getCountryCallingCodes(listener = any())).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError,
            )
        }
        val actual = underTest.getCountryCallingCodes()
        Truth.assertThat(actual).isEqualTo(listOf("$countryCode:$callingCode,"))
    }
}
