package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateNewImageUriUseCaseTest {
    private lateinit var underTest: CreateNewImageUriUseCase
    private val repository: FileSystemRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = CreateNewImageUriUseCase(repository = repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke returns correct value`() = runTest {
        val fileName = "fileName"
        val uri = "uri"
        whenever(repository.createNewImageUri(fileName)).thenReturn(uri)
        Truth.assertThat(underTest(fileName)).isEqualTo(uri)
    }
}