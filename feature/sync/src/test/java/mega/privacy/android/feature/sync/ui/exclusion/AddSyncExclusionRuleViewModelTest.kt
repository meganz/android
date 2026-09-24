package mega.privacy.android.feature.sync.ui.exclusion

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.usecase.exclusion.ValidateSyncExclusionValueUseCase
import mega.privacy.android.feature.sync.ui.mapper.exclusion.AddSyncExclusionResultMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionPatternMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncSizeExclusionFormMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionExtensionError
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import mega.privacy.android.shared.resources.R as sharedR

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddSyncExclusionRuleViewModelTest {

    private lateinit var underTest: AddSyncExclusionRuleViewModel

    @BeforeEach
    fun setUp() {
        underTest = createViewModel()
    }

    private fun createViewModel(staged: List<SyncExclusionRule> = emptyList()) =
        AddSyncExclusionRuleViewModel(
            staged = staged,
            resultMapper = AddSyncExclusionResultMapper(
                patternMapper = SyncExclusionPatternMapper(),
                sizeFormMapper = SyncSizeExclusionFormMapper(),
            ),
            validateSyncExclusionValueUseCase = ValidateSyncExclusionValueUseCase(),
        )

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
            assertThat(initial.canConfirm).isFalse()
        }
    }

    @Test
    fun `test that canConfirm stays false while the name form is incomplete`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)

        underTest.uiState.test {
            assertThat(awaitItem().canConfirm).isFalse()

            underTest.onConditionSelected(SyncExclusionCondition.EndingWith)

            assertThat(awaitItem().canConfirm).isFalse()

            underTest.onNameValueChanged("   ")

            assertThat(awaitItem().canConfirm).isFalse()
        }
    }

    @Test
    fun `test that canConfirm turns true once the name form names a rule`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
        underTest.onConditionSelected(SyncExclusionCondition.EndingWith)

        underTest.uiState.test {
            assertThat(awaitItem().canConfirm).isFalse()

            underTest.onNameValueChanged("tmp")

            assertThat(awaitItem().canConfirm).isTrue()
        }
    }

    @Test
    fun `test that canConfirm follows the extensions field once extensions is the type`() =
        runTest {
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
            underTest.onConditionSelected(SyncExclusionCondition.EndingWith)
            underTest.onNameValueChanged("tmp")

            underTest.uiState.test {
                assertThat(awaitItem().canConfirm).isTrue()

                underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

                assertThat(awaitItem().canConfirm).isFalse()

                underTest.onExtensionValueChanged("mp3")

                assertThat(awaitItem().canConfirm).isTrue()
            }
        }

    @Test
    fun `test that canConfirm stays false while the size form is incomplete`() = runTest {
        underTest.onTabSelected(AddExclusionTab.BySize)

        underTest.uiState.test {
            assertThat(awaitItem().canConfirm).isFalse()

            underTest.onSizeConditionSelected(SyncSizeCondition.BiggerThan)

            assertThat(awaitItem().canConfirm).isFalse()

            underTest.onFirstBoundValueChanged("0")

            assertThat(awaitItem().canConfirm).isFalse()
        }
    }

    @Test
    fun `test that canConfirm turns true once the size form declares a bound`() = runTest {
        underTest.onTabSelected(AddExclusionTab.BySize)
        underTest.onSizeConditionSelected(SyncSizeCondition.BiggerThan)

        underTest.uiState.test {
            assertThat(awaitItem().canConfirm).isFalse()

            underTest.onFirstBoundValueChanged("10")

            assertThat(awaitItem().canConfirm).isTrue()
        }
    }

    @Test
    fun `test that canConfirm stays false while a range reads downwards`() = runTest {
        underTest.onTabSelected(AddExclusionTab.BySize)
        underTest.onSizeConditionSelected(SyncSizeCondition.NotBetween)
        underTest.onFirstBoundValueChanged("2")
        underTest.onFirstBoundUnitSelected(SyncSizeUnit.Gigabytes)
        underTest.onSecondBoundValueChanged("1")
        underTest.onSecondBoundUnitSelected(SyncSizeUnit.Kilobytes)

        underTest.uiState.test {
            assertThat(awaitItem().canConfirm).isFalse()

            underTest.onFirstBoundUnitSelected(SyncSizeUnit.Bytes)

            assertThat(awaitItem().canConfirm).isTrue()
        }
    }

    @Test
    fun `test that canConfirm stays false while a range names the same size twice`() = runTest {
        underTest.onTabSelected(AddExclusionTab.BySize)
        underTest.onSizeConditionSelected(SyncSizeCondition.NotBetween)
        underTest.onFirstBoundValueChanged("1024")
        underTest.onFirstBoundUnitSelected(SyncSizeUnit.Bytes)
        underTest.onSecondBoundValueChanged("1")
        underTest.onSecondBoundUnitSelected(SyncSizeUnit.Kilobytes)

        underTest.uiState.test {
            assertThat(awaitItem().canConfirm).isFalse()

            underTest.onSecondBoundUnitSelected(SyncSizeUnit.Megabytes)

            assertThat(awaitItem().canConfirm).isTrue()
        }
    }

    /** The tab decides which draft is read, so a complete draft on the hidden tab must not count. */
    @Test
    fun `test that canConfirm reads the showing tab only`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
        underTest.onConditionSelected(SyncExclusionCondition.EndingWith)
        underTest.onNameValueChanged("tmp")

        underTest.uiState.test {
            assertThat(awaitItem().canConfirm).isTrue()

            underTest.onTabSelected(AddExclusionTab.BySize)

            assertThat(awaitItem().canConfirm).isFalse()
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

    @Test
    fun `test that onNameValueChanged shows the duplicate error when the rule is already in force`() =
        runTest {
            underTest = createViewModel(staged = listOf(draftFilesRule))
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
            underTest.onConditionSelected(SyncExclusionCondition.BeginningWith)

            underTest.onNameValueChanged("draft")

            val state = underTest.uiState.value
            assertThat(state.byName.errorRes)
                .isEqualTo(sharedR.string.sync_exclusions_add_name_duplicate_error)
            assertThat(state.canConfirm).isFalse()
        }

    /** Adding it again turns the switched off line back on, which is what the user asked for. */
    @Test
    fun `test that onNameValueChanged shows no error when the matching rule is switched off`() =
        runTest {
            underTest = createViewModel(staged = listOf(draftFilesRule.copy(enabled = false)))
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
            underTest.onConditionSelected(SyncExclusionCondition.BeginningWith)

            underTest.onNameValueChanged("draft")

            val state = underTest.uiState.value
            assertThat(state.byName.errorRes).isNull()
            assertThat(state.canConfirm).isTrue()
        }

    @Test
    fun `test that onNameValueChanged shows no error when the rule differs in target`() = runTest {
        underTest = createViewModel(staged = listOf(draftFilesRule))
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Folders)
        underTest.onConditionSelected(SyncExclusionCondition.BeginningWith)

        underTest.onNameValueChanged("draft")

        assertThat(underTest.uiState.value.byName.errorRes).isNull()
        assertThat(underTest.uiState.value.canConfirm).isTrue()
    }

    @Test
    fun `test that onNameValueChanged clears the duplicate error once the name changes`() =
        runTest {
            underTest = createViewModel(staged = listOf(draftFilesRule))
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
            underTest.onConditionSelected(SyncExclusionCondition.BeginningWith)
            underTest.onNameValueChanged("draft")

            underTest.onNameValueChanged("drafts")

            assertThat(underTest.uiState.value.byName.errorRes).isNull()
            assertThat(underTest.uiState.value.canConfirm).isTrue()
        }

    /** The SDK drops the trailing space as it reads the line, leaving two copies of one rule. */
    @Test
    fun `test that onNameValueChanged shows the duplicate error when a trailing space is all that differs`() =
        runTest {
            underTest = createViewModel(
                staged = listOf(
                    SyncExclusionRule(
                        pattern = "Thumbs.db",
                        matchType = SyncExclusionMatchType.SubtreeName,
                    ),
                ),
            )
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.FilesAndFolders)
            underTest.onConditionSelected(SyncExclusionCondition.MatchingExactly)

            underTest.onNameValueChanged("Thumbs.db ")

            assertThat(underTest.uiState.value.byName.errorRes)
                .isEqualTo(sharedR.string.sync_exclusions_add_name_duplicate_error)
            assertThat(underTest.uiState.value.canConfirm).isFalse()
        }

    /** The space sits before the wildcard, where the SDK keeps it, so this is a different rule. */
    @Test
    fun `test that onNameValueChanged shows no error when the space is inside the pattern`() =
        runTest {
            underTest = createViewModel(staged = listOf(draftFilesRule))
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
            underTest.onConditionSelected(SyncExclusionCondition.BeginningWith)

            underTest.onNameValueChanged("draft ")

            assertThat(underTest.uiState.value.byName.errorRes).isNull()
        }

    @Test
    fun `test that onTypeOptionSelected leaves an extension error out of the name form gate`() =
        runTest {
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)
            underTest.onExtensionValueChanged("mp/3")

            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
            underTest.onConditionSelected(SyncExclusionCondition.EndingWith)
            underTest.onNameValueChanged("tmp")

            assertThat(underTest.uiState.value.byName.errorRes).isNull()
            assertThat(underTest.uiState.value.canConfirm).isTrue()
        }

    @Test
    fun `test that onConditionSelected clears the duplicate error once the rule no longer matches`() =
        runTest {
            underTest = createViewModel(staged = listOf(draftFilesRule))
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Files)
            underTest.onConditionSelected(SyncExclusionCondition.BeginningWith)
            underTest.onNameValueChanged("draft")

            underTest.onConditionSelected(SyncExclusionCondition.EndingWith)

            assertThat(underTest.uiState.value.byName.errorRes).isNull()
        }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["mp 3", "mp/3", "mp*", "mp?", "a:b", "..mp3", "."])
    fun `test that onExtensionValueChanged shows the invalid error for an entry an extension cannot be`(
        entry: String,
    ) = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

        underTest.onExtensionValueChanged("txt, $entry")

        val state = underTest.uiState.value
        assertThat(state.byExtension.error).isEqualTo(AddExclusionExtensionError.Invalid)
        assertThat(state.canConfirm).isFalse()
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["mp3", ".mp3", "tar.gz", "txt, mp3"])
    fun `test that onExtensionValueChanged shows no error for valid extensions`(value: String) =
        runTest {
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

            underTest.onExtensionValueChanged(value)

            val state = underTest.uiState.value
            assertThat(state.byExtension.error).isNull()
            assertThat(state.canConfirm).isTrue()
        }

    @ParameterizedTest(name = "separator \"{0}\"")
    @ValueSource(strings = [",", "，", "、", "،"])
    fun `test that onExtensionValueChanged shows the duplicate error for an extension already in force`(
        separator: String,
    ) = runTest {
        underTest = createViewModel(staged = listOf(extensionRule("mp3")))
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

        underTest.onExtensionValueChanged("txt${separator}MP3")

        val state = underTest.uiState.value
        assertThat(state.byExtension.error).isEqualTo(AddExclusionExtensionError.Duplicate("mp3"))
        assertThat(state.canConfirm).isFalse()
    }

    @Test
    fun `test that onExtensionValueChanged shows no error for an extension that is switched off`() =
        runTest {
            underTest = createViewModel(staged = listOf(extensionRule("mp3").copy(enabled = false)))
            underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

            underTest.onExtensionValueChanged("mp3")

            assertThat(underTest.uiState.value.byExtension.error).isNull()
            assertThat(underTest.uiState.value.canConfirm).isTrue()
        }

    @Test
    fun `test that onExtensionValueChanged reports the first entry that is wrong`() = runTest {
        underTest = createViewModel(staged = listOf(extensionRule("mp3")))
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)

        underTest.onExtensionValueChanged("txt, mp3, mp 4")

        assertThat(underTest.uiState.value.byExtension.error)
            .isEqualTo(AddExclusionExtensionError.Duplicate("mp3"))
    }

    @Test
    fun `test that onExtensionValueChanged clears the error once the entry is fixed`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)
        underTest.onExtensionValueChanged("mp/3")

        underTest.onExtensionValueChanged("mp3")

        assertThat(underTest.uiState.value.byExtension.error).isNull()
        assertThat(underTest.uiState.value.canConfirm).isTrue()
    }

    @Test
    fun `test that canConfirm ignores an error left on a body that is not showing`() = runTest {
        underTest.onTypeOptionSelected(SyncExclusionTypeOption.Extensions)
        underTest.onExtensionValueChanged("mp/3")
        underTest.onTabSelected(AddExclusionTab.BySize)

        underTest.onSizeConditionSelected(SyncSizeCondition.BiggerThan)
        underTest.onFirstBoundValueChanged("10")

        assertThat(underTest.uiState.value.canConfirm).isTrue()
    }

    private val draftFilesRule = SyncExclusionRule(
        pattern = "draft*",
        target = SyncExclusionTarget.Files,
        matchType = SyncExclusionMatchType.SubtreeName,
    )

    private fun extensionRule(extension: String) = SyncExclusionRule(
        pattern = "*.$extension",
        matchType = SyncExclusionMatchType.SubtreeName,
    )
}
