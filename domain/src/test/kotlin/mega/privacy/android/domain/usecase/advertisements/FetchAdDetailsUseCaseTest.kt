package mega.privacy.android.domain.usecase.advertisements

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.advertisements.AdDetails
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.repository.AdsRepository
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchAdDetailUseCaseTest {
    private lateinit var underTest: FetchAdDetailUseCase
    private val adsRepository = mock<AdsRepository>()
    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()

    private val slotId = "ANDFB"
    private val adBaseUrl = "https://megaad.nz/#z_xyz"
    private val adsCookieEnabledPostfix = "&ac=1"
    private val fetchDetailsRequest = FetchAdDetailRequest(slotId, null)
    private fun initTestClass() {
        underTest = FetchAdDetailUseCase(
            adsRepository = adsRepository,
            getCookieSettingsUseCase = getCookieSettingsUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getCookieSettingsUseCase,
        )
    }

    private fun provideFetchAdDetailUseCaseParameters() = Stream.of(
        Arguments.of(
            AdDetails(slotId, adBaseUrl),
            setOf(CookieType.ADS_CHECK, CookieType.ADVERTISEMENT),
            AdDetails(slotId, "$adBaseUrl$adsCookieEnabledPostfix"),
        ),
        Arguments.of(
            AdDetails(slotId, adBaseUrl),
            emptySet<CookieType>(),
            AdDetails(slotId, adBaseUrl),
        ),
        Arguments.of(
            AdDetails(slotId, adBaseUrl),
            setOf(CookieType.ADS_CHECK),
            AdDetails(slotId, adBaseUrl),
        ),
        Arguments.of(
            AdDetails(slotId, adBaseUrl),
            setOf(CookieType.ADVERTISEMENT),
            AdDetails(slotId, adBaseUrl),
        )
    )

    @ParameterizedTest(name = "when ads details is: {0} cookie settings are: {1} and received adDetails is: {2} then expectedAdDetails is: {3}")
    @MethodSource("provideFetchAdDetailUseCaseParameters")
    fun `test that when ads details is fetched successfully and cookie settings are returned then ads details is updated accordingly`(
        receivedAdDetails: AdDetails,
        cookieSettings: Set<CookieType>,
        expectedAdDetails: AdDetails,
    ) = runTest {
        whenever(getCookieSettingsUseCase()).thenReturn(cookieSettings)
        whenever(
            adsRepository.fetchAdDetails(
                listOf(fetchDetailsRequest.slotId),
                fetchDetailsRequest.linkHandle
            )
        ).thenReturn(listOf(receivedAdDetails))
        initTestClass()

        val result = underTest.invoke(fetchDetailsRequest)
        assertThat(result?.url).isEqualTo(expectedAdDetails.url)
    }
}