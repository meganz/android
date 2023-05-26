package mega.privacy.android.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.data.mapper.analytics.AnalyticsEventMessageMapper
import mega.privacy.android.domain.entity.analytics.ScreenViewEvent
import mega.privacy.android.domain.repository.StatisticsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
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

    private val analyticsEventMessageMapper = mock<AnalyticsEventMessageMapper>()

    @BeforeEach
    internal fun setUp() {
        underTest = DefaultStatisticsRepository(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
            statisticsPreferencesGateway = statisticsPreferencesGateway,
            analyticsEventMessageMapper = analyticsEventMessageMapper,
        )
    }

    @Test
    internal fun `test that send event would invoke api sendevent`() = runTest {
        val eventID = 1234
        val message = "ABCD"

        underTest.sendEvent(eventID, message)

        verify(megaApiGateway).sendEvent(eventID, message)
    }

    @Test
    internal fun `test that get click count would invoke getclickcount`() = runTest {
        underTest.getMediaDiscoveryClickCount()

        verify(statisticsPreferencesGateway).getClickCount()
    }

    @Test
    internal fun `test that get click count folder would invoke getclickcountfolder`() = runTest {
        underTest.getMediaDiscoveryClickCountFolder(defaultMediaHandle)

        verify(statisticsPreferencesGateway).getClickCountFolder(defaultMediaHandle)
    }

    @Test
    internal fun `test that the correct click count is returned`() = runTest {
        assertEquals(1, underTest.getMediaDiscoveryClickCount())
    }

    @Test
    internal fun `test that the correct click count folder is returned`() = runTest {
        assertEquals(1, underTest.getMediaDiscoveryClickCountFolder(defaultMediaHandle))
    }

    @Test
    internal fun `test that set click count actually updates the stored click count`() = runTest {
        val newCount = 2

        underTest.setMediaDiscoveryClickCount(newCount)

        verify(statisticsPreferencesGateway).setClickCount(newCount)
    }

    @Test
    internal fun `test that set click count folder actually updates the stored click count folder`() =
        runTest {
            val newCount = 2

            underTest.setMediaDiscoveryClickCountFolder(newCount, defaultMediaHandle)

            verify(statisticsPreferencesGateway).setClickCountFolder(newCount, defaultMediaHandle)
        }

    @Test
    internal fun `test that logEvent calls send event with the event properties`() = runTest {
        val expectedMessage = "ExpectedMessage"
        val expectedEventId = 5
        val expectedViewId = "ExpectedViewId"

        //We need to mock a specific event type because Mockito cannot mock sealed interfaces
        val event = mock<ScreenViewEvent> {
            on { getEventIdentifier() }.thenReturn(expectedEventId)
            on { viewId }.thenReturn(expectedViewId)
        }

        analyticsEventMessageMapper.stub {
            on { invoke(any()) }.thenReturn(expectedMessage)
        }

        underTest.logEvent(event)

        verify(megaApiGateway).sendEvent(
            expectedEventId, expectedMessage, true, expectedViewId
        )
    }
}