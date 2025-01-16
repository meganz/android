package mega.privacy.android.app.presentation.settings.compose.home

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.settings.compose.home.mapper.SectionHeaderMapper
import mega.privacy.android.app.presentation.settings.compose.home.mapper.SettingHeaderComparator
import mega.privacy.android.app.presentation.settings.compose.home.mapper.SettingItemFlowMapper
import mega.privacy.android.app.presentation.settings.compose.home.mapper.SettingItemMapper
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingModelItem
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsUiState
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
class SettingHomeViewModelTest {
    private lateinit var underTest: SettingHomeViewModel

    private val settingItemMapper = SettingItemMapper()
    private val settingItemFlowMapper = SettingItemFlowMapper()
    private val sectionHeaderMapper = SectionHeaderMapper()
    private val settingHeaderComparator = SettingHeaderComparator()

    private fun initUnderTest(settings: Set<FeatureSettings>, scope: CoroutineScope) {
        underTest = SettingHomeViewModel(
            featureSettings = settings,
            settingItemMapper = settingItemMapper,
            settingItemFlowMapper = settingItemFlowMapper,
            sectionHeaderMapper = sectionHeaderMapper,
            settingHeaderComparator = settingHeaderComparator,
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
        val section1 = SettingSectionHeader.Custom("A")
        val section2 = SettingSectionHeader.Custom("B")

        val itemCount = 5
        val set1 = createFeatureSettingsSet(itemCount, section1)
        val set2 = createFeatureSettingsSet(itemCount, section2)

        val input = set1 + set2

        initUnderTest(input, this)

        underTest.state.filterIsInstance<SettingsUiState.Data>().test {
            val actual = awaitItem().settings
            assertThat(actual.map { it.key }).containsExactly(
                "A",
                "A0",
                "A1",
                "A2",
                "A3",
                "A4",
                "B",
                "B0",
                "B1",
                "B2",
                "B3",
                "B4",
            )
        }
    }

    @Test
    fun `test that function actions are called`() = runTest {
        var actioned = false
        val functionAction = suspend { actioned = true }
        val input = createFeatureSettingsSet(
            1, SettingSectionHeader.Custom("X"), SettingClickActionType.FunctionAction(
                functionAction
            )
        )

        initUnderTest(input, this)
        underTest.state.filterIsInstance<SettingsUiState.Data>().test {
            awaitItem().settings.filterIsInstance<SettingModelItem>().first().onClick(mock())
        }

        testScheduler.advanceUntilIdle()
        assertThat(actioned).isTrue()
    }

    @Test
    fun `test that exceptions from function actions are handled`() = runTest {
        val functionAction = suspend { throw Throwable("Bad things happened") }
        val input = createFeatureSettingsSet(
            1, SettingSectionHeader.Custom("Y"), SettingClickActionType.FunctionAction(
                functionAction
            )
        )

        initUnderTest(input, this)
        underTest.state.filterIsInstance<SettingsUiState.Data>().test {
            awaitItem().settings.filterIsInstance<SettingModelItem>().first().onClick(mock())
        }

        testScheduler.advanceUntilIdle()
    }


    private fun createFeatureSettingsSet(
        itemCount: Int,
        section: SettingSectionHeader.Custom,
        action: SettingClickActionType = SettingClickActionType.NavigationAction(
            Unit
        ),
    ) = (0 until itemCount).map {
        createFeatureSettingsEntry(
            section = section,
            key = section.name + it,
            action = action
        )
    }.toSet()

    private fun createFeatureSettingsEntry(
        section: SettingSectionHeader,
        key: String,
        action: SettingClickActionType,
    ) = object : FeatureSettings {
        override val entryPoints = listOf(
            SettingEntryPoint(
                section = section,
                items = listOf(
                    SettingItem(
                        key = key,
                        name = key,
                        descriptionValue = null,
                        isEnabled = null,
                        clickAction = action,
                        isDestructive = false
                    )
                )
            )
        )
        override val settingsNavGraph: NavGraphBuilder.(navHostController: NavHostController) -> Unit =
            { _ -> }

        override fun getTitleForDestination(entry: NavBackStackEntry): String? = null
    }
}