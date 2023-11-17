package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import org.junit.jupiter.api.Assertions.*
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

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getUploadOptionUseCase = mock<GetUploadOptionUseCase>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()

    @BeforeAll
    fun setUp() {
        underTest = GetPendingCameraUploadsRecordsUseCase(
            cameraUploadRepository = cameraUploadRepository,
            getUploadOptionUseCase = getUploadOptionUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            cameraUploadRepository,
            getUploadOptionUseCase,
            isSecondaryFolderEnabled,
        )
    }

    @ParameterizedTest(name = "when uploadOption is {0} and isSecondaryFolderEnabled is {1}")
    @MethodSource("provideParameters")
    fun `test that the use case is invoked with correct upload status list`(
        uploadOption: UploadOption,
        isSecondaryFolderEnabled: Boolean,
    ) = runTest {
        whenever(getUploadOptionUseCase()).thenReturn(uploadOption)
        whenever(isSecondaryFolderEnabled()).thenReturn(isSecondaryFolderEnabled)

        val expectedTypes = when (uploadOption) {
            UploadOption.PHOTOS -> listOf(SyncRecordType.TYPE_PHOTO)
            UploadOption.VIDEOS -> listOf(SyncRecordType.TYPE_VIDEO)
            UploadOption.PHOTOS_AND_VIDEOS ->
                listOf(SyncRecordType.TYPE_PHOTO, SyncRecordType.TYPE_VIDEO)
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

        verify(cameraUploadRepository)
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
