package mega.privacy.android.domain.usecase.transfers

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileNameFromStringUriUseCaseTest {

    private lateinit var underTest: GetFileNameFromStringUriUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetFileNameFromStringUriUseCase(fileSystemRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Name"])
    @NullSource
    fun `test that use case invokes and returns correctly`(
        fileName: String?,
    ) = runTest {
        val uriOrPathString = "content://com.android.providers.downloads.documents/document/1234"

        whenever(fileSystemRepository.getFileNameFromUri(uriOrPathString)).thenReturn(fileName)

        Truth.assertThat(underTest(uriOrPathString)).isEqualTo(fileName)
    }
}