package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.viewedlinks.ClearViewedLinksUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearViewedLinksLogoutTaskTest {
    private lateinit var underTest: ClearViewedLinksLogoutTask

    private val clearViewedLinksUseCase = mock<ClearViewedLinksUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearViewedLinksLogoutTask(
            clearViewedLinksUseCase = clearViewedLinksUseCase,
        )
    }

    @Test
    internal fun `test that viewed links are cleared on logout success`() = runTest {
        underTest.onLogoutSuccess()

        verify(clearViewedLinksUseCase).invoke()
    }
}
