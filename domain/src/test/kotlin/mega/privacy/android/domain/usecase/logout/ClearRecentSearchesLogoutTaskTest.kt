package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.search.ClearRecentSearchesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearRecentSearchesLogoutTaskTest {
    private lateinit var underTest: ClearRecentSearchesLogoutTask

    private val clearRecentSearchesUseCase = mock<ClearRecentSearchesUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearRecentSearchesLogoutTask(
            clearRecentSearchesUseCase = clearRecentSearchesUseCase,
        )
    }

    @Test
    internal fun `test that recent searches are cleared on logout success`() = runTest {
        underTest.onLogoutSuccess()

        verify(clearRecentSearchesUseCase).invoke()
    }
}
