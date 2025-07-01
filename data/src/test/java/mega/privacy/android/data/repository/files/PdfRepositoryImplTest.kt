package mega.privacy.android.data.repository.files

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PdfRepositoryImplTest {

    private lateinit var underTest: PdfRepositoryImpl

    private val context = mock<Context>()
    private val megaApi = mock<MegaApiGateway>()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val cacheGateway = mock<CacheGateway>()
    private val fileGateway = mock<FileGateway>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()

    private val nodeHandle = 12345L
    private val lastPageViewed = 10L
    private val lastPageViewedInPdf = LastPageViewedInPdf(
        nodeHandle = nodeHandle,
        lastPageViewed = lastPageViewed
    )

    @BeforeAll
    fun setUp() {
        underTest = PdfRepositoryImpl(
            context = context,
            megaApi = megaApi,
            ioDispatcher = ioDispatcher,
            cacheGateway = cacheGateway,
            fileGateway = fileGateway,
            megaLocalRoomGateway = megaLocalRoomGateway,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            context,
            megaApi,
            cacheGateway,
            fileGateway,
        )
    }

    @Test
    fun `test that getLastPageViewedInPdf returns null when no data is found`() = runTest {
        whenever(megaLocalRoomGateway.getLastPageViewedInPdfByHandle(nodeHandle)) doReturn null

        assertThat(underTest.getLastPageViewedInPdf(nodeHandle)).isNull()
    }

    @Test
    fun `test that getLastPageViewedInPdf returns correct data when found`() = runTest {
        whenever(megaLocalRoomGateway.getLastPageViewedInPdfByHandle(nodeHandle)) doReturn lastPageViewedInPdf

        assertThat(underTest.getLastPageViewedInPdf(nodeHandle)).isEqualTo(lastPageViewed)
    }

    @Test
    fun `test that setOrUpdateLastPageViewedInPdf invokes correctly`() = runTest {
        underTest.setOrUpdateLastPageViewedInPdf(lastPageViewedInPdf)

        verify(megaLocalRoomGateway).insertOrUpdateLastPageViewedInPdf(lastPageViewedInPdf)
    }

    @Test
    fun `test that deleteLastPageViewedInPdf invokes correctly`() = runTest {
        underTest.deleteLastPageViewedInPdf(nodeHandle)

        verify(megaLocalRoomGateway).deleteLastPageViewedInPdfByHandle(nodeHandle)
    }

    @Test
    fun `test that deleteAllLastPageViewedInPdf invokes correctly`() = runTest {
        underTest.deleteAllLastPageViewedInPdf()

        verify(megaLocalRoomGateway).deleteAllLastPageViewedInPdf()
    }
}