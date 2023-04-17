package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsNewPrimaryFolderPathValidUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsNewPrimaryFolderPathValidUseCaseTest {

    private lateinit var underTest: IsNewPrimaryFolderPathValidUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsNewPrimaryFolderPathValidUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @Test
    fun `test that the new primary folder path is invalid if it is null`() = runTest {
        assertThat(underTest(null)).isFalse()
    }

    @TestFactory
    fun `test that the validity of the primary folder path is determined`() =
        listOf(
            "test/path" to "test/path2",
            "test/path" to "",
            "test/path2" to "test/path2",
            "" to "test/path2",
            "" to ""
        ).map { (newPrimaryFolderPath, secondaryFolderLocalPath) ->
            dynamicTest("test that primary path $newPrimaryFolderPath is valid when secondary path is $secondaryFolderLocalPath") {
                runTest {
                    whenever(cameraUploadRepository.getSecondaryFolderLocalPath()).thenReturn(
                        secondaryFolderLocalPath
                    )
                    val isValid =
                        newPrimaryFolderPath.isNotBlank() && newPrimaryFolderPath != secondaryFolderLocalPath

                    assertThat(underTest(newPrimaryFolderPath)).isEqualTo(isValid)
                }
            }
        }
}