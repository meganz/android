package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsExternalStorageContentUriUseCaseTest {
    private lateinit var underTest: IsExternalStorageContentUriUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsExternalStorageContentUriUseCase(fileSystemRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the use case returns repository result method`(expected: Boolean) =
        runTest {
            whenever(fileSystemRepository.isExternalStorageContentUri(any())).thenReturn(expected)

            val actual = underTest("someUri")

            Truth.assertThat(actual).isEqualTo(expected)
        }
}