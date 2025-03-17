package mega.privacy.android.domain.usecase.banner

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.repository.BannerRepository
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.hours

@ExperimentalCoroutinesApi
internal class GetBannersUseCaseTest {

    private lateinit var underTest: GetBannersUseCase
    private val bannerRepository: BannerRepository = mock()
    private val getDeviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase = mock()

    private val testBannerList = listOf(
        Banner(
            1,
            "Test Banner",
            "Test Description",
            "http://test.com",
            "http://test.com",
            "http://test.com",
            "http://test.com"
        ),
        Banner(
            2,
            "Test Banner 2",
            "Test Description 2",
            "http://test2.com",
            "http://test2.com",
            "http://test2.com",
            "http://test2.com"
        )
    )

    @BeforeEach
    fun setUp() {
        underTest = GetBannersUseCase(
            bannerRepository = bannerRepository,
            getDeviceCurrentTimeUseCase = getDeviceCurrentTimeUseCase
        )
    }

    @Test
    fun `test that invoke returns banners when force refresh is false`() = runTest {
        val currentTime = 10000L
        val lastFetchedTime = 5000L
        whenever(getDeviceCurrentTimeUseCase()).thenReturn(currentTime)
        whenever(bannerRepository.getLastFetchedBannerTime()).thenReturn(lastFetchedTime)
        whenever(bannerRepository.getBanners(false)).thenReturn(testBannerList)

        val actual = underTest()

        assertThat(actual).isEqualTo(testBannerList)
    }

    @Test
    fun `test that invoke returns banners when force refresh is true`() = runTest {
        val currentTime = 10001L + 5.hours.inWholeMilliseconds
        val lastFetchedTime = 10000L
        whenever(getDeviceCurrentTimeUseCase()).thenReturn(currentTime)
        whenever(bannerRepository.getLastFetchedBannerTime()).thenReturn(lastFetchedTime)
        whenever(bannerRepository.getBanners(true)).thenReturn(testBannerList)

        val actual = underTest()

        assertThat(actual).isEqualTo(testBannerList)
    }

    @Test
    fun `test that invoke returns empty list when no banners are available`() = runTest {
        val currentTime = 10000L
        val lastFetchedTime = 5000L
        whenever(getDeviceCurrentTimeUseCase()).thenReturn(currentTime)
        whenever(bannerRepository.getLastFetchedBannerTime()).thenReturn(lastFetchedTime)
        whenever(bannerRepository.getBanners(false)).thenReturn(emptyList())

        val actual = underTest()

        assertThat(actual).isEmpty()
    }
}