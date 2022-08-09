package test.mega.privacy.android.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.app.data.repository.DefaultStatisticsRepository
import mega.privacy.android.domain.repository.StatisticsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultStatisticsRepositoryTest {
    private lateinit var underTest: StatisticsRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    private val defaultMediaHandle = 1234L

    private val statisticsPreferencesGateway = mock<StatisticsPreferencesGateway> {
        on {
            getClickCount()
        }.thenReturn(flowOf(1))
        on {
            getClickCountFolder(defaultMediaHandle)
        }.thenReturn(flowOf(1))
    }

    @Before
    fun setUp() {
        underTest = DefaultStatisticsRepository(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
            statisticsPreferencesGateway = statisticsPreferencesGateway,
        )
    }

    @Test
    fun `test that send event would invoke api sendevent`() = runTest {
        val eventID = 1234
        val message = "ABCD"

        underTest.sendEvent(eventID, message)

        verify(megaApiGateway).sendEvent(eventID, message)
    }

    @Test
    fun `test that get click count would invoke getclickcount`() = runTest {
        underTest.getMediaDiscoveryClickCount()

        verify(statisticsPreferencesGateway).getClickCount()
    }

    @Test
    fun `test that get click count folder would invoke getclickcountfolder`() = runTest {
        underTest.getMediaDiscoveryClickCountFolder(defaultMediaHandle)

        verify(statisticsPreferencesGateway).getClickCountFolder(defaultMediaHandle)
    }

    @Test
    fun `test that the correct click count is returned`() = runTest {
        assertEquals(1, underTest.getMediaDiscoveryClickCount())
    }

    @Test
    fun `test that the correct click count folder is returned`() = runTest {
        assertEquals(1, underTest.getMediaDiscoveryClickCountFolder(defaultMediaHandle))
    }

    @Test
    fun `test that set click count actually updates the stored click count`() = runTest {
        val newCount = 2

        underTest.setMediaDiscoveryClickCount(newCount)

        verify(statisticsPreferencesGateway).setClickCount(newCount)
    }

    @Test
    fun `test that set click count folder actually updates the stored click count folder`() =
        runTest {
            val newCount = 2

            underTest.setMediaDiscoveryClickCountFolder(newCount, defaultMediaHandle)

            verify(statisticsPreferencesGateway).setClickCountFolder(newCount, defaultMediaHandle)
        }
}