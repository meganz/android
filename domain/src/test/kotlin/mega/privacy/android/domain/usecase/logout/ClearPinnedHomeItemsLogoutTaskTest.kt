package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.home.ClearPinnedHomeItemsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearPinnedHomeItemsLogoutTaskTest {
    private lateinit var underTest: ClearPinnedHomeItemsLogoutTask

    private val clearPinnedHomeItemsUseCase = mock<ClearPinnedHomeItemsUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = ClearPinnedHomeItemsLogoutTask(
            clearPinnedHomeItemsUseCase = clearPinnedHomeItemsUseCase,
        )
    }

    @Test
    fun `test that pinned home items are cleared on logout success`() = runTest {
        underTest.onLogoutSuccess()

        verify(clearPinnedHomeItemsUseCase).invoke()
    }
}
