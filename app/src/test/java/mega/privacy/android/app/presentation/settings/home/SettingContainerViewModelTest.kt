package mega.privacy.android.app.presentation.settings.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.settings.home.mapper.SectionHeaderMapper
import mega.privacy.android.app.presentation.settings.home.mapper.SettingItemFlowMapper
import mega.privacy.android.app.presentation.settings.home.mapper.SettingItemMapper
import mega.privacy.android.app.presentation.settings.home.model.SettingsUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.navigation.settings.FeatureSettings
import mega.privacy.android.navigation.settings.SettingClickActionType
import mega.privacy.android.navigation.settings.SettingEntryPoint
import mega.privacy.android.navigation.settings.SettingItem
import mega.privacy.android.navigation.settings.SettingSectionHeader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock

@ExtendWith(CoroutineMainDispatcherExtension::class)
class SettingContainerViewModelTest {
    private lateinit var underTest: SettingContainerViewModel

    private val settingItemMapper = SettingItemMapper()
    private val settingItemFlowMapper = SettingItemFlowMapper()
    private val sectionHeaderMapper = SectionHeaderMapper()

    private fun initUnderTest(settings: Set<FeatureSettings>, scope: CoroutineScope) {
        underTest = SettingContainerViewModel(
            featureSettings = settings,
            settingItemMapper = settingItemMapper,
            settingItemFlowMapper = settingItemFlowMapper,
            sectionHeaderMapper = sectionHeaderMapper,
            functionScope = scope,
        )
    }


    @Test
    fun `test that an empty set of settings returns loading state`() = runTest {
        initUnderTest(emptySet(), this)
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(SettingsUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that multiple items with the same header are grouped together`() = runTest {
        val section1 = SettingSectionHeader.Storage
        val section2 = SettingSectionHeader.Features

        val itemCount = 5
        val set1 = createFeatureSettingsSet(itemCount, section1)
        val set2 = createFeatureSettingsSet(itemCount, section2)

        val input = set1 + set2

        initUnderTest(input, this)

        underTest.state.filterIsInstance<SettingsUiState.Data>().test {
            val actual = awaitItem().settings
            assertThat(actual.size).isEqualTo(2)
            assertThat(actual.all { it.sectionItems.size == itemCount }).isTrue()
        }

    }

    @Test
    fun `test that function actions are called`() = runTest {
        var actioned = false
        val functionAction = suspend { actioned = true }
        val input = createFeatureSettingsSet(
            1, SettingSectionHeader.Help, SettingClickActionType.FunctionAction(
                functionAction
            )
        )

        initUnderTest(input, this)
        underTest.state.filterIsInstance<SettingsUiState.Data>().test {
            awaitItem().settings.first().sectionItems.first().onClick(mock())
        }

        testScheduler.advanceUntilIdle()
        assertThat(actioned).isTrue()
    }

    @Test
    fun `test that exceptions from function actions are handled`() = runTest {
        val functionAction = suspend { throw Throwable("Bad things happened") }
        val input = createFeatureSettingsSet(
            1, SettingSectionHeader.Help, SettingClickActionType.FunctionAction(
                functionAction
            )
        )

        initUnderTest(input, this)
        underTest.state.filterIsInstance<SettingsUiState.Data>().test {
            awaitItem().settings.first().sectionItems.first().onClick(mock())
        }

        testScheduler.advanceUntilIdle()
    }


    private fun createFeatureSettingsSet(
        itemCount: Int,
        section: SettingSectionHeader,
        action: SettingClickActionType = SettingClickActionType.NavigationAction(
            Unit
        ),
    ) = (0 until itemCount).map {
        createFeatureSettingsEntry(
            section = section,
            key = section.toString() + it,
            action = action
        )
    }.toSet()

    private fun createFeatureSettingsEntry(
        section: SettingSectionHeader,
        key: String,
        action: SettingClickActionType,
    ) = FeatureSettings(
        settingsNavGraph = { _, _ -> },
        entryPoints = listOf(
            SettingEntryPoint(
                section = section,
                items = listOf(
                    SettingItem(
                        key = key,
                        name = "",
                        descriptionValue = null,
                        isEnabled = null,
                        clickAction = action,
                        isDestructive = false
                    )
                )
            )
        )
    )
}