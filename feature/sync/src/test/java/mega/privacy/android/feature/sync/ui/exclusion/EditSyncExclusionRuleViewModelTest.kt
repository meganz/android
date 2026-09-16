package mega.privacy.android.feature.sync.ui.exclusion

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionEditPrefillMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionPatternMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EditSyncExclusionRuleViewModelTest {

    private val prefillMapper = SyncExclusionEditPrefillMapper(SyncExclusionPatternMapper())

    private fun underTest(
        pattern: String = "*temp*",
        target: SyncExclusionTarget = SyncExclusionTarget.Directories,
    ) = EditSyncExclusionRuleViewModel(
        rule = SyncExclusionRule(
            pattern = pattern,
            target = target,
            matchType = SyncExclusionMatchType.SubtreeName,
        ),
        prefillMapper = prefillMapper,
    )

    @Test
    fun `test that init prefills a rule with no type from its whole pattern`() = runTest {
        val viewModel = underTest(pattern = "draft*", target = SyncExclusionTarget.Symlinks)

        viewModel.uiState.test {
            val initial = awaitItem().byName

            assertThat(initial.typeOption).isNull()
            assertThat(initial.condition).isEqualTo(SyncExclusionCondition.MatchingWildcard)
            assertThat(initial.value).isEqualTo("draft*")
        }
    }

    @Test
    fun `test that init prefills the form from the rule`() = runTest {
        underTest().uiState.test {
            val initial = awaitItem().byName

            assertThat(initial.typeOption).isEqualTo(SyncExclusionTypeOption.Folders)
            assertThat(initial.condition).isEqualTo(SyncExclusionCondition.Containing)
            assertThat(initial.value).isEqualTo("temp")
        }
    }

    @Test
    fun `test that onTypeOptionSelected replaces the type`() = runTest {
        val viewModel = underTest()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onTypeOptionSelected(SyncExclusionTypeOption.Files)

            assertThat(awaitItem().byName.typeOption).isEqualTo(SyncExclusionTypeOption.Files)
        }
    }

    @Test
    fun `test that onTypeOptionSelected keeps the condition the rule opened with`() =
        runTest {
            val viewModel = underTest()

            viewModel.uiState.test {
                awaitItem()

                viewModel.onTypeOptionSelected(SyncExclusionTypeOption.Files)

                assertThat(awaitItem().byName.condition)
                    .isEqualTo(SyncExclusionCondition.Containing)
            }
        }

    @Test
    fun `test that onConditionSelected replaces the condition`() = runTest {
        val viewModel = underTest()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onConditionSelected(SyncExclusionCondition.EndingWith)

            assertThat(awaitItem().byName.condition).isEqualTo(SyncExclusionCondition.EndingWith)
        }
    }

    @Test
    fun `test that onValueChanged replaces the value`() = runTest {
        val viewModel = underTest()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onValueChanged("cache")

            assertThat(awaitItem().byName.value).isEqualTo("cache")
        }
    }
}
