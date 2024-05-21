package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [IsSecondaryFolderNodeValidUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsSecondaryFolderNodeValidUseCaseTest {

    private lateinit var underTest: IsSecondaryFolderNodeValidUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsSecondaryFolderNodeValidUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository, getPrimarySyncHandleUseCase)
    }

    @Test
    fun `test that the null secondary folder node is invalid`() = runTest {
        assertThat(underTest(null)).isFalse()
    }

    @ParameterizedTest(name = "when secondary folder node handle is {0} and primary folder node handle is {1}, then is secondary folder node valid is {2}")
    @MethodSource("provideSecondaryFolderNodeParams")
    fun `test that the non null secondary folder node is valid or not`(
        secondaryFolderNodeHandle: Long,
        primaryFolderNodeHandle: Long,
        isSecondaryFolderNodeValid: Boolean,
    ) = runTest {
        whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(getPrimarySyncHandleUseCase()).thenReturn(primaryFolderNodeHandle)

        assertThat(underTest(secondaryFolderNodeHandle)).isEqualTo(isSecondaryFolderNodeValid)
    }

    private fun provideSecondaryFolderNodeParams() = Stream.of(
        Arguments.of(123456L, 123456L, false),
        Arguments.of(123456L, -1L, true),
        Arguments.of(-1L, 123456L, false),
        Arguments.of(-1L, -1L, false),
    )
}