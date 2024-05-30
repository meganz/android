package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPendingCameraUploadsRecordsUseCaseTest {
    private lateinit var underTest: GetPendingCameraUploadsRecordsUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val getUploadOptionUseCase = mock<GetUploadOptionUseCase>()
    private val isMediaUploadsEnabledUseCase = mock<IsMediaUploadsEnabledUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetPendingCameraUploadsRecordsUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            getUploadOptionUseCase = getUploadOptionUseCase,
            isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            cameraUploadsRepository,
            getUploadOptionUseCase,
            isMediaUploadsEnabledUseCase,
        )
    }

    @ParameterizedTest(name = "when uploadOption is {0} and isSecondaryFolderEnabled is {1}")
    @MethodSource("provideParameters")
    fun `test that the use case is invoked with correct upload status list`(
        uploadOption: UploadOption,
        isSecondaryFolderEnabled: Boolean,
    ) = runTest {
        whenever(getUploadOptionUseCase()).thenReturn(uploadOption)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(isSecondaryFolderEnabled)

        val expectedTypes = when (uploadOption) {
            UploadOption.PHOTOS -> listOf(CameraUploadsRecordType.TYPE_PHOTO)
            UploadOption.VIDEOS -> listOf(CameraUploadsRecordType.TYPE_VIDEO)
            UploadOption.PHOTOS_AND_VIDEOS ->
                listOf(CameraUploadsRecordType.TYPE_PHOTO, CameraUploadsRecordType.TYPE_VIDEO)
        }

        val expectedFolderTypes =
            if (isSecondaryFolderEnabled)
                listOf(CameraUploadFolderType.Primary, CameraUploadFolderType.Secondary)
            else listOf(CameraUploadFolderType.Primary)

        val expectedUploadStatus = listOf(
            CameraUploadsRecordUploadStatus.PENDING,
            CameraUploadsRecordUploadStatus.STARTED,
            CameraUploadsRecordUploadStatus.FAILED
        )

        underTest()

        verify(cameraUploadsRepository)
            .getCameraUploadsRecordsBy(
                expectedUploadStatus,
                expectedTypes,
                expectedFolderTypes
            )
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(UploadOption.PHOTOS, false),
        Arguments.of(UploadOption.PHOTOS, true),
        Arguments.of(UploadOption.VIDEOS, false),
        Arguments.of(UploadOption.VIDEOS, true),
        Arguments.of(UploadOption.PHOTOS_AND_VIDEOS, false),
        Arguments.of(UploadOption.PHOTOS_AND_VIDEOS, true),
    )
}
