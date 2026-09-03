package mega.privacy.android.feature.sync.domain.usecase.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionValidation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidateSyncExclusionValueUseCaseTest {

    private val underTest = ValidateSyncExclusionValueUseCase()

    @Test
    fun `test that invoke returns Valid for an ordinary value`() {
        assertThat(validate("Thumbs.db", matchType = SyncExclusionMatchType.LocalName))
            .isEqualTo(SyncExclusionValidation.Valid)
    }

    @Test
    fun `test that invoke returns Valid when the value holds a comma`() {
        assertThat(validate("Backup,old")).isEqualTo(SyncExclusionValidation.Valid)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["My Folder", "a\tb"])
    fun `test that invoke returns Valid when whitespace falls inside the value`(value: String) {
        assertThat(validate(value)).isEqualTo(SyncExclusionValidation.Valid)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["temp\u00A0", "\u00A0temp"])
    fun `test that invoke returns Valid when the padding is whitespace the sdk keeps`(
        value: String,
    ) {
        assertThat(validate(value)).isEqualTo(SyncExclusionValidation.Valid)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", " ", "   ", "\t", " \t "])
    fun `test that invoke returns Blank when the value holds nothing to match on`(value: String) {
        assertThat(validate(value)).isEqualTo(SyncExclusionValidation.Blank)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["a\nb", "a\r\nb", "a\rb", "Thumbs.db\n", "\nThumbs.db"])
    fun `test that invoke returns ContainsLineBreak when the value spans more than one line`(
        value: String,
    ) {
        assertThat(validate(value)).isEqualTo(SyncExclusionValidation.ContainsLineBreak)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["Thumbs.db ", "  temp", "\tThumbs.db", "Thumbs.db\t", " Thumbs.db "])
    fun `test that invoke returns LeadingOrTrailingWhitespace when the value is padded`(
        value: String,
    ) {
        assertThat(validate(value))
            .isEqualTo(SyncExclusionValidation.LeadingOrTrailingWhitespace)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["C:", "foo:bar", ":", "-n:Thumbs.db", "C:\\Users"])
    fun `test that invoke returns ContainsPatternSeparator when the value holds a colon`(
        value: String,
    ) {
        assertThat(validate(value)).isEqualTo(SyncExclusionValidation.ContainsPatternSeparator)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["*", "?", "*.tmp", "file?.txt", "My*Folder"])
    fun `test that invoke returns ContainsWildcard when wildcards are not allowed`(value: String) {
        assertThat(validate(value, wildcardsAllowed = false))
            .isEqualTo(SyncExclusionValidation.ContainsWildcard)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["*", "?", "*.tmp", "file?.txt", "My*Folder"])
    fun `test that invoke returns Valid for the same wildcards when they are allowed`(
        value: String,
    ) {
        assertThat(validate(value, wildcardsAllowed = true))
            .isEqualTo(SyncExclusionValidation.Valid)
    }

    @Test
    fun `test that invoke returns ContainsWildcard when a path rule holds a disallowed wildcard`() {
        assertThat(
            validate(
                value = "Documents/*.tmp",
                matchType = SyncExclusionMatchType.Path,
                wildcardsAllowed = false,
            )
        ).isEqualTo(SyncExclusionValidation.ContainsWildcard)
    }

    @Test
    fun `test that invoke returns ContainsPatternSeparator when wildcards are allowed`() {
        assertThat(validate("*:tmp", wildcardsAllowed = true))
            .isEqualTo(SyncExclusionValidation.ContainsPatternSeparator)
    }

    @ParameterizedTest
    @EnumSource(
        value = SyncExclusionMatchType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["Path"],
    )
    fun `test that invoke returns ContainsPathSeparator when a name rule holds a slash`(
        matchType: SyncExclusionMatchType,
    ) {
        assertThat(validate("Documents/Drafts", matchType = matchType))
            .isEqualTo(SyncExclusionValidation.ContainsPathSeparator)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["Documents/Drafts", "/Drafts", "Documents/", "a/b/c"])
    fun `test that invoke returns Valid when a path rule holds a slash`(value: String) {
        assertThat(validate(value, matchType = SyncExclusionMatchType.Path))
            .isEqualTo(SyncExclusionValidation.Valid)
    }

    @Test
    fun `test that invoke returns Blank when a whitespace only value also holds a line break`() {
        assertThat(validate("  \n  ")).isEqualTo(SyncExclusionValidation.Blank)
    }

    @Test
    fun `test that invoke returns ContainsLineBreak when a padded value also holds a line break`() {
        assertThat(validate(" a\nb ")).isEqualTo(SyncExclusionValidation.ContainsLineBreak)
    }

    @Test
    fun `test that invoke returns ContainsLineBreak when the value also holds a colon`() {
        assertThat(validate("foo:bar\nbaz")).isEqualTo(SyncExclusionValidation.ContainsLineBreak)
    }

    @Test
    fun `test that invoke returns LeadingOrTrailingWhitespace when the value also holds a colon`() {
        assertThat(validate(" foo:bar "))
            .isEqualTo(SyncExclusionValidation.LeadingOrTrailingWhitespace)
    }

    @Test
    fun `test that invoke returns ContainsPatternSeparator when the value also holds a slash`() {
        assertThat(validate("foo:bar/baz"))
            .isEqualTo(SyncExclusionValidation.ContainsPatternSeparator)
    }

    @Test
    fun `test that invoke returns ContainsPathSeparator when a name value also holds a wildcard`() {
        assertThat(validate("Documents/*.tmp", wildcardsAllowed = false))
            .isEqualTo(SyncExclusionValidation.ContainsPathSeparator)
    }

    private fun validate(
        value: String,
        matchType: SyncExclusionMatchType = SyncExclusionMatchType.SubtreeName,
        wildcardsAllowed: Boolean = false,
    ) = underTest(value, matchType, wildcardsAllowed)
}
