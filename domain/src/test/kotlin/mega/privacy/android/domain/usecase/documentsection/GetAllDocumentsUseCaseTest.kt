package mega.privacy.android.domain.usecase.documentsection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllDocumentsUseCaseTest {
    private lateinit var underTest: GetAllDocumentsUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetAllDocumentsUseCase()
    }

    @Test
    fun `test that the list of documents is empty`() = runTest {
        assertThat(underTest()).isEmpty()
    }
}