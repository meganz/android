package mega.privacy.android.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.SlideshowPreferencesGateway
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.repository.SlideshowRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class SlideshowRepositoryTest {
    private lateinit var underTest: SlideshowRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val slideshowPreferencesGateway = mock<SlideshowPreferencesGateway>()

    @Before
    fun setUp() {
        whenever(megaApiGateway.myUserHandle).thenReturn(1234L)
        underTest = SlideshowRepositoryImpl(
            slideshowPreferencesGateway = slideshowPreferencesGateway,
            megaApiGateway = megaApiGateway,
        )
    }

    @Test
    fun `test that monitor order setting receives correct item`() = runTest {
        // given
        val expectedSetting = SlideshowOrder.Shuffle
        whenever(slideshowPreferencesGateway.monitorOrderSetting(any()))
            .thenReturn(flowOf(expectedSetting))

        // then
        underTest.monitorOrderSetting().test {
            val actualSetting = awaitItem()
            assertEquals(actualSetting, expectedSetting)
            awaitComplete()
        }
    }

    @Test
    fun `test that monitor speed setting receives correct item`() = runTest {
        // given
        val expectedSetting = SlideshowSpeed.Normal
        whenever(slideshowPreferencesGateway.monitorSpeedSetting(any()))
            .thenReturn(flowOf(expectedSetting))

        // then
        underTest.monitorSpeedSetting().test {
            val actualSetting = awaitItem()
            assertEquals(actualSetting, expectedSetting)
            awaitComplete()
        }
    }

    @Test
    fun `test that monitor repeat setting receives correct item`() = runTest {
        // given
        val expectedSetting = true
        whenever(slideshowPreferencesGateway.monitorRepeatSetting(any()))
            .thenReturn(flowOf(expectedSetting))

        // then
        underTest.monitorRepeatSetting().test {
            val actualSetting = awaitItem()
            assertEquals(actualSetting, expectedSetting)
            awaitComplete()
        }
    }

    @Test
    fun `test that save order setting works properly`() = runTest {
        // given
        val setting = SlideshowOrder.Newest

        // then
        try {
            underTest.saveOrderSetting(setting)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }

    @Test
    fun `test that save speed setting works properly`() = runTest {
        // given
        val setting = SlideshowSpeed.Fast

        // then
        try {
            underTest.saveSpeedSetting(setting)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }

    @Test
    fun `test that save repeat setting works properly`() = runTest {
        // given
        val setting = true

        // then
        try {
            underTest.saveRepeatSetting(setting)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }
}
