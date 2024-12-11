package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AdsGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MegaStringListMapper
import mega.privacy.android.data.mapper.advertisements.AdDetailsMapper
import mega.privacy.android.domain.entity.advertisements.AdDetails
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AdsRepositoryImplTest {
    private lateinit var underTest: AdsRepository

    private val adsGateway: AdsGateway = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val adDetailsMapper: AdDetailsMapper = mock()
    private val megaStringListMapper: MegaStringListMapper = mock()
    private val uiPreferencesGateway: UIPreferencesGateway = mock()

    private val testAdDetailsList = listOf(AdDetails("ANDFB", "https://megaad.nz/#z_xyz"))
    private val testAdSlotList = listOf("ANDFB")

    @BeforeAll
    fun setUp() {
        underTest = AdsRepositoryImpl(
            ioDispatcher = ioDispatcher,
            adsGateway = adsGateway,
            adDetailsMapper = adDetailsMapper,
            megaStringListMapper = megaStringListMapper,
            uiPreferencesGateway = uiPreferencesGateway
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            adsGateway,
            adDetailsMapper,
            megaStringListMapper,
            uiPreferencesGateway
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
            whenever(adDetailsMapper(any())).thenReturn(testAdDetailsList)
            whenever(megaStringListMapper(testAdSlotList)).thenReturn(mock())
            whenever(adsGateway.fetchAds(any(), any(), any(), any())).thenAnswer {
                (it.arguments[3] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.fetchAdDetails(testAdSlotList, 0L)
            assertThat(actual).isEqualTo(testAdDetailsList)
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

            whenever(adDetailsMapper(any())).thenReturn(testAdDetailsList)
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

    @Test
    fun `test that monitor ads closing timestamp returns successfully`() =
        runTest {
            val testTimestamp = 123456L
            whenever(uiPreferencesGateway.monitorAdsClosingTimestamp()).thenReturn(
                flowOf(testTimestamp)
            )
            underTest.monitorAdsClosingTimestamp().test {
                assertThat(awaitItem()).isEqualTo(testTimestamp)
                awaitComplete()
            }
        }

    @Test
    fun `test that set ads closing timestamp invoke correctly`() =
        runTest {
            val testTimestamp = 123456L
            underTest.setAdsClosingTimestamp(testTimestamp)
            verify(uiPreferencesGateway).setAdsClosingTimestamp(testTimestamp)
        }
}