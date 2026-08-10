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
class RemoveRecentlyUsedItemUseCaseTest {

    private lateinit var underTest: RemoveRecentlyUsedItemUseCase

    private val repository = mock<ContinueWhereLeftOffRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RemoveRecentlyUsedItemUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke calls repository with correct nodeHandle`() = runTest {
        underTest(123L)

        verify(repository).removeRecentlyUsedItem(123L)
    }
}
