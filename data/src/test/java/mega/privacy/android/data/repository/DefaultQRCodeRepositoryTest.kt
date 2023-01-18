package mega.privacy.android.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultQRCodeRepositoryTest {

    private lateinit var underTest: DefaultQRCodeRepository
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val cacheFolderGateway = mock<CacheFolderGateway>()

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest =
            DefaultQRCodeRepository(
                cacheFolderGateway = cacheFolderGateway,
                defaultDispatcher = testCoroutineDispatcher,
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