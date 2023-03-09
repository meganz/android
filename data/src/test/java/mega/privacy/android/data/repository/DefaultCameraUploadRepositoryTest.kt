package mega.privacy.android.data.repository

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.CameraUploadMediaGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MediaStoreFileTypeUriMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsHandlesMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordTypeIntMapper
import mega.privacy.android.data.mapper.syncStatusToInt
import mega.privacy.android.data.mapper.toVideoAttachment
import mega.privacy.android.data.mapper.toVideoQuality
import mega.privacy.android.data.mapper.videoQualityToInt
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.CameraUploadRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.LinkedList
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultCameraUploadRepositoryTest {
    private lateinit var underTest: CameraUploadRepository

    private val localStorageGateway = mock<MegaLocalStorageGateway>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val fileAttributeGateway = mock<FileAttributeGateway>()
    private val cameraUploadMediaGateway = mock<CameraUploadMediaGateway>()
    private val syncRecordTypeIntMapper = mock<SyncRecordTypeIntMapper>()
    private val mediaStoreFileTypeUriWrapper = mock<MediaStoreFileTypeUriMapper>()
    private val cameraUploadsHandlesMapper = mock<CameraUploadsHandlesMapper>()
    private val videoCompressorGateway = mock<VideoCompressorGateway>()

    private val fakeRecord = SyncRecord(
        id = 0,
        localPath = null,
        newPath = null,
        originFingerprint = null,
        newFingerprint = null,
        timestamp = null,
        fileName = null,
        longitude = null,
        latitude = null,
        status = SyncStatus.STATUS_PENDING.value,
        type = -1,
        nodeHandle = null,
        isCopyOnly = false,
        isSecondary = false
    )

    @Before
    fun setUp() {
        underTest = DefaultCameraUploadRepository(
            localStorageGateway = localStorageGateway,
            megaApiGateway = megaApiGateway,
            cacheGateway = mock(),
            fileAttributeGateway = fileAttributeGateway,
            cameraUploadMediaGateway = cameraUploadMediaGateway,
            syncRecordTypeIntMapper = syncRecordTypeIntMapper,
            mediaStoreFileTypeUriMapper = mediaStoreFileTypeUriWrapper,
            cameraUploadsHandlesMapper = cameraUploadsHandlesMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
            appEventGateway = mock(),
            broadcastReceiverGateway = mock(),
            videoQualityIntMapper = ::videoQualityToInt,
            videoQualityMapper = ::toVideoQuality,
            syncStatusIntMapper = ::syncStatusToInt,
            videoCompressorGateway = videoCompressorGateway,
            videoAttachmentMapper = ::toVideoAttachment,
            uploadOptionMapper = mock(),
            uploadOptionIntMapper = mock(),
            context = mock()
        )
    }

    @Test
    fun `test camera upload sync by wifi only setting`() = runTest {
        whenever(localStorageGateway.isSyncByWifi()).thenReturn(true)
        assertThat(underTest.isSyncByWifi()).isTrue()
    }

    @Test
    fun `test camera upload retrieves sync records`() = runTest {
        whenever(localStorageGateway.getPendingSyncRecords()).thenReturn(listOf(fakeRecord))
        assertThat(underTest.getPendingSyncRecords()).isEqualTo(listOf(fakeRecord))
    }

    @Test
    fun `test that the current upload video quality in camera uploads is original quality`() =
        testGetUploadVideoQuality(input = "3", expectedVideoQuality = VideoQuality.ORIGINAL)

    @Test
    fun `test that the current upload video quality in camera uploads is high quality`() =
        testGetUploadVideoQuality(input = "2", expectedVideoQuality = VideoQuality.HIGH)

    @Test
    fun `test that the current upload video quality in camera uploads is medium quality`() =
        testGetUploadVideoQuality(input = "1", expectedVideoQuality = VideoQuality.MEDIUM)

    @Test
    fun `test that the current upload video quality in camera uploads is low quality`() =
        testGetUploadVideoQuality(input = "0", expectedVideoQuality = VideoQuality.LOW)

    @Test
    fun `test that the current upload video quality will return null`() =
        testGetUploadVideoQuality(input = "5", expectedVideoQuality = null)

    private fun testGetUploadVideoQuality(input: String, expectedVideoQuality: VideoQuality?) =
        runTest {
            whenever(localStorageGateway.getUploadVideoQuality()).thenReturn(input)
            assertThat(underTest.getUploadVideoQuality()).isEqualTo(expectedVideoQuality)
        }

    @Test
    fun `test that the videos uploaded through camera uploads will now retain their original resolutions`() =
        testSetUploadVideoQuality(VideoQuality.ORIGINAL)

    @Test
    fun `test that the videos uploaded through camera uploads are now compressed in high quality`() =
        testSetUploadVideoQuality(VideoQuality.HIGH)

    @Test
    fun `test that the videos uploaded through camera uploads are now compressed in medium quality`() =
        testSetUploadVideoQuality(VideoQuality.MEDIUM)

    @Test
    fun `test that the videos uploaded through camera uploads are now compressed in low quality`() =
        testSetUploadVideoQuality(VideoQuality.LOW)

    private fun testSetUploadVideoQuality(videoQuality: VideoQuality) = runTest {
        underTest.setUploadVideoQuality(videoQuality)

        verify(localStorageGateway, times(1)).setUploadVideoQuality(videoQuality.value)
    }

    @Test
    fun `test that the videos to be uploaded by camera uploads are now subject for compression`() =
        testSetUploadVideoSyncStatus(SyncStatus.STATUS_TO_COMPRESS)

    @Test
    fun `test that the videos to be uploaded by camera uploads are now queued for upload`() =
        testSetUploadVideoSyncStatus(SyncStatus.STATUS_PENDING)

    private fun testSetUploadVideoSyncStatus(syncStatus: SyncStatus) = runTest {
        underTest.setUploadVideoSyncStatus(syncStatus)

        verify(
            localStorageGateway,
            times(1)
        ).setUploadVideoSyncStatus(syncStatus.value)
    }

    @Test
    fun `test camera upload gets sync record by fingerprint`() = runTest {
        whenever(
            localStorageGateway.getSyncRecordByFingerprint(
                fingerprint = null,
                isSecondary = false,
                isCopy = false
            )
        ).thenReturn(null)
        assertThat(
            underTest.getSyncRecordByFingerprint(
                fingerprint = null,
                isSecondary = false,
                isCopy = false
            )
        ).isEqualTo(null)
    }

    @Test
    fun `test camera upload gets sync record by new path`() = runTest {
        whenever(localStorageGateway.getSyncRecordByNewPath("")).thenReturn(null)
        assertThat(underTest.getSyncRecordByNewPath("")).isEqualTo(null)
    }

    @Test
    fun `test camera upload gets sync record by local path`() = runTest {
        whenever(localStorageGateway.getSyncRecordByLocalPath("", false)).thenReturn(null)
        assertThat(underTest.getSyncRecordByLocalPath("", false)).isEqualTo(null)
    }

    @Test
    fun `test camera upload retrieves file name exists`() = runTest {
        whenever(localStorageGateway.doesFileNameExist("", false, -1)).thenReturn(true)
        whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)
        assertThat(underTest.doesFileNameExist("", false, SyncRecordType.TYPE_ANY)).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves local path exists`() = runTest {
        whenever(localStorageGateway.doesLocalPathExist("", false, -1)).thenReturn(true)
        whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)
        assertThat(underTest.doesLocalPathExist("", false, SyncRecordType.TYPE_ANY)).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves the correct sync time stamp`() = runTest {
        whenever(localStorageGateway.getPhotoTimeStamp()).thenReturn("150")
        assertThat(underTest.getSyncTimeStamp(SyncTimeStamp.PRIMARY_PHOTO)).isEqualTo(
            150
        )
    }

    @Test
    fun `test camera upload retrieves if credentials exist`() = runTest {
        whenever(localStorageGateway.doCredentialsExist()).thenReturn(true)
        assertThat(underTest.doCredentialsExist()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves if preferences are set`() = runTest {
        whenever(localStorageGateway.doPreferencesExist()).thenReturn(true)
        assertThat(underTest.doPreferencesExist()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves if camera upload sync is enabled`() = runTest {
        whenever(localStorageGateway.isSyncEnabled()).thenReturn(true)
        assertThat(underTest.isSyncEnabled()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves sync local path`() = runTest {
        whenever(localStorageGateway.getSyncLocalPath()).thenReturn("")
        assertThat(underTest.getSyncLocalPath()).isEqualTo("")
    }

    @Test
    fun `test camera upload retrieves secondary folder path`() = runTest {
        whenever(localStorageGateway.getSecondaryFolderPath()).thenReturn("")
        assertThat(underTest.getSecondaryFolderPath()).isEqualTo("")
    }

    @Test
    fun `test that calling areLocationTagsEnabled retrieves the value`() = runTest {
        whenever(localStorageGateway.areLocationTagsEnabled()).thenReturn(false)
        assertThat(underTest.areLocationTagsEnabled()).isEqualTo(false)
    }

    @Test
    fun `test that setLocationTagsEnabled is invoked`() = runTest {
        underTest.setLocationTagsEnabled(true)
        verify(localStorageGateway).setLocationTagsEnabled(true)
    }

    @Test
    fun `test camera upload retrieves keep file names preference`() = runTest {
        whenever(localStorageGateway.getKeepFileNames()).thenReturn(true)
        assertThat(underTest.getKeepFileNames()).isEqualTo(true)
    }

    @Test
    fun `test camera upload folder is on external SD card`() = runTest {
        whenever(localStorageGateway.isFolderExternalSd()).thenReturn(true)
        assertThat(underTest.isFolderExternalSd()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves external SD card URI`() = runTest {
        whenever(localStorageGateway.getUriExternalSd()).thenReturn("")
        assertThat(underTest.getUriExternalSd()).isEqualTo("")
    }

    @Test
    fun `test camera upload retrieves secondary folder enabled preference`() = runTest {
        whenever(localStorageGateway.isSecondaryMediaFolderEnabled()).thenReturn(false)
        assertThat(underTest.isSecondaryMediaFolderEnabled()).isEqualTo(false)
    }

    @Test
    fun `test camera upload if secondary media folder is on external SD card`() = runTest {
        whenever(localStorageGateway.isMediaFolderExternalSd()).thenReturn(false)
        assertThat(underTest.isMediaFolderExternalSd()).isEqualTo(false)
    }

    @Test
    fun `test camera upload retrieves media folder external SD card URI`() = runTest {
        whenever(localStorageGateway.getUriMediaFolderExternalSd()).thenReturn("")
        assertThat(underTest.getUriMediaFolderExternalSd()).isEqualTo("")
    }

    @Test
    fun `test camera upload should clear all sync records`() = runTest {
        whenever(localStorageGateway.shouldClearSyncRecords()).thenReturn(true)
        assertThat(underTest.shouldClearSyncRecords()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves maximal time stamp`() = runTest {
        whenever(localStorageGateway.getMaxTimestamp(false, -1)).thenReturn(1000L)
        whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)
        assertThat(underTest.getMaxTimestamp(false, SyncRecordType.TYPE_ANY)).isEqualTo(1000L)
    }

    @Test
    fun `test camera upload retrieves video sync records by status`() = runTest {
        whenever(localStorageGateway.getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING.value)).thenReturn(
            listOf(fakeRecord)
        )
        assertThat(underTest.getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING)).isEqualTo(
            listOf(fakeRecord)
        )
    }

    @Test
    fun `test camera upload retrieves charging on size`() = runTest {
        whenever(localStorageGateway.getChargingOnSizeString()).thenReturn("1")
        assertThat(underTest.getChargingOnSize()).isEqualTo(1)
    }

    @Test
    fun `test that the device needs to be charged when compressing videos`() =
        testIsChargingRequiredForVideoCompression(true)

    @Test
    fun `test that the device does not need to be charged when compressing videos`() =
        testIsChargingRequiredForVideoCompression(false)

    private fun testIsChargingRequiredForVideoCompression(expectedResult: Boolean) = runTest {
        whenever(localStorageGateway.isChargingRequiredForVideoCompression()).thenReturn(
            expectedResult
        )
        assertThat(underTest.isChargingRequiredForVideoCompression()).isEqualTo(expectedResult)
    }

    @Test
    fun `test that setup primary folder returns success when api set camera upload folders returns API_OK`() =
        runTest {
            val result = 69L
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            val megaRequest = mock<MegaRequest> {
                on { nodeHandle }.thenReturn(result)
            }
            whenever(
                megaApiGateway.setCameraUploadsFolders(
                    any(),
                    any(),
                    listener = any()
                )
            ).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError
                )
            }

            assertThat(underTest.setupPrimaryFolder(1L)).isEqualTo(result)
        }

    @Test(expected = MegaException::class)
    fun `test that setup primary folder returns an exception when api set camera upload folders does not return API_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ENOENT)
            }
            val megaRequest = mock<MegaRequest> {}
            whenever(
                megaApiGateway.setCameraUploadsFolders(
                    any(),
                    any(),
                    listener = any()
                )
            ).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError
                )
            }

            underTest.setupPrimaryFolder(1L)
        }

    @Test
    fun `test that setup secondary folder returns success when api set camera upload folders returns API_OK`() =
        runTest {
            val result = 69L
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            val megaRequest = mock<MegaRequest> {
                on { parentHandle }.thenReturn(result)
            }
            whenever(
                megaApiGateway.setCameraUploadsFolders(
                    any(),
                    any(),
                    listener = any()
                )
            ).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError
                )
            }

            assertThat(underTest.setupSecondaryFolder(1L)).isEqualTo(result)
        }

    @Test(expected = MegaException::class)
    fun `test that setup secondary folder returns an exception when api set camera upload folders does not return API_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ENOENT)
            }
            val megaRequest = mock<MegaRequest> {}
            whenever(
                megaApiGateway.setCameraUploadsFolders(
                    any(),
                    any(),
                    listener = any()
                )
            ).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError
                )
            }

            underTest.setupSecondaryFolder(1L)
        }

    @Test
    fun `test camera upload get the correct media queues by media store file type`() = runTest {
        val result = LinkedList(listOf(CameraUploadMedia("", 1)))
        whenever(
            cameraUploadMediaGateway.getMediaQueue(
                anyOrNull(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            result
        )
        whenever(mediaStoreFileTypeUriWrapper(any())).thenReturn(Uri.EMPTY)
        val actual = underTest.getMediaQueue(
            MediaStoreFileType.IMAGES_INTERNAL,
            "",
            false,
            ""
        )
        assertThat(actual).isEqualTo(result)
    }

    @Test
    fun `test that primary folder handle is returned successfully`() {
        runTest {
            val result = 1L
            whenever(localStorageGateway.getCamSyncHandle()).thenReturn(result)
            val actual = underTest.getPrimarySyncHandle()
            assertThat(actual).isEqualTo(result)
        }
    }

    @Test
    fun `test that secondary folder handle is returned successfully`() {
        runTest {
            val result = 2L
            whenever(localStorageGateway.getMegaHandleSecondaryFolder()).thenReturn(result)
            val actual = underTest.getSecondarySyncHandle()
            assertThat(actual).isEqualTo(result)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `test that reset total uploads is invoked`() = runTest {
        underTest.resetTotalUploads()
        verify(megaApiGateway).resetTotalUploads()
    }

    @Test
    fun `test that correct GPS coordinates are retrieved by file type video`() {
        val result = Pair(6F, 9F)
        runTest {
            whenever(fileAttributeGateway.getVideoGPSCoordinates("")).thenReturn(result)
            val actual = underTest.getVideoGPSCoordinates("")
            assertThat(actual).isEqualTo(result)
        }
    }

    @Test
    fun `test that correct GPS coordinates are retrieved by file type photo`() {
        val result = Pair(6F, 9F)
        runTest {
            whenever(fileAttributeGateway.getPhotoGPSCoordinates("")).thenReturn(result)
            val actual = underTest.getPhotoGPSCoordinates("")
            assertThat(actual).isEqualTo(result)
        }
    }

    @Test
    fun `test that starting video compression emit events in order`() {
        val list = listOf(25, 50, 57, 100)
        val flow = flow {
            list.forEach {
                emit(VideoCompressionState.Progress(it, 1, 2, ""))
            }
            emit(
                VideoCompressionState.FinishedCompression(
                    "",
                    true,
                    1
                )
            )
            emit(VideoCompressionState.Finished)
        }
        runTest {
            whenever(videoCompressorGateway.start()).thenReturn(flow)
            underTest.compressVideos("", VideoQuality.ORIGINAL, emptyList()).test {
                list.forEach {
                    val item = awaitItem()
                    assertThat(item.javaClass).isEqualTo(VideoCompressionState.Progress::class.java)
                    assertThat((item as VideoCompressionState.Progress).progress).isEqualTo(it)
                }
                val finishedCompressionItem = awaitItem()
                assertThat(finishedCompressionItem.javaClass).isEqualTo(VideoCompressionState.FinishedCompression::class.java)
                val finished = awaitItem()
                assertThat(finished.javaClass).isEqualTo(VideoCompressionState.Finished::class.java)
                cancelAndConsumeRemainingEvents()
            }
        }
    }
}
