package mega.privacy.android.app.presentation.settings.customisenavigation

import mega.privacy.android.app.presentation.settings.model.customiseNavigationTargetPreference
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CustomiseNavigationNavKey
import mega.privacy.android.navigation.destination.LegacySettingsNavKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomiseNavigationScreenDestinationTest {

    private val navigationHandler = mock<NavigationHandler>()

    @BeforeEach
    fun resetMocks() {
        reset(navigationHandler)
    }

    @Test
    fun `test that exitCustomiseNavigation navigates to legacy settings when returnToSettings is true`() {
        val key = CustomiseNavigationNavKey(returnToSettings = true)

        navigationHandler.exitCustomiseNavigation(key)

        verify(navigationHandler).navigateAndClearTo(
            destination = LegacySettingsNavKey(customiseNavigationTargetPreference),
            newParent = key,
            inclusive = true,
        )
        verifyNoMoreInteractions(navigationHandler)
    }

    @Test
    fun `test that exitCustomiseNavigation navigates back when returnToSettings is false`() {
        navigationHandler.exitCustomiseNavigation(CustomiseNavigationNavKey())

        verify(navigationHandler).back()
        verifyNoMoreInteractions(navigationHandler)
    }
}
