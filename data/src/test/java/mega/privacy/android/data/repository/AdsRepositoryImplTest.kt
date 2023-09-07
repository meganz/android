package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AdsGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MegaStringListMapper
import mega.privacy.android.data.mapper.advertisements.AdDetailMapper
import mega.privacy.android.domain.entity.advertisements.AdDetail
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.AdsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AdsRepositoryImplTest {
    private lateinit var underTest: AdsRepository

    private val adsGateway: AdsGateway = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val adDetailMapper: AdDetailMapper = mock()
    private val megaStringListMapper: MegaStringListMapper = mock()

    private val testAdDetailList = listOf(AdDetail("ANDFB", "https://megaad.nz/#z_xyz"))
    private val testAdSlotList = listOf("ANDFB")

    @BeforeAll
    fun setUp() {
        underTest = AdsRepositoryImpl(
            ioDispatcher = ioDispatcher,
            adsGateway = adsGateway,
            megaApiGateway = megaApiGateway,
            adDetailMapper = adDetailMapper,
            megaStringListMapper = megaStringListMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            adsGateway,
            adDetailMapper,
            megaStringListMapper,
        )
    }

    @Test
    fun `test that fetch ads detail returns successfully if no error is thrown`() =
        runTest(ioDispatcher) {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { megaStringMap }.thenReturn(mock())
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(adDetailMapper(any())).thenReturn(testAdDetailList)
            whenever(megaStringListMapper(testAdSlotList)).thenReturn(mock())
            whenever(adsGateway.fetchAds(any(), any(), any(), any())).thenAnswer {
                (it.arguments[3] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.fetchAdDetails(testAdSlotList, 0L)
            assertThat(actual).isEqualTo(testAdDetailList)
        }


    @Test
    fun `test that fetch ads detail throws an exception when the api returns an error`() =
        runTest(ioDispatcher) {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { megaStringMap }.thenReturn(mock())
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(adDetailMapper(any())).thenReturn(testAdDetailList)
            whenever(megaStringListMapper(testAdSlotList)).thenReturn(mock())
            whenever(adsGateway.fetchAds(any(), any(), any(), any())).thenAnswer {
                (it.arguments[3] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            assertFailsWith(
                exceptionClass = MegaException::class,
                block = { underTest.fetchAdDetails(testAdSlotList, 0L) }
            )
        }
}