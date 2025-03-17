package mega.privacy.android.data.repository.banner

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.banner.MegaBannerMapper
import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.BannerRepository
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
internal class BannerRepositoryImplTest {
    private lateinit var underTest: BannerRepository

    private val megaApiGateway: MegaApiGateway = mock()
    private val megaBannerMapper: MegaBannerMapper = mock()
    private val ioDispatcher = UnconfinedTestDispatcher()
    private val bannerCache: Cache<List<Banner>> = mock()

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

    @BeforeAll
    fun setUp() {
        underTest = BannerRepositoryImpl(
            megaApiGateway = megaApiGateway,
            megaBannerMapper = megaBannerMapper,
            ioDispatcher = ioDispatcher,
            bannerCache = bannerCache
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            megaBannerMapper,
            bannerCache
        )
    }

    @Test
    fun `getBanners returns cached banners if forceRefresh is false`() = runTest(ioDispatcher) {
        whenever(bannerCache.get()).thenReturn(testBannerList)
        val actual = underTest.getBanners(forceRefresh = false)
        assertThat(actual).isEqualTo(testBannerList)
    }

    @Test
    fun `getBanners fetches new banners if forceRefresh is true`() = runTest(ioDispatcher) {
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest> {
            on { megaBannerList }.thenReturn(mock())
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }
        whenever(megaBannerMapper(any())).thenReturn(testBannerList)
        whenever(megaApiGateway.getBanners(any())).thenAnswer {
            (it.arguments[0] as OptionalMegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }
        val actual = underTest.getBanners(forceRefresh = true)
        assertThat(actual).isEqualTo(testBannerList)
    }

    @Test
    fun `getBanners throws an exception when the api returns an error`() = runTest(ioDispatcher) {
        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest> {
            on { megaBannerList }.thenReturn(mock())
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK + 1)
        }
        whenever(megaBannerMapper(any())).thenReturn(testBannerList)
        whenever(megaApiGateway.getBanners(any())).thenAnswer {
            (it.arguments[0] as OptionalMegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }
        assertFailsWith(
            exceptionClass = MegaException::class,
            block = { underTest.getBanners(forceRefresh = true) }
        )
    }

    @Test
    fun `dismissBanner removes banner from cache`() = runTest(ioDispatcher) {
        whenever(bannerCache.get()).thenReturn(testBannerList)
        underTest.dismissBanner(1)
        verify(megaApiGateway).dismissBanner(1)
        verify(bannerCache).set(testBannerList.filter { it.id != 1 })
    }

    @Test
    fun `clearCache clears the banner cache`() {
        underTest.clearCache()
        verify(bannerCache).clear()
    }
}