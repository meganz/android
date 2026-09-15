package mega.privacy.android.feature.sync.ui.exclusion

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddSyncExclusionRuleViewModelTest {

    private lateinit var underTest: AddSyncExclusionRuleViewModel

    @BeforeEach
    fun setUp() {
        underTest = AddSyncExclusionRuleViewModel()
    }

    @Test
    fun `test that init opens the by name tab with empty drafts`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()

            assertThat(initial.selectedTab).isEqualTo(AddExclusionTab.ByName)
            assertThat(initial.byName.typeOption).isNull()
            assertThat(initial.byName.value).isEmpty()
            assertThat(initial.byExtension.value).isEmpty()
            assertThat(initial.bySize).isEqualTo(AddExclusionBySizeFormState())
            assertThat(initial.showsExtensionForm).isFalse()
        }
    }

    @Test
    fun `test that onTabSelected switches the tab`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().selectedTab).isEqualTo(AddExclusionTab.ByName)

            underTest.onTabSelected(AddExclusionTab.BySize)

            assertThat(awaitItem().selectedTab).isEqualTo(AddExclusionTab.BySize)
        }
    }

    @Test
    fun `test that onTabSelected emits nothing when the tab is already showing`() = runTest {
        underTest.uiState.test {
            awaitItem()

            underTest.onTabSelected(AddExclusionTab.ByName)

            expectNoEvents()
        }
    }

    @Test
    fun `test that switching tabs keeps the name draft for the tab it belongs to`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
        underTest.onConditionSelected(SyncExclusionCondition.EndingWith)
        underTest.onNameValueChanged("tmp")

        underTest.uiState.test {
            assertThat(awaitItem().byName.value).isEqualTo("tmp")

            underTest.onTabSelected(AddExclusionTab.BySize)

            val state = awaitItem()
            assertThat(state.selectedTab).isEqualTo(AddExclusionTab.BySize)
            assertThat(state.byName.value).isEqualTo("tmp")
            assertThat(state.byName.condition).isEqualTo(SyncExclusionCondition.EndingWith)
        }
    }

    @Test
    fun `test that onTypeOptionSelected drops the condition when extensions is picked`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
        underTest.onConditionSelected(SyncExclusionCondition.Containing)

        underTest.uiState.test {
            assertThat(awaitItem().byName.condition).isEqualTo(SyncExclusionCondition.Containing)

            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

            assertThat(awaitItem().byName.condition).isNull()
        }
    }

    @Test
    fun `test that onTypeOptionSelected keeps the condition when another name type is picked`() =
        runTest {
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
            underTest.onConditionSelected(SyncExclusionCondition.Containing)

            underTest.uiState.test {
                awaitItem()

                underTest.onTypeOptionSelected(SyncExclusionTypeOption.Folders)

                val state = awaitItem()
                assertThat(state.byName.typeOption).isEqualTo(SyncExclusionTypeOption.Folders)
                assertThat(state.byName.condition).isEqualTo(SyncExclusionCondition.Containing)
            }
        }

    @Test
    fun `test that showsExtensionForm swaps the by name body when extensions is picked`() =
        runTest {
            underTest.uiState.test {
                assertThat(awaitItem().showsExtensionForm).isFalse()

                underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

                assertThat(awaitItem().showsExtensionForm).isTrue()
            }
        }

    @Test
    fun `test that showsExtensionForm keeps the name body for a name type`() = runTest {
        underTest.uiState.test {
            awaitItem()

            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)

            assertThat(awaitItem().showsExtensionForm).isFalse()
        }
    }

    @Test
    fun `test that showsExtensionForm is false on the size tab`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

        underTest.uiState.test {
            assertThat(awaitItem().showsExtensionForm).isTrue()

            underTest.onTabSelected(AddExclusionTab.BySize)

            assertThat(awaitItem().showsExtensionForm).isFalse()
        }
    }

    @Test
    fun `test that switching tabs does not carry a name draft into the size form`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
        underTest.onConditionSelected(SyncExclusionCondition.EndingWith)
        underTest.onNameValueChanged("tmp")

        underTest.uiState.test {
            assertThat(awaitItem().byName.value).isEqualTo("tmp")

            underTest.onTabSelected(AddExclusionTab.BySize)

            val state = awaitItem()
            assertThat(state.bySize).isEqualTo(AddExclusionBySizeFormState())
            assertThat(state.byName.value).isEqualTo("tmp")
        }
    }

    @Test
    fun `test that switching tabs does not carry a size draft into the name form`() = runTest {
        underTest.onTabSelected(AddExclusionTab.BySize)
        underTest.onSizeConditionSelected(SyncSizeCondition.BiggerThan)
        underTest.onFirstBoundValueChanged("10")

        underTest.uiState.test {
            assertThat(awaitItem().bySize.firstBound.value).isEqualTo("10")

            underTest.onTabSelected(AddExclusionTab.ByName)

            val state = awaitItem()
            assertThat(state.byName.value).isEmpty()
            assertThat(state.byName.typeOption).isNull()
            assertThat(state.bySize.firstBound.value).isEqualTo("10")
        }
    }

    /** Each edit is awaited on its own, so a bound that moved another would surface here. */
    @Test
    fun `test that each size bound keeps its own unit`() = runTest {
        underTest.onSizeConditionSelected(SyncSizeCondition.NotBetween)

        underTest.uiState.test {
            awaitItem()

            underTest.onFirstBoundValueChanged("1")
            assertThat(awaitItem().bySize.firstBound.value).isEqualTo("1")

            underTest.onFirstBoundUnitSelected(SyncSizeUnit.Kilobytes)
            assertThat(awaitItem().bySize.firstBound.unit).isEqualTo(SyncSizeUnit.Kilobytes)

            underTest.onSecondBoundValueChanged("2")
            assertThat(awaitItem().bySize.secondBound.value).isEqualTo("2")

            underTest.onSecondBoundUnitSelected(SyncSizeUnit.Gigabytes)

            val bySize = awaitItem().bySize
            assertThat(bySize.firstBound.unit).isEqualTo(SyncSizeUnit.Kilobytes)
            assertThat(bySize.secondBound.unit).isEqualTo(SyncSizeUnit.Gigabytes)
            assertThat(bySize.firstBound.bytes).isEqualTo(1_024L)
            assertThat(bySize.secondBound.bytes).isEqualTo(2_147_483_648L)
        }
    }

    @Test
    fun `test that changing one bound unit leaves the other alone`() = runTest {
        underTest.onSizeConditionSelected(SyncSizeCondition.NotBetween)

        underTest.uiState.test {
            assertThat(awaitItem().bySize.firstBound.unit).isEqualTo(SyncSizeUnit.Megabytes)

            underTest.onSecondBoundUnitSelected(SyncSizeUnit.Bytes)

            val bySize = awaitItem().bySize
            assertThat(bySize.secondBound.unit).isEqualTo(SyncSizeUnit.Bytes)
            assertThat(bySize.firstBound.unit).isEqualTo(SyncSizeUnit.Megabytes)
        }
    }

    @Test
    fun `test that the extensions draft is separate from the name draft`() = runTest {
        underTest.onNameValueChanged("notes")

        underTest.uiState.test {
            assertThat(awaitItem().byName.value).isEqualTo("notes")

            underTest.onExtensionValueChanged("mp3, mp4")

            val state = awaitItem()
            assertThat(state.byName.value).isEqualTo("notes")
            assertThat(state.byExtension.extensions).containsExactly("mp3", "mp4")
        }
    }
}
