package mega.privacy.android.app.presentation.settings.home.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.settings.home.model.SettingHeaderItem
import mega.privacy.android.navigation.settings.SettingSectionHeader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingHeaderComparatorTest {
    private val underTest = SettingHeaderComparator()

    @Test
    fun `test that known sections are mapped correctly`() {
        val customHeader = settingHeaderItemOf(SettingSectionHeader.Custom("test"))
        val input = listOf(
            settingHeaderItemOf(SettingSectionHeader.About),
            settingHeaderItemOf(SettingSectionHeader.Appearance),
            customHeader,
            settingHeaderItemOf(SettingSectionHeader.Features),
            settingHeaderItemOf(SettingSectionHeader.Help),
            settingHeaderItemOf(SettingSectionHeader.Media),
            settingHeaderItemOf(SettingSectionHeader.Security),
            settingHeaderItemOf(SettingSectionHeader.Storage),
            settingHeaderItemOf(SettingSectionHeader.UserInterface),
        )

        val expected = listOf(
            settingHeaderItemOf(SettingSectionHeader.Appearance),
            settingHeaderItemOf(SettingSectionHeader.Features),
            settingHeaderItemOf(SettingSectionHeader.Storage),
            settingHeaderItemOf(SettingSectionHeader.UserInterface),
            settingHeaderItemOf(SettingSectionHeader.Media),
            settingHeaderItemOf(SettingSectionHeader.Security),
            settingHeaderItemOf(SettingSectionHeader.Help),
            settingHeaderItemOf(SettingSectionHeader.About),
            customHeader,
        )

        assertThat(input.sortedWith(underTest)).isEqualTo(expected)
    }

    @Test
    fun `test that custom sections are sorted alphabetically`() {
        val input = listOf(
            settingHeaderItemOf(SettingSectionHeader.Custom("G")),
            settingHeaderItemOf(SettingSectionHeader.Custom("E")),
            settingHeaderItemOf(SettingSectionHeader.Custom("B")),
            settingHeaderItemOf(SettingSectionHeader.Custom("F")),
            settingHeaderItemOf(SettingSectionHeader.Custom("A")),
            settingHeaderItemOf(SettingSectionHeader.Custom("C")),
            settingHeaderItemOf(SettingSectionHeader.Custom("D")),
        )

        val expected = listOf(
            settingHeaderItemOf(SettingSectionHeader.Custom("A")),
            settingHeaderItemOf(SettingSectionHeader.Custom("B")),
            settingHeaderItemOf(SettingSectionHeader.Custom("C")),
            settingHeaderItemOf(SettingSectionHeader.Custom("D")),
            settingHeaderItemOf(SettingSectionHeader.Custom("E")),
            settingHeaderItemOf(SettingSectionHeader.Custom("F")),
            settingHeaderItemOf(SettingSectionHeader.Custom("G")),
        )

        assertThat(input.sortedWith(underTest)).isEqualTo(expected)
    }

    private fun settingHeaderItemOf(section: SettingSectionHeader) =
        SettingHeaderItem({ "" }, section.toString())
}