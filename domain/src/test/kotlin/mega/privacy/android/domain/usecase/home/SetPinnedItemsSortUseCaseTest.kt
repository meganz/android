package mega.privacy.android.domain.usecase.home

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.home.PinnedHomeItemsSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetPinnedItemsSortUseCaseTest {

    private val settingsRepository: SettingsRepository = mock()
    private lateinit var underTest: SetPinnedItemsSortUseCase

    @BeforeEach
    fun setUp() {
        reset(settingsRepository)
        underTest = SetPinnedItemsSortUseCase(settingsRepository)
    }

    @Test
    fun `test that invoke delegates to repository`() = runTest {
        underTest(PinnedHomeItemsSortField.Name, SortDirection.Descending)

        verify(settingsRepository).setPinnedItemsSortPreference(
            PinnedHomeItemsSortField.Name,
            SortDirection.Descending,
        )
    }
}
