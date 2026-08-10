package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetContinueWhereLeftOffSortUseCaseTest {

    private lateinit var underTest: SetContinueWhereLeftOffSortUseCase

    private val repository = mock<ContinueWhereLeftOffRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetContinueWhereLeftOffSortUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke delegates to repository setSortPreference`() = runTest {
        underTest(
            sortField = ContinueWhereLeftOffSortField.Name,
            sortDirection = SortDirection.Ascending,
        )

        verify(repository).setSortPreference(
            ContinueWhereLeftOffSortField.Name,
            SortDirection.Ascending,
        )
    }

    @Test
    fun `test that invoke forwards timestamp descending to repository`() = runTest {
        underTest(
            sortField = ContinueWhereLeftOffSortField.Timestamp,
            sortDirection = SortDirection.Descending,
        )

        verify(repository).setSortPreference(
            ContinueWhereLeftOffSortField.Timestamp,
            SortDirection.Descending,
        )
    }
}
