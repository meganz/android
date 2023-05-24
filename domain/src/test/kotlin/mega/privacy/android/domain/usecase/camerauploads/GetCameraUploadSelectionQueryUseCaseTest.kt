package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCameraUploadSelectionQueryUseCaseTest {

    private lateinit var underTest: GetCameraUploadSelectionQueryUseCase

    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase = mock()
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = GetCameraUploadSelectionQueryUseCase(
            cameraUploadRepository = cameraUploadRepository,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository, getPrimaryFolderPathUseCase, getSecondaryFolderPathUseCase)
    }

    @ParameterizedTest(name = "{4} is expected with sync timestamp {0} and current timestamp is {2} and secondary folder is {3}  ")
    @MethodSource("provideParameters")
    internal fun `test that when invoked `(
        timestampType: SyncTimeStamp,
        localPath: String,
        timeStamp: Long,
        isSecondaryEnabled: Boolean,
        selectionQuery: String?,
    ) =
        runTest {
            whenever(cameraUploadRepository.getSyncTimeStamp(timestampType)).thenReturn(
                timeStamp
            )
            whenever(cameraUploadRepository.isSecondaryMediaFolderEnabled()).thenReturn(
                isSecondaryEnabled
            )
            whenever(getPrimaryFolderPathUseCase()).thenReturn(localPath)
            whenever(getSecondaryFolderPathUseCase()).thenReturn(localPath)
            whenever(
                cameraUploadRepository.getSelectionQuery(
                    timeStamp,
                    localPath
                )
            ).thenReturn(selectionQuery)
            val actual = underTest(timestampType)
            Truth.assertThat(actual).isEqualTo(selectionQuery)
        }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            SyncTimeStamp.PRIMARY_PHOTO,
            "/path/primary",
            1234L,
            false,
            "selection_query"
        ),
        Arguments.of(
            SyncTimeStamp.PRIMARY_VIDEO,
            "/path/primary",
            1234L,
            false,
            "selection_query"
        ),
        Arguments.of(
            SyncTimeStamp.SECONDARY_PHOTO,
            "/path/secondary",
            1234L,
            true,
            "selection_query"
        ),
        Arguments.of(
            SyncTimeStamp.SECONDARY_VIDEO,
            "/path/secondary",
            1234L,
            true,
            "selection_query"
        ),
        Arguments.of(
            SyncTimeStamp.SECONDARY_PHOTO,
            "/path/secondary",
            1234L,
            false,
            null
        ),
        Arguments.of(
            SyncTimeStamp.SECONDARY_VIDEO,
            "/path/secondary",
            1234L,
            false,
            null
        ),
        Arguments.of(
            SyncTimeStamp.SECONDARY_PHOTO,
            "/path/secondary",
            0L,
            true,
            null
        ),
        Arguments.of(
            SyncTimeStamp.SECONDARY_VIDEO,
            "/path/secondary",
            0L,
            true,
            null
        ),
    )
}
