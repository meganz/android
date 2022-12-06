package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.CountryCallingCodeMapper
import mega.privacy.android.domain.repository.VerificationRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
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
    private val countryCallingCodeMapper = mock<CountryCallingCodeMapper>()

    @Before
    fun setUp() {
        underTest = DefaultVerificationRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            mock(),
            countryCallingCodeMapper = countryCallingCodeMapper,
        )
    }

    @Test
    fun `test that get Country calling returns a successful result`() = runTest {
        val countryCode = "AD"
        val callingCode = "376"
        val expected = listOf("$countryCode:$callingCode,")

        val listMap = mock<MegaStringListMap>()
        whenever(countryCallingCodeMapper.invoke(listMap)).thenReturn(expected)

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
        Truth.assertThat(actual).isEqualTo(expected)
    }
}
