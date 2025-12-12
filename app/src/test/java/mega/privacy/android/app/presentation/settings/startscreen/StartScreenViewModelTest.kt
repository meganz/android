package mega.privacy.android.app.presentation.settings.startscreen

import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.content.mapper.ScreenPreferenceDestinationMapper
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenDestinationOptionMapper
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenDestinationPreferenceNavKeyMapper
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenOptionMapper
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOption
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenSettingsState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.SetStartScreenPreference
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.domain.usecase.preference.SetStartScreenPreferenceDestinationUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartScreenViewModelTest {
    private lateinit var underTest: StartScreenViewModel

    private val monitorStartScreenPreference = mock<MonitorStartScreenPreference>()

    private val setStartScreenPreference = mock<SetStartScreenPreference>()

    private val startScreenDestinationOptionMapper = mock<StartScreenDestinationOptionMapper>()

    private val setStartScreenPreferenceDestinationUseCase =
        mock<SetStartScreenPreferenceDestinationUseCase>()

    private val startScreenDestinationPreferenceNavKeyMapper =
        mock<StartScreenDestinationPreferenceNavKeyMapper>()

    private val monitorStartScreenPreferenceDestinationUseCase =
        mock<MonitorStartScreenPreferenceDestinationUseCase>()
    private val screenPreferenceDestinationMapper = mock<ScreenPreferenceDestinationMapper>()

    private val mapStartScreenOption = mock<StartScreenOptionMapper> {
        on { invoke(any()) }.thenAnswer {
            val startScreen = it.arguments[0] as StartScreen
            StartScreenOption(startScreen, 0, IconPack.Medium.Thin.Outline.AlertCircle)
        }
    }

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    private val defaultStartScreen = mock<MainNavItemNavKey>()

    private fun initViewModel(mainNavItems: Set<MainNavItem> = setOf(mock<MainNavItem>())) {
        underTest = StartScreenViewModel(
            monitorStartScreenPreference = monitorStartScreenPreference,
            setStartScreenPreference = setStartScreenPreference,
            startScreenOptionMapper = mapStartScreenOption,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            mainDestinations = mainNavItems,
            monitorStartScreenPreferenceDestinationUseCase = monitorStartScreenPreferenceDestinationUseCase,
            screenPreferenceDestinationMapper = screenPreferenceDestinationMapper,
            startScreenDestinationPreferenceNavKeyMapper = startScreenDestinationPreferenceNavKeyMapper,
            setStartScreenPreferenceDestinationUseCase = setStartScreenPreferenceDestinationUseCase,
            startScreenDestinationOptionMapper = startScreenDestinationOptionMapper,
            defaultStartScreen = defaultStartScreen,
        )
    }

    @BeforeEach
    fun setUp() {
        reset(
            monitorStartScreenPreference,
            monitorStartScreenPreferenceDestinationUseCase,
            setStartScreenPreference,
            setStartScreenPreferenceDestinationUseCase,
        )
    }

    @ParameterizedTest(name = "with single activity flag enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that initial value has default screen selected`(singleActivityFlagEnabled: Boolean) =
        runTest {
            getFeatureFlagValueUseCase.stub { onBlocking { invoke(AppFeatures.SingleActivity) } doReturn singleActivityFlagEnabled }

            val firstMainNavItem = mock<MainNavItem> {
                on { preferredSlot } doReturn PreferredSlot.Ordered(1)
            }
            val mainNavItems = setOf(
                firstMainNavItem,
                mock<MainNavItem> { on { preferredSlot } doReturn PreferredSlot.Ordered(2) },
                mock<MainNavItem> { on { preferredSlot } doReturn PreferredSlot.Last },
            )
            initViewModel(mainNavItems)

            if (singleActivityFlagEnabled) {
                val expected = defaultStartScreen
                val option = mock<StartScreenOption<NavKey>> {
                    on { startScreen } doReturn expected
                }

                startScreenDestinationOptionMapper.stub {
                    on { invoke(firstMainNavItem) } doReturn option
                }


                monitorStartScreenPreferenceDestinationUseCase.stub {
                    on { invoke() } doReturn
                            flow {
                                emit(null)
                                awaitCancellation()
                            }
                }

                underTest.state
                    .filterIsInstance<StartScreenSettingsState.Data>()
                    .test {
                        val (_, selectedScreen) = awaitItem()
                        assertThat(selectedScreen).isEqualTo(expected)
                    }
            } else {
                val expected = StartScreen.Chat
                monitorStartScreenPreference.stub {
                    on { invoke() } doReturn
                            flow {
                                emit(expected)
                                awaitCancellation()
                            }
                }

                underTest.state
                    .filterIsInstance<StartScreenSettingsState.LegacyData>()
                    .test {
                        val (_, selectedScreen) = awaitItem()
                        assertThat(selectedScreen).isEqualTo(expected)
                    }
            }
        }

    @ParameterizedTest(name = "with single activity flag enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that all values are included as an option`(singleActivityFlagEnabled: Boolean) =
        runTest {
            getFeatureFlagValueUseCase.stub { onBlocking { invoke(AppFeatures.SingleActivity) } doReturn singleActivityFlagEnabled }

            val expectedCount = 5

            val mainNavItems = mutableSetOf<MainNavItem>().apply {
                repeat(expectedCount) {
                    add(mock<MainNavItem> {
                        on { preferredSlot } doReturn PreferredSlot.Ordered(1)
                    })
                }
            }

            // Stub mapper before initializing ViewModel to avoid flow timeout
            if (singleActivityFlagEnabled) {
                val option = mock<StartScreenOption<NavKey>>()
                startScreenDestinationOptionMapper.stub {
                    on { invoke(any()) } doReturn option
                }

                val preferenceFlow = MutableStateFlow<StartScreenDestinationPreference?>(null)
                monitorStartScreenPreferenceDestinationUseCase.stub {
                    on { invoke() } doReturn preferenceFlow
                }
            } else {
                val preferenceFlow = MutableStateFlow<StartScreen>(StartScreen.Home)
                monitorStartScreenPreference.stub {
                    on { invoke() } doReturn preferenceFlow
                }
            }

            initViewModel(mainNavItems)

            if (singleActivityFlagEnabled) {
                underTest.state
                    .filterIsInstance<StartScreenSettingsState.Data>()
                    .test {
                        assertThat(awaitItem().options).hasSize(expectedCount)
                    }
            } else {
                underTest.state
                    .filterIsInstance<StartScreenSettingsState.LegacyData>()
                    .test {
                        val (options, _) = awaitItem()
                        assertThat(options.map { it.startScreen }).containsExactlyElementsIn(
                            StartScreen.entries
                        )
                    }
            }
        }

    @ParameterizedTest(name = "with single activity flag enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that selected screen is set if returned`(singleActivityFlagEnabled: Boolean) =
        runTest {
            getFeatureFlagValueUseCase.stub { onBlocking { invoke(AppFeatures.SingleActivity) } doReturn singleActivityFlagEnabled }

            initViewModel()
            val preference = mock<StartScreenDestinationPreference>()
            monitorStartScreenPreferenceDestinationUseCase.stub {
                on { invoke() } doReturn
                        flow {
                            emit(preference)
                            awaitCancellation()
                        }
            }
            if (singleActivityFlagEnabled) {
                val expected = mock<MainNavItemNavKey>()
                screenPreferenceDestinationMapper.stub {
                    on { invoke(preference) } doReturn expected
                }
                underTest.state
                    .filterIsInstance<StartScreenSettingsState.Data>()
                    .map { it.selectedScreen }
                    .test {
                        assertThat(awaitItem()).isEqualTo(expected)
                    }
            } else {
                val expected = StartScreen.Chat
                whenever(monitorStartScreenPreference()).thenReturn(flowOf(expected))
                underTest.state
                    .filterIsInstance<StartScreenSettingsState.LegacyData>()
                    .map { it.selectedScreen }
                    .test {
                        assertThat(awaitItem()).isEqualTo(expected)
                    }
            }

        }

    @ParameterizedTest(name = "with single activity flag enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that set use case is called when new screen is set`(singleActivityFlagEnabled: Boolean) {

        initViewModel()

        if (singleActivityFlagEnabled) {
            val input = mock<NavKey>()
            val expected = mock<StartScreenDestinationPreference>()
            startScreenDestinationPreferenceNavKeyMapper.stub {
                on { invoke(input) } doReturn expected
            }
            underTest.navDestinationClicked(input)
            scheduler.advanceUntilIdle()
            verifyBlocking(setStartScreenPreferenceDestinationUseCase) { invoke(expected) }
        } else {
            val expected = StartScreen.Photos
            underTest.newScreenClicked(expected)
            scheduler.advanceUntilIdle()
            verifyBlocking(setStartScreenPreference) { invoke(expected) }
        }

    }

    @Test
    fun `test that nav items are sorted by preferred slot when single activity flag is enabled`() =
        runTest {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(AppFeatures.SingleActivity) } doReturn true
            }

            // Create nav items with different preferred slots in random order
            val navItemSlot3 = mock<MainNavItem> {
                on { preferredSlot } doReturn PreferredSlot.Ordered(3)
            }
            val navItemSlot1 = mock<MainNavItem> {
                on { preferredSlot } doReturn PreferredSlot.Ordered(1)
            }
            val navItemLast = mock<MainNavItem> {
                on { preferredSlot } doReturn PreferredSlot.Last
            }
            val navItemSlot2 = mock<MainNavItem> {
                on { preferredSlot } doReturn PreferredSlot.Ordered(2)
            }
            val navItemSlot5 = mock<MainNavItem> {
                on { preferredSlot } doReturn PreferredSlot.Ordered(5)
            }

            // Pass items in random order
            val mainNavItems = setOf(
                navItemSlot3,
                navItemSlot1,
                navItemLast,
                navItemSlot2,
                navItemSlot5,
            )

            // Create corresponding options with identifiable destinations
            val navKeySlot1 = mock<MainNavItemNavKey> {
                on { toString() } doReturn "slot1"
            }
            val navKeySlot2 = mock<MainNavItemNavKey> {
                on { toString() } doReturn "slot2"
            }
            val navKeySlot3 = mock<MainNavItemNavKey> {
                on { toString() } doReturn "slot3"
            }
            val navKeySlot5 = mock<MainNavItemNavKey> {
                on { toString() } doReturn "slot5"
            }
            val navKeyLast = mock<MainNavItemNavKey> {
                on { toString() } doReturn "last"
            }

            val optionSlot1 = mock<StartScreenOption<NavKey>> {
                on { startScreen } doReturn navKeySlot1
            }
            val optionSlot2 = mock<StartScreenOption<NavKey>> {
                on { startScreen } doReturn navKeySlot2
            }
            val optionSlot3 = mock<StartScreenOption<NavKey>> {
                on { startScreen } doReturn navKeySlot3
            }
            val optionSlot5 = mock<StartScreenOption<NavKey>> {
                on { startScreen } doReturn navKeySlot5
            }
            val optionLast = mock<StartScreenOption<NavKey>> {
                on { startScreen } doReturn navKeyLast
            }

            whenever(startScreenDestinationOptionMapper(navItemSlot1)).thenReturn(optionSlot1)
            whenever(startScreenDestinationOptionMapper(navItemSlot2)).thenReturn(optionSlot2)
            whenever(startScreenDestinationOptionMapper(navItemSlot3)).thenReturn(optionSlot3)
            whenever(startScreenDestinationOptionMapper(navItemSlot5)).thenReturn(optionSlot5)
            whenever(startScreenDestinationOptionMapper(navItemLast)).thenReturn(optionLast)

            monitorStartScreenPreferenceDestinationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(null)
                    awaitCancellation()
                }
            }

            initViewModel(mainNavItems)

            underTest.state
                .filterIsInstance<StartScreenSettingsState.Data>()
                .test {
                    val state = awaitItem()
                    val options = state.options

                    // Verify order: slot1, slot2, slot3, slot5, last
                    assertThat(options).hasSize(5)
                    assertThat(options[0].startScreen.toString()).isEqualTo("slot1")
                    assertThat(options[1].startScreen.toString()).isEqualTo("slot2")
                    assertThat(options[2].startScreen.toString()).isEqualTo("slot3")
                    assertThat(options[3].startScreen.toString()).isEqualTo("slot5")
                    assertThat(options[4].startScreen.toString()).isEqualTo("last")
                }
        }


    companion object {
        private val scheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(scheduler))
    }
}