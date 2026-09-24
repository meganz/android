package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.SyncMatchStrategy
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.navigation.AddSyncExclusionResult
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByExtensionFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.AddSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddSyncExclusionResultMapperTest {

    private val underTest = AddSyncExclusionResultMapper(
        patternMapper = SyncExclusionPatternMapper(),
        sizeFormMapper = SyncSizeExclusionFormMapper(),
    )

    @Test
    fun `test that invoke adds nothing when the name form has no type`() {
        val actual = underTest(
            state(byName = AddExclusionByNameFormState(value = "temp")),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke adds nothing when the name form has no condition`() {
        val actual = underTest(
            state(
                byName = AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.Files,
                    value = "temp",
                ),
            ),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke adds nothing when the name form value is blank`() {
        val actual = underTest(
            state(
                byName = AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.Files,
                    condition = SyncExclusionCondition.Containing,
                    value = "   ",
                ),
            ),
        )

        assertThat(actual).isNull()
    }

    @ParameterizedTest(name = "{0} becomes {1}")
    @MethodSource("targets")
    fun `test that invoke builds one rule targeting the type the user picked`(
        typeOption: SyncExclusionTypeOption,
        expected: SyncExclusionTarget,
    ) {
        val actual = underTest(
            state(
                byName = AddExclusionByNameFormState(
                    typeOption = typeOption,
                    condition = SyncExclusionCondition.BeginningWith,
                    value = "draft",
                ),
            ),
        )

        val rules = (actual as AddSyncExclusionResult.Rules).rules
        assertThat(rules).hasSize(1)
        assertThat(rules.single().pattern).isEqualTo("draft*")
        assertThat(rules.single().target).isEqualTo(expected)
        assertThat(rules.single().matchType).isEqualTo(SyncExclusionMatchType.SubtreeName)
    }

    private fun targets(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncExclusionTypeOption.Files, SyncExclusionTarget.Files),
        Arguments.of(SyncExclusionTypeOption.Folders, SyncExclusionTarget.Directories),
        Arguments.of(SyncExclusionTypeOption.FilesAndFolders, SyncExclusionTarget.All),
    )

    /**
     * The grammar this screen never renders has to come out as what an unlettered line means, or a
     * rule added here would not read back as the one the user described.
     */
    @Test
    fun `test that invoke leaves the fields the form does not render at their defaults`() {
        val actual = underTest(
            state(
                byName = AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.Files,
                    condition = SyncExclusionCondition.MatchingExactly,
                    value = "notes.txt",
                ),
            ),
        )

        val rule = (actual as AddSyncExclusionResult.Rules).rules.single()
        assertThat(rule.exclusionClass).isEqualTo(SyncExclusionClass.Exclude)
        assertThat(rule.strategy).isEqualTo(SyncMatchStrategy.GlobCaseInsensitive)
        assertThat(rule.enabled).isTrue()
    }

    @Test
    fun `test that invoke builds a rule per extension the field names`() {
        val actual = underTest(
            state(
                byName = extensionsSelected,
                byExtension = AddExclusionByExtensionFormState(value = "mp3, tar.gz"),
            ),
        )

        val rules = (actual as AddSyncExclusionResult.Rules).rules
        assertThat(rules.map { it.pattern }).containsExactly("*.mp3", "*.tar.gz").inOrder()
        assertThat(rules.map { it.target }).containsExactly(
            SyncExclusionTarget.All,
            SyncExclusionTarget.All,
        )
    }

    /**
     * A predefined extension is held at every node type, and only a rule of that shape is shown as
     * a chip and folded into the default holding the same pattern.
     */
    @Test
    fun `test that invoke builds an extension the same way as a predefined one`() {
        val actual = underTest(
            state(
                byName = extensionsSelected,
                byExtension = AddExclusionByExtensionFormState(value = "tmp"),
            ),
        )

        val rule = (actual as AddSyncExclusionResult.Rules).rules.single()
        assertThat(rule).isEqualTo(
            MegaIgnoreDefaults.RULES.single { it.pattern == "*.tmp" },
        )
    }

    /**
     * De-duplication elsewhere compares patterns exactly, so a repeated entry would otherwise
     * stage the same rule twice.
     */
    @Test
    fun `test that invoke folds extensions naming the same pattern`() {
        val actual = underTest(
            state(
                byName = extensionsSelected,
                byExtension = AddExclusionByExtensionFormState(value = "mp3, .mp3, MP3"),
            ),
        )

        val rules = (actual as AddSyncExclusionResult.Rules).rules
        assertThat(rules.map { it.pattern }).containsExactly("*.mp3")
    }

    @Test
    fun `test that invoke drops an extension no pattern can be built from`() {
        val actual = underTest(
            state(
                byName = extensionsSelected,
                byExtension = AddExclusionByExtensionFormState(value = "..mp3, jpg"),
            ),
        )

        val rules = (actual as AddSyncExclusionResult.Rules).rules
        assertThat(rules.map { it.pattern }).containsExactly("*.jpg")
    }

    @Test
    fun `test that invoke adds nothing when no extension yields a pattern`() {
        val actual = underTest(
            state(
                byName = extensionsSelected,
                byExtension = AddExclusionByExtensionFormState(value = "..mp3, , "),
            ),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke reads the size form when the size tab is showing`() {
        val actual = underTest(
            state(
                selectedTab = AddExclusionTab.BySize,
                bySize = AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.BiggerThan,
                    firstBound = SyncSizeBoundInput("100", SyncSizeUnit.Megabytes),
                ),
            ),
        )

        assertThat(actual).isEqualTo(
            AddSyncExclusionResult.SizeBounds(
                SyncSizeExclusion(excludeLargerThanBytes = 100L * 1024 * 1024),
            ),
        )
    }

    @Test
    fun `test that invoke adds nothing when the size form is incomplete`() {
        val actual = underTest(
            state(
                selectedTab = AddExclusionTab.BySize,
                bySize = AddExclusionBySizeFormState(condition = SyncSizeCondition.BiggerThan),
            ),
        )

        assertThat(actual).isNull()
    }

    /**
     * Each tab keeps its own draft, so reading the tab that is not showing would add a rule the
     * user built and then moved away from.
     */
    @Test
    fun `test that invoke reads only the tab that is showing`() {
        val actual = underTest(
            state(
                selectedTab = AddExclusionTab.BySize,
                byName = AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.Files,
                    condition = SyncExclusionCondition.Containing,
                    value = "temp",
                ),
                bySize = AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.SmallerThan,
                    firstBound = SyncSizeBoundInput("1", SyncSizeUnit.Kilobytes),
                ),
            ),
        )

        assertThat(actual).isEqualTo(
            AddSyncExclusionResult.SizeBounds(SyncSizeExclusion(excludeSmallerThanBytes = 1024)),
        )
    }

    private val extensionsSelected = AddExclusionByNameFormState(
        typeOption = SyncExclusionTypeOption.Extensions,
    )

    private fun state(
        selectedTab: AddExclusionTab = AddExclusionTab.ByName,
        byName: AddExclusionByNameFormState = AddExclusionByNameFormState(),
        byExtension: AddExclusionByExtensionFormState = AddExclusionByExtensionFormState(),
        bySize: AddExclusionBySizeFormState = AddExclusionBySizeFormState(),
    ) = AddSyncExclusionRuleUiState(
        selectedTab = selectedTab,
        byName = byName,
        byExtension = byExtension,
        bySize = bySize,
    )

    @Test
    fun `test that extensionRule builds the rule an extension entry stands for`() {
        assertThat(underTest.extensionRule(".tar.gz")).isEqualTo(
            SyncExclusionRule(
                pattern = "*.tar.gz",
                target = SyncExclusionTarget.All,
                matchType = SyncExclusionMatchType.SubtreeName,
            ),
        )
    }

    @Test
    fun `test that extensionRule returns null when no pattern can be built`() {
        assertThat(underTest.extensionRule("..mp3")).isNull()
    }
}
