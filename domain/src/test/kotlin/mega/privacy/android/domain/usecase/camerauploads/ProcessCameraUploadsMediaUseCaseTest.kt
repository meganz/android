package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.mapper.CameraUploadsRecordMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcessCameraUploadsMediaUseCaseTest {

    private lateinit var underTest: ProcessCameraUploadsMediaUseCase

    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()
    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()
    private val getMediaStoreFileTypesUseCase = mock<GetMediaStoreFileTypesUseCase>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val retrieveMediaFromMediaStoreUseCase = mock<RetrieveMediaFromMediaStoreUseCase>()
    private val cameraUploadsRecordMapper = mock<CameraUploadsRecordMapper>()
    private val saveCameraUploadsRecordUseCase = mock<SaveCameraUploadsRecordUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = ProcessCameraUploadsMediaUseCase(
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            getMediaStoreFileTypesUseCase = getMediaStoreFileTypesUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            retrieveMediaFromMediaStoreUseCase = retrieveMediaFromMediaStoreUseCase,
            saveCameraUploadsRecordUseCase = saveCameraUploadsRecordUseCase,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            getPrimaryFolderPathUseCase,
            getSecondaryFolderPathUseCase,
            getMediaStoreFileTypesUseCase,
            isSecondaryFolderEnabled,
            retrieveMediaFromMediaStoreUseCase,
            cameraUploadsRecordMapper,
            saveCameraUploadsRecordUseCase,
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("test that media are retrieved")
    inner class MediaRetrieved {

        @ParameterizedTest(name = "when file type list is {0}")
        @MethodSource("mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCaseTest#provideImageMediaStoreFileTypeParameters")
        fun `test that photo media are being retrieved for the primary folder if file types list contains image file type`(
            mediaStoreFileType: List<MediaStoreFileType>,
        ) = runTest {
            val primaryFolderPath = "primaryFolderPath"
            val folderType = CameraUploadFolderType.Primary
            val fileType = SyncRecordType.TYPE_PHOTO
            val tempRoot = "tempRoot"
            whenever(getMediaStoreFileTypesUseCase()).thenReturn(mediaStoreFileType)
            whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
            whenever(retrieveMediaFromMediaStoreUseCase(any(), any(), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(isSecondaryFolderEnabled()).thenReturn(false)

            val (photoFileTypes, videoFileTypes) = mediaStoreFileType.partition { it.isImageFileType() }

            underTest("tempRoot")

            verify(retrieveMediaFromMediaStoreUseCase).invoke(
                primaryFolderPath,
                photoFileTypes,
                folderType,
                fileType,
                tempRoot,
            )
            verify(retrieveMediaFromMediaStoreUseCase, never()).invoke(
                primaryFolderPath,
                videoFileTypes,
                folderType,
                fileType,
                tempRoot,
            )
        }

        @ParameterizedTest(name = "when file type list is {0}")
        @MethodSource("mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCaseTest#provideVideoMediaStoreFileTypeParameters")
        fun `test that video media are being retrieved for the primary folder if file types list contain video type`(
            mediaStoreFileType: List<MediaStoreFileType>,
        ) = runTest {
            val primaryFolderPath = "primaryFolderPath"
            val folderType = CameraUploadFolderType.Primary
            val tempRoot = "tempRoot"
            whenever(getMediaStoreFileTypesUseCase()).thenReturn(mediaStoreFileType)
            whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
            whenever(retrieveMediaFromMediaStoreUseCase(any(), any(), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(isSecondaryFolderEnabled()).thenReturn(false)

            val (photoFileTypes, videoFileTypes) = mediaStoreFileType.partition { it.isImageFileType() }

            underTest("tempRoot")

            verify(retrieveMediaFromMediaStoreUseCase, never()).invoke(
                primaryFolderPath,
                photoFileTypes,
                folderType,
                SyncRecordType.TYPE_PHOTO,
                tempRoot,
            )
            verify(retrieveMediaFromMediaStoreUseCase).invoke(
                primaryFolderPath,
                videoFileTypes,
                folderType,
                SyncRecordType.TYPE_VIDEO,
                tempRoot,
            )
        }

        @ParameterizedTest(name = "when file type list is {0}")
        @MethodSource("mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCaseTest#provideBothTypesMediaStoreFileTypeParameters")
        fun `test that photo and video media are being retrieved for the primary folder if file types list contain both type`(
            mediaStoreFileType: List<MediaStoreFileType>,
        ) = runTest {
            val primaryFolderPath = "primaryFolderPath"
            val folderType = CameraUploadFolderType.Primary
            val tempRoot = "tempRoot"
            whenever(getMediaStoreFileTypesUseCase()).thenReturn(mediaStoreFileType)
            whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
            whenever(retrieveMediaFromMediaStoreUseCase(any(), any(), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(isSecondaryFolderEnabled()).thenReturn(false)

            val types = mediaStoreFileType.partition { it.isImageFileType() }

            underTest("tempRoot")

            verify(retrieveMediaFromMediaStoreUseCase).invoke(
                primaryFolderPath,
                types.first,
                folderType,
                SyncRecordType.TYPE_PHOTO,
                tempRoot,
            )
            verify(retrieveMediaFromMediaStoreUseCase).invoke(
                primaryFolderPath,
                types.second,
                folderType,
                SyncRecordType.TYPE_VIDEO,
                tempRoot,
            )
        }

        @ParameterizedTest(name = "when file type list is {0}")
        @MethodSource("mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCaseTest#provideAllMediaStoreFileTypeParameters")
        fun `test that media for secondary folder are not retrieved if secondary folder is disabled`(
            mediaStoreFileType: List<MediaStoreFileType>,
        ) = runTest {
            val primaryFolderPath = "primaryFolderPath"
            val secondaryFolderPath = "secondaryFolderPath"
            whenever(getMediaStoreFileTypesUseCase()).thenReturn(mediaStoreFileType)
            whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
            whenever(retrieveMediaFromMediaStoreUseCase(any(), any(), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(isSecondaryFolderEnabled()).thenReturn(false)
            whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)

            underTest("tempRoot")

            verify(retrieveMediaFromMediaStoreUseCase, never()).invoke(
                eq(secondaryFolderPath),
                any(),
                any(),
                any(),
                any(),
            )
        }

        @ParameterizedTest(name = "when file type list is {0}")
        @MethodSource("mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCaseTest#provideImageMediaStoreFileTypeParameters")
        fun `test that photo media are being retrieved for the secondary folder if file types list contains image file type`(
            mediaStoreFileType: List<MediaStoreFileType>,
        ) = runTest {
            val primaryFolderPath = "primaryFolderPath"
            val secondaryFolderPath = "secondaryFolderPath"
            val folderType = CameraUploadFolderType.Secondary
            val tempRoot = "tempRoot"
            whenever(getMediaStoreFileTypesUseCase()).thenReturn(mediaStoreFileType)
            whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
            whenever(retrieveMediaFromMediaStoreUseCase(any(), any(), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)

            val types = mediaStoreFileType.partition { it.isImageFileType() }

            underTest("tempRoot")

            verify(retrieveMediaFromMediaStoreUseCase).invoke(
                secondaryFolderPath,
                types.first,
                folderType,
                SyncRecordType.TYPE_PHOTO,
                tempRoot,
            )
            verify(retrieveMediaFromMediaStoreUseCase, never()).invoke(
                secondaryFolderPath,
                types.second,
                folderType,
                SyncRecordType.TYPE_VIDEO,
                tempRoot
            )
        }

        @ParameterizedTest(name = "when file type list is {0}")
        @MethodSource("mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCaseTest#provideVideoMediaStoreFileTypeParameters")
        fun `test that video media are being retrieved for the secondary folder if file types list contain video type`(
            mediaStoreFileType: List<MediaStoreFileType>,
        ) = runTest {
            val primaryFolderPath = "primaryFolderPath"
            val secondaryFolderPath = "secondaryFolderPath"
            val folderType = CameraUploadFolderType.Secondary
            val tempRoot = "tempRoot"
            whenever(getMediaStoreFileTypesUseCase()).thenReturn(mediaStoreFileType)
            whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
            whenever(retrieveMediaFromMediaStoreUseCase(any(), any(), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)

            val types = mediaStoreFileType.partition { it.isImageFileType() }

            underTest("tempRoot")

            verify(retrieveMediaFromMediaStoreUseCase, never()).invoke(
                secondaryFolderPath,
                types.first,
                folderType,
                SyncRecordType.TYPE_PHOTO,
                tempRoot,
            )
            verify(retrieveMediaFromMediaStoreUseCase).invoke(
                secondaryFolderPath,
                types.second,
                folderType,
                SyncRecordType.TYPE_VIDEO,
                tempRoot,
            )
        }

        @ParameterizedTest(name = "when file type list is {0}")
        @MethodSource("mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCaseTest#provideBothTypesMediaStoreFileTypeParameters")
        fun `test that photo and video media are being retrieved for the secondary folder if file types list contain both type`(
            mediaStoreFileType: List<MediaStoreFileType>,
        ) = runTest {
            val primaryFolderPath = "primaryFolderPath"
            val secondaryFolderPath = "secondaryFolderPath"
            val folderType = CameraUploadFolderType.Secondary
            val tempRoot = "tempRoot"
            whenever(getMediaStoreFileTypesUseCase()).thenReturn(mediaStoreFileType)
            whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
            whenever(retrieveMediaFromMediaStoreUseCase(any(), any(), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)

            val types = mediaStoreFileType.partition { it.isImageFileType() }

            underTest("tempRoot")

            verify(retrieveMediaFromMediaStoreUseCase).invoke(
                secondaryFolderPath,
                types.first,
                folderType,
                SyncRecordType.TYPE_PHOTO,
                tempRoot,
            )
            verify(retrieveMediaFromMediaStoreUseCase).invoke(
                secondaryFolderPath,
                types.second,
                folderType,
                SyncRecordType.TYPE_VIDEO,
                tempRoot,
            )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("test that correct result is returned")
    inner class ResultSavedInDatabase {

        @ParameterizedTest(name = "when file type list is {0}")
        @MethodSource("mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCaseTest#provideAllMediaStoreFileTypeParameters")
        fun `test that it returns the combination of all the camera uploads record retrieved`(
            mediaStoreFileType: List<MediaStoreFileType>,
        ) =
            runTest {
                val primaryFolderPath = "primaryFolderPath"
                val secondaryFolderPath = "secondaryFolderPath"
                val primaryFolderType = CameraUploadFolderType.Primary
                val secondaryFolderType = CameraUploadFolderType.Secondary
                val photoRecordType = SyncRecordType.TYPE_PHOTO
                val videoRecordType = SyncRecordType.TYPE_VIDEO
                val tempRoot = "tempRoot"
                val types = mediaStoreFileType.partition { it.isImageFileType() }

                val photoPrimaryRecordList =
                    if (types.first.isEmpty()) emptyList()
                    else listOf<CameraUploadsRecord>(mock())
                val videoPrimaryRecordList =
                    if (types.second.isEmpty()) emptyList()
                    else listOf<CameraUploadsRecord>(mock(), mock())
                val photoSecondaryRecordList =
                    if (types.first.isEmpty()) emptyList()
                    else listOf<CameraUploadsRecord>(mock(), mock(), mock())
                val videoSecondaryRecordList =
                    if (types.second.isEmpty()) emptyList()
                    else listOf<CameraUploadsRecord>(mock(), mock(), mock(), mock())


                whenever(getMediaStoreFileTypesUseCase()).thenReturn(mediaStoreFileType)
                whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
                whenever(
                    retrieveMediaFromMediaStoreUseCase(
                        primaryFolderPath,
                        types.first,
                        primaryFolderType,
                        photoRecordType,
                        tempRoot
                    )
                ).thenReturn(photoPrimaryRecordList)
                whenever(
                    retrieveMediaFromMediaStoreUseCase(
                        primaryFolderPath,
                        types.second,
                        primaryFolderType,
                        videoRecordType,
                        tempRoot
                    )
                ).thenReturn(videoPrimaryRecordList)
                whenever(
                    retrieveMediaFromMediaStoreUseCase(
                        secondaryFolderPath,
                        types.first,
                        secondaryFolderType,
                        photoRecordType,
                        tempRoot
                    )
                ).thenReturn(photoSecondaryRecordList)
                whenever(
                    retrieveMediaFromMediaStoreUseCase(
                        secondaryFolderPath,
                        types.second,
                        secondaryFolderType,
                        videoRecordType,
                        tempRoot
                    )
                ).thenReturn(videoSecondaryRecordList)
                whenever(isSecondaryFolderEnabled()).thenReturn(true)
                whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)

                val expected =
                    photoPrimaryRecordList + videoPrimaryRecordList + photoSecondaryRecordList + videoSecondaryRecordList

                underTest.invoke(tempRoot)
                verify(saveCameraUploadsRecordUseCase).invoke(expected)
            }
    }

    companion object {
        @JvmStatic
        fun provideImageMediaStoreFileTypeParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(MediaStoreFileType.IMAGES_INTERNAL)),
            Arguments.of(listOf(MediaStoreFileType.IMAGES_EXTERNAL)),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_INTERNAL,
                    MediaStoreFileType.IMAGES_EXTERNAL
                )
            )
        )

        @JvmStatic
        fun provideVideoMediaStoreFileTypeParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(MediaStoreFileType.VIDEO_INTERNAL)),
            Arguments.of(listOf(MediaStoreFileType.VIDEO_EXTERNAL)),
            Arguments.of(
                listOf(
                    MediaStoreFileType.VIDEO_INTERNAL,
                    MediaStoreFileType.VIDEO_EXTERNAL
                )
            )
        )

        @JvmStatic
        fun provideBothTypesMediaStoreFileTypeParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_INTERNAL,
                    MediaStoreFileType.VIDEO_INTERNAL
                )
            ),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_EXTERNAL,
                    MediaStoreFileType.VIDEO_INTERNAL
                )
            ),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_INTERNAL,
                    MediaStoreFileType.IMAGES_EXTERNAL,
                    MediaStoreFileType.VIDEO_INTERNAL
                )
            ),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_INTERNAL,
                    MediaStoreFileType.VIDEO_EXTERNAL
                )
            ),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_EXTERNAL,
                    MediaStoreFileType.VIDEO_EXTERNAL
                )
            ),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_INTERNAL,
                    MediaStoreFileType.IMAGES_EXTERNAL,
                    MediaStoreFileType.VIDEO_EXTERNAL
                )
            ),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_INTERNAL,
                    MediaStoreFileType.VIDEO_INTERNAL,
                    MediaStoreFileType.VIDEO_EXTERNAL
                )
            ),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_EXTERNAL,
                    MediaStoreFileType.VIDEO_INTERNAL,
                    MediaStoreFileType.VIDEO_EXTERNAL
                )
            ),
            Arguments.of(
                listOf(
                    MediaStoreFileType.IMAGES_EXTERNAL,
                    MediaStoreFileType.VIDEO_INTERNAL,
                    MediaStoreFileType.VIDEO_INTERNAL,
                    MediaStoreFileType.VIDEO_EXTERNAL
                )
            ),
        )

        @JvmStatic
        fun provideAllMediaStoreFileTypeParameters(): Stream<Arguments> =
            Stream.of(
                provideImageMediaStoreFileTypeParameters(),
                provideVideoMediaStoreFileTypeParameters(),
                provideBothTypesMediaStoreFileTypeParameters()
            ).flatMap { stream -> stream }
    }
}


