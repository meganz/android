package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateNewVideoUriUseCaseTest {
    private lateinit var underTest: CreateNewVideoUriUseCase
    private val repository: FileSystemRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = CreateNewVideoUriUseCase(repository = repository)
    }

    @Test
    fun `test that invoke returns correct value`() = runTest {
        val fileName = "fileName"
        val uri = "uri"
        whenever(repository.createNewVideoUri(fileName)).thenReturn(uri)
        Truth.assertThat(underTest(fileName)).isEqualTo(uri)
    }
}