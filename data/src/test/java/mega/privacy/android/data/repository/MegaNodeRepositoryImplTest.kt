package mega.privacy.android.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.MegaShareMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.FileNotCreatedException
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class MegaNodeRepositoryImplTest {
    private lateinit var underTest: FileSystemRepository

    private val context: Context = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val megaApiFolderGateway: MegaApiFolderGateway = mock()
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val megaLocalStorageGateway: MegaLocalStorageGateway = mock()
    private val megaShareMapper: MegaShareMapper = mock()
    private val megaExceptionMapper: MegaExceptionMapper = mock()
    private val sortOrderIntMapper: SortOrderIntMapper = mock()
    private val cacheFolderGateway: CacheFolderGateway = mock()
    private val nodeMapper: NodeMapper = mock()
    private val fileTypeInfoMapper: FileTypeInfoMapper = mock()
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper = mock()
    private val fileGateway: FileGateway = mock()
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper = mock()
    private val fileVersionsOptionCache: Cache<Boolean> = mock()
    private val streamingGateway = mock<StreamingGateway>()
    private val deviceGateway = mock<DeviceGateway>()


    @Before
    fun setUp() {
        underTest = MegaNodeRepositoryImpl(
            context = context,
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = ioDispatcher,
            megaLocalStorageGateway = megaLocalStorageGateway,
            megaShareMapper = megaShareMapper,
            megaExceptionMapper = megaExceptionMapper,
            sortOrderIntMapper = sortOrderIntMapper,
            cacheFolderGateway = cacheFolderGateway,
            nodeMapper = nodeMapper,
            fileTypeInfoMapper = fileTypeInfoMapper,
            offlineNodeInformationMapper = offlineNodeInformationMapper,
            fileGateway = fileGateway,
            chatFilesFolderUserAttributeMapper = chatFilesFolderUserAttributeMapper,
            fileVersionsOptionCache = fileVersionsOptionCache,
            streamingGateway = streamingGateway,
            deviceGateway = deviceGateway,
        )
    }

    @Test
    fun `test that data return from cache when fileVersionsOptionCache is not null and call getFileVersionsOption with forceRefresh false`() =
        runTest {
            val expectedFileVersionsOption = true
            whenever(fileVersionsOptionCache.get()).thenReturn(expectedFileVersionsOption)
            val actual = underTest.getFileVersionsOption(false)
            verify(fileVersionsOptionCache, times(0)).set(any())
            verify(megaApiGateway, times(0)).getFileVersionsOption(any())
            assertEquals(expectedFileVersionsOption, actual)
        }

    @Test
    fun `test that data return from sdk when fileVersionsOptionCache is not null and call getFileVersionsOption with forceRefresh true`() =
        runTest {
            val expectedFileVersionsOption = true
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(expectedFileVersionsOption)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(fileVersionsOptionCache.get()).thenReturn(expectedFileVersionsOption.not())
            whenever(megaApiGateway.getFileVersionsOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.getFileVersionsOption(true)
            verify(fileVersionsOptionCache, times(1)).set(expectedFileVersionsOption)
            verify(megaApiGateway, times(1)).getFileVersionsOption(any())
            assertEquals(expectedFileVersionsOption, actual)
        }

    @Test
    fun `test that data return from sdk when fileVersionsOptionCache is null and call getFileVersionsOption with forceRefresh false`() =
        runTest {
            val expectedFileVersionsOption = true
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(expectedFileVersionsOption)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(fileVersionsOptionCache.get()).thenReturn(null)
            whenever(megaApiGateway.getFileVersionsOption(any())).thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            val actual = underTest.getFileVersionsOption(false)
            verify(fileVersionsOptionCache, times(1)).set(expectedFileVersionsOption)
            verify(megaApiGateway, times(1)).getFileVersionsOption(any())
            assertEquals(expectedFileVersionsOption, actual)
        }

    @Test
    fun `test that local file url string is returned if node exists`() = runTest {
        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
        val expected = "expectedUrl"
        whenever(streamingGateway.getLocalLink(any())).thenReturn(expected)

        val actual = underTest.getFileStreamingUri(mock { on { id }.thenReturn(NodeId(1L)) })

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that temporary file is created successfully when sync record is valid`() = runTest {
        val localPath = "/path/to/local"
        val newPath = "/path/to/new"
        val rootPath = "/path/to/root"
        val syncRecord = SyncRecord(
            id = 0,
            localPath = localPath,
            newPath = newPath,
            originFingerprint = null,
            newFingerprint = null,
            timestamp = null,
            fileName = null,
            longitude = null,
            latitude = null,
            status = 0,
            type = 0,
            nodeHandle = null,
            isCopyOnly = false,
            isSecondary = false,
        )
        whenever(fileGateway.createTempFile(rootPath, localPath, newPath)).thenReturn(Unit)
        val actual = underTest.createTempFile(rootPath, syncRecord)
        assertThat(actual).isEqualTo(newPath)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that file not found exception is thrown when local path is null`() = runTest {
        val localPath = "/path/to/local"
        val newPath = "/path/to/new"
        val rootPath = "/path/to/root"
        val syncRecord = SyncRecord(
            id = 0,
            localPath = null,
            newPath = newPath,
            originFingerprint = null,
            newFingerprint = null,
            timestamp = null,
            fileName = null,
            longitude = null,
            latitude = null,
            status = 0,
            type = 0,
            nodeHandle = null,
            isCopyOnly = false,
            isSecondary = false,
        )
        whenever(fileGateway.createTempFile(rootPath, localPath, newPath)).thenReturn(Unit)
        underTest.createTempFile(rootPath, syncRecord)
    }

    @Test(expected = NotEnoughStorageException::class)
    fun `test that not enough storage exception is thrown when there is not enough storage`() =
        runTest {
            val localPath = "/path/to/local"
            val newPath = "/path/to/new"
            val rootPath = "/path/to/root"
            val syncRecord = SyncRecord(
                id = 0,
                localPath = localPath,
                newPath = newPath,
                originFingerprint = null,
                newFingerprint = null,
                timestamp = null,
                fileName = null,
                longitude = null,
                latitude = null,
                status = 0,
                type = 0,
                nodeHandle = null,
                isCopyOnly = false,
                isSecondary = false,
            )
            whenever(fileGateway.createTempFile(rootPath, localPath, newPath)).thenThrow(
                NotEnoughStorageException()
            )
            underTest.createTempFile(rootPath, syncRecord)
        }

    @Test(expected = FileNotCreatedException::class)
    fun `test that file not created exception is thrown when file creation is not successful`() =
        runTest {
            val localPath = "/path/to/local"
            val newPath = "/path/to/new"
            val rootPath = "/path/to/root"
            val syncRecord = SyncRecord(
                id = 0,
                localPath = localPath,
                newPath = newPath,
                originFingerprint = null,
                newFingerprint = null,
                timestamp = null,
                fileName = null,
                longitude = null,
                latitude = null,
                status = 0,
                type = 0,
                nodeHandle = null,
                isCopyOnly = false,
                isSecondary = false,
            )
            whenever(fileGateway.createTempFile(rootPath, localPath, newPath)).thenThrow(
                FileNotCreatedException()
            )
            underTest.createTempFile(rootPath, syncRecord)
        }
}
