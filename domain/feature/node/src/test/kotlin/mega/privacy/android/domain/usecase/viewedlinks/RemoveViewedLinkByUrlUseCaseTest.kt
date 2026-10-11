package mega.privacy.android.domain.usecase.viewedlinks

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ViewedLinksRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoveViewedLinkByUrlUseCaseTest {

    private val repository: ViewedLinksRepository = mock()
    private lateinit var underTest: RemoveViewedLinkByUrlUseCase

    @BeforeEach
    fun setUp() {
        reset(repository)
        underTest = RemoveViewedLinkByUrlUseCase(repository)
    }

    @Test
    fun `test that invoke delegates to repository removeLinkByUrl`() = runTest {
        val url = "https://mega.nz/file/abc"

        underTest(url)

        verify(repository).removeLinkByUrl(url)
    }
}
