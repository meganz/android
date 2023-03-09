package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.FolderLoginStatusMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.mapper.toFolderLoginStatus
import nz.mega.sdk.MegaError
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class FolderLinkRepositoryImplTest {

    private lateinit var underTest: FolderLinkRepositoryImpl
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val folderLoginStatusMapper = mock<FolderLoginStatusMapper>()
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val nodeMapper = mock<NodeMapper>()
    private val cacheFolderGateway = mock<CacheFolderGateway>()
    private val fileTypeInfoMapper = mock<FileTypeInfoMapper>()

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest =
            FolderLinkRepositoryImpl(
                megaApiFolderGateway = megaApiFolderGateway,
                folderLoginStatusMapper = folderLoginStatusMapper,
                megaLocalStorageGateway = megaLocalStorageGateway,
                nodeMapper = nodeMapper,
                cacheFolderGateway = cacheFolderGateway,
                fileTypeInfoMapper = fileTypeInfoMapper,
                ioDispatcher = UnconfinedTestDispatcher()
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that status is returned correctly`() = runTest {
        val folderLink = "test"
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val expectedResult = toFolderLoginStatus(megaError)

        whenever(folderLoginStatusMapper(megaError)).thenReturn(expectedResult)

        whenever(megaApiFolderGateway.loginToFolder(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaError
            )
        }

        assertThat(underTest.loginToFolder(folderLink)).isEqualTo(expectedResult)
    }
}