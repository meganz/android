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
 * Test class for [IsSecondaryFolderPathValidUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsSecondaryFolderPathValidUseCaseTest {

    private lateinit var underTest: IsSecondaryFolderPathValidUseCase

    private val isFolderPathExistingUseCase = mock<IsFolderPathExistingUseCase>()
    private val isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase =
        mock<IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsSecondaryFolderPathValidUseCase(
            isFolderPathExistingUseCase = isFolderPathExistingUseCase,
            isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase = isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(isFolderPathExistingUseCase, isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase)
    }

    @Test
    fun `test that the secondary folder is invalid if its path is null`() = runTest {
        assertThat(underTest(null)).isFalse()
    }

    @Test
    fun `test that the secondary folder is invalid if the file system cannot find it`() = runTest {
        val folderPath = "secondary/folder/path"

        whenever(isFolderPathExistingUseCase(folderPath)).thenReturn(false)

        assertThat(underTest(folderPath)).isFalse()
    }

    @Test
    fun `test that the secondary folder is invalid if it exists in the local file system but is related to the primary folder`() =
        runTest {
            val folderPath = "secondary/folder/path"

            whenever(isFolderPathExistingUseCase(folderPath)).thenReturn(true)
            whenever(isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(folderPath)).thenReturn(
                false
            )

            assertThat(underTest(folderPath)).isFalse()
        }

    @Test
    fun `test that the secondary folder is valid if it exists in the local file system and is unrelated to the primary folder`() =
        runTest {
            val folderPath = "secondary/folder/path"

            whenever(isFolderPathExistingUseCase(folderPath)).thenReturn(true)
            whenever(isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(folderPath)).thenReturn(
                true
            )

            assertThat(underTest(folderPath)).isTrue()
        }
}
