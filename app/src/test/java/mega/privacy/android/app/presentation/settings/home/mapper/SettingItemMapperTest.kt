package mega.privacy.android.app.presentation.settings.home.mapper

import androidx.navigation.NavHostController
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.navigation.settings.SettingClickActionType
import mega.privacy.android.navigation.settings.SettingDescriptionValue
import mega.privacy.android.navigation.settings.SettingItem
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class SettingItemMapperTest {
    private val underTest = SettingItemMapper()

    private object NavTarget

    @Test
    fun `test that static values are set`() {
        val expectedKey = "Key"
        val expectedName = "name"
        val expectedIsDestructive = true
        val expectedDescription = "description"

        val actual = underTest(
            item = SettingItem(
                key = expectedKey,
                name = expectedName,
                descriptionValue = SettingDescriptionValue.StaticDescription(expectedDescription),
                isEnabled = null,
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = expectedIsDestructive
            ),
            suspendHandler = {}
        )

        assertThat(actual.key).isEqualTo(expectedKey)
        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.isDestructive).isEqualTo(expectedIsDestructive)
    }

    @Test
    fun `test that dynamic description is set to empty string`() {
        val actual = underTest(
            item = SettingItem(
                key = "expectedKey",
                name = "expectedName",
                descriptionValue = SettingDescriptionValue.DynamicDescription(flowOf("This comes later")),
                isEnabled = null,
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = false
            ),
            suspendHandler = {}
        )

        assertThat(actual.description).isEqualTo("")
    }

    @Test
    fun `test that dynamic enabled state returns a non null function that returns null`() {
        val actual = underTest(
            item = SettingItem(
                key = "expectedKey",
                name = "expectedName",
                descriptionValue = null,
                isEnabled = flowOf(true),
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = false
            ),
            suspendHandler = {}
        )

        assertThat(actual.isEnabled?.invoke()).isEqualTo(null)
    }

    @Test
    fun `test that navigation actions are mapped to calls on the navigation host`() {
        val actual = underTest(
            item = SettingItem(
                key = "expectedKey",
                name = "expectedName",
                descriptionValue = SettingDescriptionValue.StaticDescription("expectedDescription"),
                isEnabled = null,
                clickAction = SettingClickActionType.NavigationAction(NavTarget),
                isDestructive = false
            ),
            suspendHandler = {}
        )

        val navHostController = mock<NavHostController?>()

        actual.onClick(navHostController)

        verify(navHostController).navigate(NavTarget)
    }

    @Test
    fun `test that suspend click actions are mapped to calls in suspend wrapper`() = runTest {
        var actioned = false
        val functionAction = suspend { actioned = true }
        val wrapper: (suspend () -> Unit) -> Unit =
            { func: suspend () -> Unit -> this.launch { func() } }

        val actual = underTest(
            item = SettingItem(
                key = "expectedKey",
                name = "expectedName",
                descriptionValue = SettingDescriptionValue.StaticDescription("expectedDescription"),
                isEnabled = null,
                clickAction = SettingClickActionType.FunctionAction(functionAction),
                isDestructive = false
            ),
            suspendHandler = wrapper
        )

        actual.onClick(mock())
        testScheduler.advanceUntilIdle()

        assertThat(actioned).isTrue()
    }
}