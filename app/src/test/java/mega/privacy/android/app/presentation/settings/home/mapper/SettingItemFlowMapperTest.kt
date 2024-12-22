package mega.privacy.android.app.presentation.settings.home.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.settings.home.model.SettingModelItem
import mega.privacy.android.navigation.settings.SettingClickActionType
import mega.privacy.android.navigation.settings.SettingDescriptionValue
import mega.privacy.android.navigation.settings.SettingItem
import mega.privacy.android.navigation.settings.SettingSectionHeader
import org.junit.jupiter.api.Test

class SettingItemFlowMapperTest {
    private val underTest = SettingItemFlowMapper()

    @Test
    fun `test that items with static values return null`() {
        val actual = underTest(
            SettingItem(
                key = "",
                name = "",
                descriptionValue = null,
                isEnabled = null,
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = false
            )
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that items with isEnabled value updates the boolean value`() = runTest {
        val key = "board"
        val expectedEnabledState = true
        val actual = underTest(
            SettingItem(
                key = key,
                name = "",
                descriptionValue = null,
                isEnabled = flowOf(expectedEnabledState),
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = false
            )
        )

        val item = getModel(key)

        actual?.collect {
            val updated = it.invoke(listOf(item)).first()
            assertThat(updated.isEnabled?.invoke()).isEqualTo(expectedEnabledState)
        }
    }

    @Test
    fun `test that items with dynamic description updates the description`() = runTest {
        val key = "key"
        val expectedDescription = "expectedDescription"
        val actual = underTest(
            SettingItem(
                key = key,
                name = "",
                descriptionValue = SettingDescriptionValue.DynamicDescription(
                    flowOf(
                        expectedDescription
                    )
                ),
                isEnabled = null,
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = false
            )
        )

        val item = getModel(key)

        actual?.collect {
            val updated = it.invoke(listOf(item)).first()
            assertThat(updated.description).isEqualTo(expectedDescription)
        }
    }

    @Test
    fun `test that an item with both only gets the enabled update`() = runTest {
        //None of our use cases support a setting where both values are dynamic,
        // this tests is added to document that and avoid confusion regarding the implementation

        val key = "skeleton"
        val expectedEnabledState = true
        val notExpectedDescription = "expectedDescription"

        val actual = underTest(
            SettingItem(
                key = key,
                name = "",
                descriptionValue = SettingDescriptionValue.DynamicDescription(
                    flowOf(
                        notExpectedDescription
                    )
                ),
                isEnabled = flowOf(expectedEnabledState),
                clickAction = SettingClickActionType.NavigationAction(Unit),
                isDestructive = false
            )
        )

        val item = getModel(key)

        actual?.collect {
            val updated = it.invoke(listOf(item)).first()
            assertThat(updated.isEnabled?.invoke()).isEqualTo(expectedEnabledState)
            assertThat(updated.description).isNotEqualTo(notExpectedDescription)
        }
    }

    private fun getModel(key: String) = SettingModelItem(
        section = SettingSectionHeader.Help,
        key = key,
        name = "",
        description = null,
        isEnabled = null,
        isDestructive = false,
    )
}