package mega.privacy.android.data.repository

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
import mega.privacy.android.data.mapper.ScannedContactLinkResultMapper
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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

}