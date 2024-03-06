package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
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
 * Test class for [IsPrimaryFolderNodeValidUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsPrimaryFolderNodeValidUseCaseTest {

    private lateinit var underTest: IsPrimaryFolderNodeValidUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()

    @BeforeAll
    fun setUp() {
        underTest = IsPrimaryFolderNodeValidUseCase(
            cameraUploadRepository = cameraUploadRepository,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository, getSecondarySyncHandleUseCase, isSecondaryFolderEnabled)
    }

    @Test
    fun `test that the null primary folder node is invalid`() = runTest {
        assertThat(underTest(null)).isFalse()
    }

    @ParameterizedTest(name = "when node handle is {0}, then is primary folder node valid is {1}")
    @MethodSource("provideSecondaryFolderDisabledParams")
    fun `test that the non null primary folder node is valid or not when secondary folder uploads are disabled`(
        nodeHandle: Long,
        isPrimaryFolderNodeValid: Boolean,
    ) = runTest {
        whenever(isSecondaryFolderEnabled()).thenReturn(false)
        whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)

        assertThat(underTest(nodeHandle)).isEqualTo(isPrimaryFolderNodeValid)
    }

    private fun provideSecondaryFolderDisabledParams() = Stream.of(
        Arguments.of(123456L, true),
        Arguments.of(-1L, false),
    )

    @ParameterizedTest(name = "when primary folder node handle is {0} and secondary folder node handle is {1}, then is primary folder node valid is {2}")
    @MethodSource("provideSecondaryFolderEnabledParams")
    fun `test that the non null primary folder node is valid or not when secondary folder uploads are enabled`(
        primaryFolderNodeHandle: Long,
        secondaryFolderNodeHandle: Long,
        isPrimaryFolderNodeValid: Boolean,
    ) = runTest {
        whenever(isSecondaryFolderEnabled()).thenReturn(true)
        whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(getSecondarySyncHandleUseCase()).thenReturn(secondaryFolderNodeHandle)

        assertThat(underTest(primaryFolderNodeHandle)).isEqualTo(isPrimaryFolderNodeValid)
    }

    private fun provideSecondaryFolderEnabledParams() = Stream.of(
        Arguments.of(123456L, 123456L, false),
        Arguments.of(123456L, -1L, true),
        Arguments.of(-1L, 123456L, false),
        Arguments.of(-1L, -1L, false),
    )
}