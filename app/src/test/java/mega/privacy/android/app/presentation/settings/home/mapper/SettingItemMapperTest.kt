package mega.privacy.android.app.presentation.settings.home.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.navigation.settings.SettingClickActionType
import mega.privacy.android.navigation.settings.SettingDescriptionValue
import mega.privacy.android.navigation.settings.SettingItem
import mega.privacy.android.navigation.settings.SettingSectionHeader
import org.junit.jupiter.api.Test

class SettingItemMapperTest {
    private val underTest = SettingItemMapper()

    @Test
    fun `test that static values are set`() {
        val expectedSection = SettingSectionHeader.About
        val expectedKey = "Key"
        val expectedName = "name"
        val expectedIsDestructive = true
        val expectedDescription = "description"

        val actual = underTest(
            section = expectedSection,
            item = SettingItem(
                key = expectedKey,
                name = expectedName,
                descriptionValue = SettingDescriptionValue.StaticDescription(expectedDescription),
                isEnabled = null,
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = expectedIsDestructive
            )
        )

        assertThat(actual.section).isEqualTo(expectedSection)
        assertThat(actual.key).isEqualTo(expectedKey)
        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.isDestructive).isEqualTo(expectedIsDestructive)
    }

    @Test
    fun `test that dynamic description is set to empty string`() {
        val actual = underTest(
            section = SettingSectionHeader.Media,
            item = SettingItem(
                key = "expectedKey",
                name = "expectedName",
                descriptionValue = SettingDescriptionValue.DynamicDescription(flowOf("This comes later")),
                isEnabled = null,
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = false
            )
        )

        assertThat(actual.description).isEqualTo("")
    }

    @Test
    fun `test that dynamic enabled state returns a non null function that returns null`() {
        val actual = underTest(
            section = SettingSectionHeader.Media,
            item = SettingItem(
                key = "expectedKey",
                name = "expectedName",
                descriptionValue = null,
                isEnabled = flowOf(true),
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = false
            )
        )

        assertThat(actual.isEnabled?.invoke()).isEqualTo(null)
    }
}