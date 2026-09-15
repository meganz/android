package mega.privacy.android.feature.sync.ui.exclusion

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
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
