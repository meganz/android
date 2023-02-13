package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ScannedContactLinkResultMapper
import mega.privacy.android.data.mapper.toScannedContactLinkResult
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultQRCodeRepositoryTest {

    private lateinit var underTest: DefaultQRCodeRepository
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val cacheFolderGateway = mock<CacheFolderGateway>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val scannedContactLinkResultMapper = mock<ScannedContactLinkResultMapper>()

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest =
            DefaultQRCodeRepository(
                cacheFolderGateway = cacheFolderGateway,
                megaApiGateway = megaApiGateway,
                megaLocalStorageGateway = megaLocalStorageGateway,
                scannedContactLinkResultMapper = scannedContactLinkResultMapper,
                defaultDispatcher = testCoroutineDispatcher,
                ioDispatcher = UnconfinedTestDispatcher()
            )
    }

    @Test
    fun `test that getCacheFile of CacheFolderGateway is invoked when getQRFile is invoked`() =
        runTest {
            val fileName = "fileName"
            underTest.getQRFile(fileName)
            verify(cacheFolderGateway).getCacheFile(CacheFolderConstant.QR_FOLDER, fileName)
        }

    @Test
    fun `test that contact details is successfully returned`() = runTest {
        val base64Handle = "12353"
        val handle: Long = 12345
        val contactEmail = "abc@gmail.com"
        val megaRequest = mock<MegaRequest> {
            on { name }.thenReturn("abc")
            on { text }.thenReturn("xyz")
            on { email }.thenReturn(contactEmail)
            on { nodeHandle }.thenReturn(handle)
        }
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

        val expectedResult = toScannedContactLinkResult(megaRequest, megaError, false)

        whenever(scannedContactLinkResultMapper(megaRequest, megaError, false))
            .thenReturn(expectedResult)
        whenever(megaApiGateway.base64ToHandle(base64Handle)).thenReturn(handle)
        whenever(megaApiGateway.getContact(contactEmail)).thenReturn(null)

        whenever(megaApiGateway.getContactLink(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError
            )
        }

        assertThat(underTest.queryScannedContactLink(base64Handle)).isEqualTo(expectedResult)
    }
}
