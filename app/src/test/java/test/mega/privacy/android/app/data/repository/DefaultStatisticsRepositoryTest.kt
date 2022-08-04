package test.mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.repository.DefaultStatisticsRepository
import mega.privacy.android.domain.repository.StatisticsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultStatisticsRepositoryTest {
    private lateinit var underTest: StatisticsRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    @Before
    fun setUp() {
        underTest = DefaultStatisticsRepository(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway
        )
    }

    @Test
    fun `test that send event would invoke api sendevent`() = runTest {
        val eventID = 1234
        val message = "ABCD"

        underTest.sendEvent(eventID, message)

        verify(megaApiGateway).sendEvent(eventID, message)
    }
}