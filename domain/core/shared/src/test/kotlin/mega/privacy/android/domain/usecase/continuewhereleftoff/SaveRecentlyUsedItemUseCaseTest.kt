package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveRecentlyUsedItemUseCaseTest {

    private lateinit var underTest: SaveRecentlyUsedItemUseCase

    private val repository = mock<ContinueWhereLeftOffRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SaveRecentlyUsedItemUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke calls repository with correct parameters`() = runTest {
        underTest(1L, RecentlyUsedType.PDF, "test.pdf")

        verify(repository).saveRecentlyUsedItem(1L, RecentlyUsedType.PDF, "test.pdf")
    }
}
