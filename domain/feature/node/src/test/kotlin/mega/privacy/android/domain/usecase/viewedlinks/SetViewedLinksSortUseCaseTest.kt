package mega.privacy.android.domain.usecase.viewedlinks

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.domain.repository.ViewedLinksRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetViewedLinksSortUseCaseTest {

    private val repository: ViewedLinksRepository = mock()
    private lateinit var underTest: SetViewedLinksSortUseCase

    @BeforeEach
    fun setUp() {
        reset(repository)
        underTest = SetViewedLinksSortUseCase(repository)
    }

    @Test
    fun `test that invoke delegates to repository`() = runTest {
        underTest(ViewedLinksSortField.Name, SortDirection.Descending)

        verify(repository).setSortPreference(
            ViewedLinksSortField.Name,
            SortDirection.Descending,
        )
    }
}
