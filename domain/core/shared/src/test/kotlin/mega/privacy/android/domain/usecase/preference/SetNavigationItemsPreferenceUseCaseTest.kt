package mega.privacy.android.domain.usecase.preference

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetNavigationItemsPreferenceUseCaseTest {
    private lateinit var underTest: SetNavigationItemsPreferenceUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetNavigationItemsPreferenceUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that invoke calls repository with the preference`() = runTest {
        val preference = NavigationItemsPreference(
            orderedVisibleItemIds = listOf("home", "photos", "chat"),
        )

        underTest(preference)

        verify(settingsRepository).setNavigationItemsPreference(preference)
    }
}
