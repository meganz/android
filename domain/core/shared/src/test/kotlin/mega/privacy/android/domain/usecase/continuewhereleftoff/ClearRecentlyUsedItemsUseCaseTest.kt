package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearRecentlyUsedItemsUseCaseTest {

    private lateinit var underTest: ClearRecentlyUsedItemsUseCase

    private val repository = mock<ContinueWhereLeftOffRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearRecentlyUsedItemsUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke calls repository clearAllRecentlyUsedItems`() = runTest {
        underTest()

        verify(repository).clearAllRecentlyUsedItems()
    }
}
