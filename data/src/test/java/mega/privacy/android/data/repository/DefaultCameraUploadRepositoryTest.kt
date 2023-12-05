package mega.privacy.android.data.repository

import android.net.Uri
import androidx.work.Data
import androidx.work.WorkInfo
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STATUS_INFO
import mega.privacy.android.data.gateway.AndroidDeviceGateway
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.CameraUploadsMediaGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.data.gateway.WorkerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MediaStoreFileTypeUriMapper
import mega.privacy.android.data.mapper.VideoQualityIntMapper
import mega.privacy.android.data.mapper.VideoQualityMapper
import mega.privacy.android.data.mapper.camerauploads.BackupStateIntMapper
import mega.privacy.android.data.mapper.camerauploads.BackupStateMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsHandlesMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsStatusInfoMapper
import mega.privacy.android.data.mapper.camerauploads.HeartbeatStatusIntMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordTypeIntMapper
import mega.privacy.android.data.mapper.camerauploads.UploadOptionIntMapper
import mega.privacy.android.data.mapper.camerauploads.UploadOptionMapper
import mega.privacy.android.data.mapper.syncStatusToInt
import mega.privacy.android.data.mapper.toVideoAttachment
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.CameraUploadRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.LinkedList
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertFailsWith

/**
 * Test class for [DefaultCameraUploadRepository]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultCameraUploadRepositoryTest {
    private lateinit var underTest: CameraUploadRepository

    private val localStorageGateway = mock<MegaLocalStorageGateway>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val fileGateway = mock<FileGateway>()
    private val cameraUploadsMediaGateway = mock<CameraUploadsMediaGateway>()
    private val workerGateway = mock<WorkerGateway>()
    private val syncRecordTypeIntMapper = mock<SyncRecordTypeIntMapper>()
    private val heartbeatStatusIntMapper = mock<HeartbeatStatusIntMapper>()
    private val mediaStoreFileTypeUriWrapper = mock<MediaStoreFileTypeUriMapper>()
    private val cameraUploadsHandlesMapper = mock<CameraUploadsHandlesMapper>()
    private val backupStateMapper = mock<BackupStateMapper>()
    private val backupStateIntMapper = mock<BackupStateIntMapper>()
    private val videoCompressorGateway = mock<VideoCompressorGateway>()
    private val appEventGateway = mock<AppEventGateway>()
    private val uploadOptionIntMapper = mock<UploadOptionIntMapper>()
    private val uploadOptionMapper = mock<UploadOptionMapper>()
    private val deviceGateway = mock<AndroidDeviceGateway>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val videoQualityMapper: VideoQualityMapper = mock()
    private val videoQualityIntMapper: VideoQualityIntMapper = mock()
    private val cameraUploadsSettingsPreferenceGateway: CameraUploadsSettingsPreferenceGateway =
        mock()
    private val cameraUploadsStatusInfoMapper: CameraUploadsStatusInfoMapper = mock()

    private val fakeRecord = SyncRecord(
        id = 0,
        localPath = "localPath",
        newPath = null,
        originFingerprint = null,
        newFingerprint = null,
        timestamp = 0L,
        fileName = "fileName.jpg",
        longitude = null,
        latitude = null,
        status = SyncStatus.STATUS_PENDING.value,
        type = SyncRecordType.TYPE_ANY,
        nodeHandle = null,
        isCopyOnly = false,
        isSecondary = false,
    )

    @BeforeAll
    fun setUp() {
        underTest = DefaultCameraUploadRepository(
            localStorageGateway = localStorageGateway,
            megaApiGateway = megaApiGateway,
            cacheGateway = mock(),
            fileGateway = fileGateway,
            cameraUploadsMediaGateway = cameraUploadsMediaGateway,
            workerGateway = workerGateway,
            syncRecordTypeIntMapper = syncRecordTypeIntMapper,
            heartbeatStatusIntMapper = heartbeatStatusIntMapper,
            mediaStoreFileTypeUriMapper = mediaStoreFileTypeUriWrapper,
            cameraUploadsHandlesMapper = cameraUploadsHandlesMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
            appEventGateway = appEventGateway,
            deviceGateway = deviceGateway,
            videoQualityIntMapper = videoQualityIntMapper,
            videoQualityMapper = videoQualityMapper,
            syncStatusIntMapper = ::syncStatusToInt,
            backupStateMapper = backupStateMapper,
            backupStateIntMapper = backupStateIntMapper,
            videoCompressorGateway = videoCompressorGateway,
            videoAttachmentMapper = ::toVideoAttachment,
            uploadOptionMapper = uploadOptionMapper,
            uploadOptionIntMapper = uploadOptionIntMapper,
            megaLocalRoomGateway = megaLocalRoomGateway,
            context = mock(),
            cameraUploadsSettingsPreferenceGateway = cameraUploadsSettingsPreferenceGateway,
            cameraUploadsStatusInfoMapper = cameraUploadsStatusInfoMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            localStorageGateway,
            megaApiGateway,
            fileGateway,
            cameraUploadsMediaGateway,
            workerGateway,
            syncRecordTypeIntMapper,
            heartbeatStatusIntMapper,
            mediaStoreFileTypeUriWrapper,
            cameraUploadsHandlesMapper,
            videoCompressorGateway,
            appEventGateway,
            deviceGateway,
            backupStateMapper,
            backupStateIntMapper,
            uploadOptionMapper,
            uploadOptionIntMapper,
            cameraUploadsSettingsPreferenceGateway,
            cameraUploadsStatusInfoMapper,
        )
    }

    @Nested
    @DisplayName("Connection Type")
    inner class ConnectionTypeTest {
        @ParameterizedTest(name = "wi-fi only: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that camera uploads can only run on specific connection types`(wifiOnly: Boolean) =
            runTest {
                whenever(cameraUploadsSettingsPreferenceGateway.isUploadByWifi()).thenReturn(
                    wifiOnly
                )
                assertThat(underTest.isCameraUploadsByWifi()).isEqualTo(wifiOnly)
            }

        @ParameterizedTest(name = "wi-fi only: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that camera uploads can now be run on specific connection types`(wifiOnly: Boolean) =
            runTest {
                underTest.setCameraUploadsByWifi(wifiOnly)
                verify(cameraUploadsSettingsPreferenceGateway).setUploadsByWifi(wifiOnly)
            }
    }


    @Nested
    @DisplayName("Content to Upload")
    inner class ContentToUpload {
        @TestFactory
        fun `test that camera uploads will upload specific content`() =
            listOf(
                1001 to UploadOption.PHOTOS,
                1002 to UploadOption.VIDEOS,
                1003 to UploadOption.PHOTOS_AND_VIDEOS,
                1004 to null
            ).map { (input, expectedUploadOption) ->
                dynamicTest("test that the value $input will return $expectedUploadOption") {
                    runTest {
                        whenever(cameraUploadsSettingsPreferenceGateway.getFileUploadOption()).thenReturn(
                            input
                        )
                        whenever(uploadOptionMapper(input)).thenReturn(expectedUploadOption)

                        assertThat(underTest.getUploadOption()).isEqualTo(expectedUploadOption)
                    }
                }
            }

        @ParameterizedTest(name = "upload option: {0}")
        @EnumSource(UploadOption::class)
        fun `test that a new upload option for camera uploads is set`(uploadOption: UploadOption) =
            runTest {
                underTest.setUploadOption(uploadOption)
                verify(cameraUploadsSettingsPreferenceGateway).setFileUploadOption(
                    uploadOptionIntMapper(
                        uploadOption
                    )
                )
            }

        @Test
        fun `test that the correct media queues are retrieved by media store file type`() =
            runTest {
                val result =
                    LinkedList(listOf(CameraUploadsMedia(1234L, "displayName", "filePath", 1)))

                whenever(
                    cameraUploadsMediaGateway.getMediaList(
                        uri = anyOrNull(),
                        selectionQuery = any(),
                    )
                ).thenReturn(
                    result
                )
                whenever(mediaStoreFileTypeUriWrapper(any())).thenReturn(Uri.EMPTY)

                val actual = underTest.getMediaList(
                    mediaStoreFileType = MediaStoreFileType.IMAGES_INTERNAL,
                    selectionQuery = "",
                )
                assertThat(actual).isEqualTo(result)
            }

        @Test
        fun `test that getMediaSelectionQuery returns the result of cameraUploadsMediaGateway getMediaSelectionQuery`() =
            runTest {
                val expected = "selectionQuery"
                val parentPath = "parentPath"
                whenever(cameraUploadsMediaGateway.getMediaSelectionQuery(parentPath)).thenReturn(
                    expected
                )
                assertThat(underTest.getMediaSelectionQuery(parentPath)).isEqualTo(expected)
            }
    }

    @Nested
    @DisplayName("Video Upload Quality")
    inner class VideoUploadQuality {
        @TestFactory
        fun `test that camera uploads will upload videos on a specific video quality`() =
            listOf(
                0 to VideoQuality.LOW,
                1 to VideoQuality.MEDIUM,
                2 to VideoQuality.HIGH,
                3 to VideoQuality.ORIGINAL,
                4 to null,
            ).map { (input, expectedVideoQuality) ->
                dynamicTest("test that the value $input will return $expectedVideoQuality") {
                    runTest {
                        whenever(videoQualityMapper.invoke(input)).thenReturn(expectedVideoQuality)
                        whenever(cameraUploadsSettingsPreferenceGateway.getUploadVideoQuality()).thenReturn(
                            input
                        )
                        assertThat(underTest.getUploadVideoQuality()).isEqualTo(expectedVideoQuality)
                    }
                }
            }

        @ParameterizedTest(name = "video quality: {0}")
        @EnumSource(VideoQuality::class)
        fun `test that a new video quality is set when uploading videos`(videoQuality: VideoQuality) =
            runTest {
                whenever(videoQualityIntMapper.invoke(videoQuality)).thenReturn(videoQuality.value)
                underTest.setUploadVideoQuality(videoQuality)
                verify(cameraUploadsSettingsPreferenceGateway).setUploadVideoQuality(videoQuality.value)
            }
    }

    @Nested
    @DisplayName("Video Sync Status")
    inner class VideoSyncStatus {
        @ParameterizedTest(name = "video sync status: {0}")
        @EnumSource(SyncStatus::class)
        fun `test that the upload video sync status is updated`(syncStatus: SyncStatus) = runTest {
            underTest.setUploadVideoSyncStatus(syncStatus)
            verify(megaLocalRoomGateway).setUploadVideoSyncStatus(syncStatus.value)
        }
    }

    @Nested
    @DisplayName("Sync")
    inner class SyncTest {
        @Test
        fun `test that the camera uploads sync records are retrieved`() = runTest {
            whenever(megaLocalRoomGateway.getPendingSyncRecords()).thenReturn(listOf(fakeRecord))
            assertThat(underTest.getPendingSyncRecords()).isEqualTo(listOf(fakeRecord))
        }

        @Test
        fun `test that camera uploads retrieves the sync record by fingerprint`() = runTest {
            whenever(
                megaLocalRoomGateway.getSyncRecordByFingerprint(
                    fingerprint = anyOrNull(),
                    isSecondary = any(),
                    isCopy = any(),
                )
            ).thenReturn(null)
            assertThat(
                underTest.getSyncRecordByFingerprint(
                    fingerprint = null,
                    isSecondary = false,
                    isCopy = false,
                )
            ).isEqualTo(null)
        }

        @Test
        fun `test that camera uploads retrieves the sync record by new path`() = runTest {
            whenever(megaLocalRoomGateway.getSyncRecordByNewPath(any())).thenReturn(null)
            assertThat(underTest.getSyncRecordByNewPath("")).isEqualTo(null)
        }

        @Test
        fun `test that camera uploads retrieves the sync record by local path`() = runTest {
            whenever(
                megaLocalRoomGateway.getSyncRecordByLocalPath(
                    path = any(),
                    isSecondary = any(),
                )
            ).thenReturn(null)
            assertThat(
                underTest.getSyncRecordByLocalPath(
                    path = "",
                    isSecondary = false,
                )
            ).isEqualTo(null)
        }

        @ParameterizedTest(name = "file name exists: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the file name exists or not`(fileNameExists: Boolean) = runTest {
            whenever(
                megaLocalRoomGateway.doesFileNameExist(
                    fileName = any(),
                    isSecondary = any(),
                )
            ).thenReturn(
                fileNameExists
            )
            whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)

            assertThat(
                underTest.doesFileNameExist(
                    fileName = "",
                    isSecondary = false,
                )
            ).isEqualTo(fileNameExists)
        }

        @ParameterizedTest(name = "local path exists: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the local path exists or not`(localPathExists: Boolean) = runTest {
            whenever(
                megaLocalRoomGateway.doesLocalPathExist(
                    fileName = any(),
                    isSecondary = any(),
                )
            ).thenReturn(
                localPathExists
            )
            whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)

            assertThat(
                underTest.doesLocalPathExist(
                    fileName = "",
                    isSecondary = false,
                )
            ).isEqualTo(localPathExists)
        }

        @Test
        fun `test that the sync time stamp is retrieved`() = runTest {
            val testTimeStamp = 150L

            whenever(cameraUploadsSettingsPreferenceGateway.getPhotoTimeStamp()).thenReturn(
                testTimeStamp
            )
            assertThat(underTest.getSyncTimeStamp(SyncTimeStamp.PRIMARY_PHOTO)).isEqualTo(
                testTimeStamp
            )
        }

        @ParameterizedTest(name = "sync enabled: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that camera uploads is enabled or not`(syncEnabled: Boolean) = runTest {
            whenever(cameraUploadsSettingsPreferenceGateway.isCameraUploadsEnabled()).thenReturn(
                syncEnabled
            )
            assertThat(underTest.isCameraUploadsEnabled()).isEqualTo(syncEnabled)
        }

        @ParameterizedTest(name = "clear all sync records: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the sync records should be cleared or not`(clearAllSyncRecords: Boolean) =
            runTest {
                whenever(localStorageGateway.shouldClearSyncRecords()).thenReturn(
                    clearAllSyncRecords
                )
                assertThat(underTest.shouldClearSyncRecords()).isEqualTo(clearAllSyncRecords)
            }

        @Test
        fun `test that the maximum time stamp is retrieved`() = runTest {
            val testTimeStamps = listOf(1000L, 999L, 1L)

            whenever(
                megaLocalRoomGateway.getAllTimestampsOfSyncRecord(
                    isSecondary = any(),
                    syncRecordType = any(),
                )
            ).thenReturn(testTimeStamps)
            whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)

            assertThat(
                underTest.getMaxTimestamp(
                    isSecondary = false,
                    syncRecordType = SyncRecordType.TYPE_ANY,
                )
            ).isEqualTo(
                1000L
            )
        }

        @Test
        fun `test that the zero is returned when time stamps are empty`() = runTest {
            val testTimeStamps = emptyList<Long>()

            whenever(
                megaLocalRoomGateway.getAllTimestampsOfSyncRecord(
                    isSecondary = any(),
                    syncRecordType = any(),
                )
            ).thenReturn(testTimeStamps)
            whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)

            assertThat(
                underTest.getMaxTimestamp(
                    isSecondary = false,
                    syncRecordType = SyncRecordType.TYPE_ANY,
                )
            ).isEqualTo(
                0L
            )
        }

        @Test
        fun `test that the video sync records are retrieved by status`() = runTest {
            whenever(megaLocalRoomGateway.getVideoSyncRecordsByStatus(any())).thenReturn(
                listOf(fakeRecord)
            )
            assertThat(underTest.getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING)).isEqualTo(
                listOf(fakeRecord)
            )
        }
    }

    @Nested
    @DisplayName("Credentials")
    inner class CredentialsTest {
        @ParameterizedTest(name = "credentials exist: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the credentials exist or not`(credentialsExist: Boolean) = runTest {
            whenever(localStorageGateway.doCredentialsExist()).thenReturn(credentialsExist)
            assertThat(underTest.doCredentialsExist()).isEqualTo(credentialsExist)
        }
    }

    @Nested
    @DisplayName("Preferences")
    inner class PreferencesTest {
        @ParameterizedTest(name = "preferences exist: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the preferences exist or not`(preferencesExist: Boolean) = runTest {
            whenever(localStorageGateway.doPreferencesExist()).thenReturn(preferencesExist)
            assertThat(underTest.doPreferencesExist()).isEqualTo(preferencesExist)
        }
    }

    @Nested
    @DisplayName("Primary Folder")
    inner class PrimaryFolderTest {
        @Test
        fun `test that the primary folder local path is retrieved`() = runTest {
            val testPath = "test/primary/path"

            whenever(cameraUploadsSettingsPreferenceGateway.getCameraUploadsLocalPath()).thenReturn(
                testPath
            )
            assertThat(underTest.getPrimaryFolderLocalPath()).isEqualTo(testPath)
        }

        @Test
        fun `test that the new primary folder local path is set`() = runTest {
            val testPath = "test/new/primary/path"

            underTest.setPrimaryFolderLocalPath(testPath)
            verify(cameraUploadsSettingsPreferenceGateway).setCameraUploadsLocalPath(testPath)
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
                        primaryFolder = any(),
                        secondaryFolder = any(),
                        listener = any(),
                    )
                ).thenAnswer {
                    (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                        api = mock(),
                        request = megaRequest,
                        error = megaError,
                    )
                }
                assertThat(underTest.setupPrimaryFolder(1L)).isEqualTo(result)
            }

        @Test
        fun `test that setup primary folder returns an exception when api set camera upload folders does not return API_OK`() =
            runTest {
                val megaError = mock<MegaError> {
                    on { errorCode }.thenReturn(MegaError.API_ENOENT)
                }
                val megaRequest = mock<MegaRequest> {}

                whenever(
                    megaApiGateway.setCameraUploadsFolders(
                        primaryFolder = any(),
                        secondaryFolder = any(),
                        listener = any(),
                    )
                ).thenAnswer {
                    (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                        api = mock(),
                        request = megaRequest,
                        error = megaError,
                    )
                }

                assertFailsWith(
                    exceptionClass = MegaException::class,
                    block = { underTest.setupPrimaryFolder(1L) },
                )
            }

        @Test
        fun `test that the primary folder handle is retrieved`() = runTest {
            val testHandle = 1L

            whenever(cameraUploadsSettingsPreferenceGateway.getCameraUploadsHandle()).thenReturn(
                testHandle
            )
            assertThat(underTest.getPrimarySyncHandle()).isEqualTo(testHandle)
        }
    }

    @Nested
    @DisplayName("Secondary Folder")
    inner class SecondaryFolderTest {
        @Test
        fun `test that the secondary folder local path is retrieved`() = runTest {
            val testPath = "test/secondary/path"

            whenever(cameraUploadsSettingsPreferenceGateway.getMediaUploadsLocalPath()).thenReturn(
                testPath
            )
            assertThat(underTest.getSecondaryFolderLocalPath()).isEqualTo(testPath)
        }

        @Test
        fun `test that the new secondary folder local path is set`() = runTest {
            val testPath = "test/new/secondary/path"

            underTest.setSecondaryFolderLocalPath(testPath)
            verify(cameraUploadsSettingsPreferenceGateway).setMediaUploadsLocalPath(testPath)
        }

        @ParameterizedTest(name = "secondary folder enabled: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the secondary folder is enabled or not`(enabled: Boolean) =
            runTest {
                whenever(cameraUploadsSettingsPreferenceGateway.isMediaUploadsEnabled()).thenReturn(
                    enabled
                )
                assertThat(underTest.isSecondaryMediaFolderEnabled()).isEqualTo(enabled)
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
                        primaryFolder = any(),
                        secondaryFolder = any(),
                        listener = any(),
                    )
                ).thenAnswer {
                    (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                        api = mock(),
                        request = megaRequest,
                        error = megaError,
                    )
                }
                assertThat(underTest.setupSecondaryFolder(1L)).isEqualTo(result)
            }

        @Test
        fun `test that setup secondary folder returns an exception when api set camera upload folders does not return API_OK`() =
            runTest {
                val megaError = mock<MegaError> {
                    on { errorCode }.thenReturn(MegaError.API_ENOENT)
                }
                val megaRequest = mock<MegaRequest> {}
                whenever(
                    megaApiGateway.setCameraUploadsFolders(
                        primaryFolder = any(),
                        secondaryFolder = any(),
                        listener = any(),
                    )
                ).thenAnswer {
                    (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                        api = mock(),
                        request = megaRequest,
                        error = megaError,
                    )
                }

                assertFailsWith(
                    exceptionClass = MegaException::class,
                    block = { underTest.setupSecondaryFolder(1L) },
                )
            }

        @Test
        fun `test that the secondary folder handle is retrieved`() {
            runTest {
                val testHandle = 2L

                whenever(cameraUploadsSettingsPreferenceGateway.getMediaUploadsHandle()).thenReturn(
                    testHandle
                )
                assertThat(underTest.getSecondarySyncHandle()).isEqualTo(testHandle)
            }
        }
    }

    @Nested
    @DisplayName("Location Tags")
    inner class LocationTagsTest {
        @ParameterizedTest(name = "location tags enabled: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the location tags are added or not when uploading photos`(includeLocationTags: Boolean) =
            runTest {
                whenever(cameraUploadsSettingsPreferenceGateway.areLocationTagsEnabled()).thenReturn(
                    includeLocationTags
                )
                assertThat(underTest.areLocationTagsEnabled()).isEqualTo(includeLocationTags)
            }

        @ParameterizedTest(name = "location tags enabled: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the location tags are now handled when uploading photos`(includeLocationTags: Boolean) =
            runTest {
                underTest.setLocationTagsEnabled(includeLocationTags)
                verify(cameraUploadsSettingsPreferenceGateway).setLocationTagsEnabled(
                    includeLocationTags
                )
            }
    }

    @Nested
    @DisplayName("File Names")
    inner class FileNamesTest {
        @ParameterizedTest(name = "file names kept: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the file names are kept or not when uploading content`(keepFileNames: Boolean) =
            runTest {
                whenever(cameraUploadsSettingsPreferenceGateway.areUploadFileNamesKept()).thenReturn(
                    keepFileNames
                )
                assertThat(underTest.areUploadFileNamesKept()).isEqualTo(keepFileNames)
            }

        @ParameterizedTest(name = "file names kept: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the file names for uploads are handled`(keepFileNames: Boolean) =
            runTest {
                underTest.setUploadFileNamesKept(keepFileNames)
                verify(cameraUploadsSettingsPreferenceGateway).setUploadFileNamesKept(keepFileNames)
            }
    }

    @Nested
    @DisplayName("Video Compression")
    inner class VideoCompressionTest {
        @ParameterizedTest(name = "charging required: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that charging could be required to compress videos`(chargingRequired: Boolean) =
            runTest {
                whenever(cameraUploadsSettingsPreferenceGateway.isChargingRequiredForVideoCompression()).thenReturn(
                    chargingRequired
                )
                assertThat(underTest.isChargingRequiredForVideoCompression()).isEqualTo(
                    chargingRequired
                )
            }

        @ParameterizedTest(name = "charging required: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that compressing videos will depend on the device charging status`(
            chargingRequired: Boolean,
        ) =
            runTest {
                underTest.setChargingRequiredForVideoCompression(chargingRequired)
                verify(cameraUploadsSettingsPreferenceGateway).setChargingRequiredForVideoCompression(
                    chargingRequired
                )
            }

        @ParameterizedTest(name = "device charging: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that charging state is correctly returned`(
            isCharging: Boolean,
        ) =
            runTest {
                whenever(deviceGateway.isCharging()).thenReturn(isCharging)
                val actual = underTest.isCharging()
                assertThat(actual).isEqualTo(isCharging)
            }

        @Test
        fun `test that the video compression size limit is retrieved`() = runTest {
            val testSizeLimit = 300

            whenever(cameraUploadsSettingsPreferenceGateway.getVideoCompressionSizeLimit()).thenReturn(
                testSizeLimit
            )
            assertThat(underTest.getVideoCompressionSizeLimit()).isEqualTo(testSizeLimit)
        }

        @Test
        fun `test that the new video compression size limit is set`() = runTest {
            val testSizeLimit = 300

            underTest.setVideoCompressionSizeLimit(testSizeLimit)
            verify(cameraUploadsSettingsPreferenceGateway).setVideoCompressionSizeLimit(
                testSizeLimit
            )
        }

        @Test
        fun `test that starting video compression emits events in order`() {
            runTest {
                val list = listOf(25, 50, 57, 100)
                val flow = flow {
                    list.forEach {
                        emit(
                            VideoCompressionState.Progress(
                                progress = it,
                                currentIndex = 1,
                                totalCount = 2,
                                path = "",
                            )
                        )
                    }
                    emit(
                        VideoCompressionState.FinishedCompression(
                            returnedFile = "",
                            isSuccess = true,
                            messageId = 1,
                        )
                    )
                    emit(VideoCompressionState.Finished)
                }

                whenever(videoCompressorGateway.start()).thenReturn(flow)
                underTest.compressVideos(
                    root = "",
                    quality = VideoQuality.ORIGINAL,
                    records = emptyList(),
                ).test {
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

    @Nested
    @DisplayName("Camera Uploads Operation")
    inner class CameraUploadsOperationTest {
        @Test
        fun `test that the worker is called to start camera uploads`() = runTest {
            underTest.startCameraUploads()
            verify(workerGateway).startCameraUploads()
        }

        @Test
        fun `test that the worker is called to stop camera uploads`() = runTest {
            val shouldReschedule = false
            underTest.stopCameraUploads(shouldReschedule)
            verify(workerGateway).stopCameraUploads(shouldReschedule)
        }

        @Test
        fun `test that the worker is called to schedule camera uploads`() = runTest {
            underTest.scheduleCameraUploads()
            verify(workerGateway).scheduleCameraUploads()
        }

        @Test
        fun `test that the worker is called to stop camera uploads heartbeat workers`() = runTest {
            underTest.stopCameraUploadSyncHeartbeatWorkers()
            verify(workerGateway).cancelCameraUploadAndHeartbeatWorkRequest()
        }

        @Test
        fun `test that the app event gateway is notified of the camera uploads progress`() {
            runTest {
                val expected = Pair(50, 25)

                underTest.broadcastCameraUploadProgress(
                    progress = expected.first,
                    pending = expected.second,
                )
                verify(appEventGateway).broadcastCameraUploadProgress(
                    progress = expected.first,
                    pending = expected.second,
                )
            }
        }

        @Test
        fun `test that the camera uploads progress is being observed`() {
            runTest {
                val progress1 = Pair(50, 25)
                val progress2 = Pair(51, 24)
                val expected = flowOf(progress1, progress2)

                whenever(appEventGateway.monitorCameraUploadProgress).thenReturn(expected)
                assertThat(underTest.monitorCameraUploadProgress()).isEqualTo(expected)
            }
        }

        @Test
        fun `test that the camera uploads status info is being observed`() {
            runTest {
                val progress = mock<Data> {
                    on { keyValueMap }.thenReturn(mapOf<String, Any>(Pair(STATUS_INFO, PROGRESS)))
                }
                val workInfo = mock<WorkInfo> {
                    on { this.progress }.thenReturn(progress)
                }
                val workInfoFlow = flowOf(listOf(workInfo, mock()))

                val expected = mock<CameraUploadsStatusInfo.Progress>()

                whenever(cameraUploadsStatusInfoMapper(workInfo.progress)).thenReturn(expected)
                whenever(workerGateway.monitorCameraUploadsStatusInfo()).thenReturn(workInfoFlow)
                underTest.monitorCameraUploadsStatusInfo().test {
                    assertThat(awaitItem()).isEqualTo(expected)
                    cancelAndConsumeRemainingEvents()
                }
            }
        }
    }
}
