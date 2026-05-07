package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.continuewhereleftoff.ClearRecentlyUsedItemsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearContinueWhereLeftOffDataLogoutTaskTest {
    private lateinit var underTest: ClearContinueWhereLeftOffDataLogoutTask

    private val clearRecentlyUsedItemsUseCase = mock<ClearRecentlyUsedItemsUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearContinueWhereLeftOffDataLogoutTask(
            clearRecentlyUsedItemsUseCase = clearRecentlyUsedItemsUseCase,
        )
    }

    @Test
    internal fun `test that recently used items are cleared on logout success`() = runTest {
        underTest.onLogoutSuccess()

        verify(clearRecentlyUsedItemsUseCase).invoke()
    }
}
