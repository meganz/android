package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.StatisticsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSendStatisticsMediaDiscoveryTest {
    private lateinit var underTest: SendStatisticsMediaDiscovery

    private val defaultMediaHandle = 1234L

    private val statisticsRepository = mock<StatisticsRepository> {
        onBlocking {
            getMediaDiscoveryClickCount()
        }.thenReturn(1)
        onBlocking {
            getMediaDiscoveryClickCountFolder(defaultMediaHandle)
        }.thenReturn(1)
    }

    private val mediaDiscoveryClickedEventId = 99200
    private val multiClickEventId = 99201
    private val sameFolderMultiClickEventId = 99202

    @Before
    fun setUp() {
        underTest = DefaultSendStatisticsMediaDiscovery(
            statisticsRepository = statisticsRepository,
        )
    }

    @Test
    fun `test that the basic click statistic event is fired`() = runTest {
        underTest(defaultMediaHandle)

        verify(statisticsRepository).sendEvent(mediaDiscoveryClickedEventId, "Media Discovery Click")
    }

    @Test
    fun `test that a one increment of the click count is set`() = runTest {
        underTest(defaultMediaHandle)

        verify(statisticsRepository).setMediaDiscoveryClickCount(2)
    }

    @Test
    fun `test that a one increment of the click count folder is set`() = runTest {
        underTest(defaultMediaHandle)

        verify(statisticsRepository).setMediaDiscoveryClickCountFolder(2, defaultMediaHandle)
    }

    @Test
    fun `test that an event is fired when click count is greater or equal to 3 after increment`() =
        runTest {
            whenever(statisticsRepository.getMediaDiscoveryClickCount()).thenReturn(2)
            underTest(defaultMediaHandle)

            verify(statisticsRepository).sendEvent(
                multiClickEventId,
                "Media Discovery Click >= 3"
            )
        }

    @Test
    fun `test that an event is fired when click count folder is greater or equal to 3 after increment`() =
        runTest {
            whenever(statisticsRepository.getMediaDiscoveryClickCountFolder(defaultMediaHandle))
                .thenReturn(2)

            underTest(defaultMediaHandle)

            verify(statisticsRepository).sendEvent(
                sameFolderMultiClickEventId,
                "Media Discovery Click Specific Folder >= 3"
            )
        }
}
