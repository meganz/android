package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsPrimaryFolderPathValidUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsPrimaryFolderPathValidUseCaseTest {

    private lateinit var underTest: IsPrimaryFolderPathValidUseCase

    private val isFolderPathExistingUseCase = mock<IsFolderPathExistingUseCase>()
    private val isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase =
        mock<IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsPrimaryFolderPathValidUseCase(
            isFolderPathExistingUseCase = isFolderPathExistingUseCase,
            isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase = isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(isFolderPathExistingUseCase, isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase)
    }

    @Test
    fun `test that the primary folder is invalid if its path is null`() = runTest {
        assertThat(underTest(null)).isFalse()
    }

    @Test
    fun `test that the primary folder is invalid if the file system cannot find it`() = runTest {
        val folderPath = "primary/folder/path"

        whenever(isFolderPathExistingUseCase(folderPath)).thenReturn(false)

        assertThat(underTest(folderPath)).isFalse()
    }

    @Test
    fun `test that the primary folder is invalid if it exists in the local file system but is related to the secondary folder`() =
        runTest {
            val folderPath = "primary/folder/path"

            whenever(isFolderPathExistingUseCase(folderPath)).thenReturn(true)
            whenever(isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(folderPath)).thenReturn(
                false
            )

            assertThat(underTest(folderPath)).isFalse()
        }

    @Test
    fun `test that the primary folder is valid if it exists in the local file system and is unrelated to the secondary folder`() =
        runTest {
            val folderPath = "primary/folder/path"

            whenever(isFolderPathExistingUseCase(folderPath)).thenReturn(true)
            whenever(isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(folderPath)).thenReturn(
                true
            )

            assertThat(underTest(folderPath)).isTrue()
        }
}
