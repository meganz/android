package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionDisplay
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionPatternMapperTest {

    private val underTest = SyncExclusionPatternMapper()

    private val defaultDisplays = listOf(
        "Thumbs.db" to condition(SyncExclusionCondition.MatchingExactly, "Thumbs.db"),
        "desktop.ini" to condition(SyncExclusionCondition.MatchingExactly, "desktop.ini"),
        "~*" to condition(SyncExclusionCondition.BeginningWith, "~"),
        ".*" to condition(SyncExclusionCondition.BeginningWith, "."),
        "*~.*" to condition(SyncExclusionCondition.Containing, "~."),
        "*.crdownload" to SyncExclusionDisplay.Extension("crdownload"),
        "*.sb-????????-??????" to SyncExclusionDisplay.Extension("sb-????????-??????"),
        "*.tmp" to SyncExclusionDisplay.Extension("tmp"),
    )

    private fun provideDefaultDisplays(): Stream<Arguments> = defaultDisplays
        .map { (pattern, display) -> Arguments.of(pattern, display) }
        .stream()

    private fun provideDefaultPatterns(): Stream<Arguments> = MegaIgnoreDefaults.RULES
        .map { Arguments.of(it.pattern) }
        .stream()

    @Test
    fun `test that the display cases cover every default rule`() {
        assertThat(defaultDisplays.map { it.first })
            .containsExactlyElementsIn(MegaIgnoreDefaults.RULES.map { it.pattern })
            .inOrder()
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("provideDefaultDisplays")
    fun `test that invoke derives the display of every default pattern`(
        pattern: String,
        expected: SyncExclusionDisplay,
    ) {
        assertThat(underTest(pattern)).isEqualTo(expected)
    }

    @Test
    fun `test that the default set splits into three extensions and five conditions`() {
        val displays = MegaIgnoreDefaults.RULES.map { underTest(it.pattern) }

        assertThat(displays.filterIsInstance<SyncExclusionDisplay.Extension>())
            .containsExactly(
                SyncExclusionDisplay.Extension("crdownload"),
                SyncExclusionDisplay.Extension("sb-????????-??????"),
                SyncExclusionDisplay.Extension("tmp"),
            )
        assertThat(displays.filterIsInstance<SyncExclusionDisplay.Condition>()).hasSize(5)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDefaultPatterns")
    fun `test that invoke round trips every default rule pattern`(pattern: String) {
        assertThat(rebuild(underTest(pattern))).isEqualTo(pattern)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = ["*", "**", "*a*b*", "a*b", "a?b", "?", "*..mp3", "*.", "*.tmp ", "draft*",
            "*draft*", "report.pdf", "*.mp4", "*.tar.gz", ".*", "~*", "*~.*", "x", "*.a*b"],
    )
    fun `test that invoke round trips every pattern shape`(pattern: String) {
        assertThat(rebuild(underTest(pattern))).isEqualTo(pattern)
    }

    @Test
    fun `test that invoke derives beginning with when only the last character is a star`() {
        assertThat(underTest("draft*"))
            .isEqualTo(condition(SyncExclusionCondition.BeginningWith, "draft"))
    }

    @Test
    fun `test that invoke derives ending with when only the first character is a star`() {
        assertThat(underTest("*report"))
            .isEqualTo(condition(SyncExclusionCondition.EndingWith, "report"))
    }

    @Test
    fun `test that invoke prefers the chip shape over ending with`() {
        assertThat(underTest("*.mp4")).isEqualTo(SyncExclusionDisplay.Extension("mp4"))
    }

    @Test
    fun `test that invoke derives containing when the pattern is starred at both ends`() {
        assertThat(underTest("*draft*"))
            .isEqualTo(condition(SyncExclusionCondition.Containing, "draft"))
    }

    @Test
    fun `test that invoke derives matching exactly when the pattern holds no wildcard`() {
        assertThat(underTest("report.pdf"))
            .isEqualTo(condition(SyncExclusionCondition.MatchingExactly, "report.pdf"))
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["*", "**", "*a*b*", "a*b", "a?b", "?", "*?"])
    fun `test that invoke falls back to matching wildcard when no condition fits`(pattern: String) {
        assertThat(underTest(pattern))
            .isEqualTo(condition(SyncExclusionCondition.MatchingWildcard, pattern))
    }

    @Test
    fun `test that invoke falls back to matching wildcard when the pattern is empty`() {
        assertThat(underTest("")).isEqualTo(condition(SyncExclusionCondition.MatchingWildcard, ""))
    }

    @Test
    fun `test that invoke reads a doubly dotted extension as a condition`() {
        assertThat(underTest("*..mp3"))
            .isEqualTo(condition(SyncExclusionCondition.EndingWith, "..mp3"))
    }

    @Test
    fun `test that invoke reads an empty extension as a condition`() {
        assertThat(underTest("*.")).isEqualTo(condition(SyncExclusionCondition.EndingWith, "."))
    }

    @Test
    fun `test that invoke keeps whitespace the SDK does not strip`() {
        assertThat(underTest("*.tmp "))
            .isEqualTo(SyncExclusionDisplay.Extension("tmp "))
    }

    @Test
    fun `test that invoke keeps whitespace the SDK does not strip in a condition value`() {
        assertThat(underTest("*draft *"))
            .isEqualTo(condition(SyncExclusionCondition.Containing, "draft "))
    }

    @Test
    fun `test that invoke appends a star for beginning with`() {
        assertThat(underTest(SyncExclusionCondition.BeginningWith, "draft")).isEqualTo("draft*")
    }

    @Test
    fun `test that invoke prepends a star for ending with`() {
        assertThat(underTest(SyncExclusionCondition.EndingWith, ".mp4")).isEqualTo("*.mp4")
    }

    @Test
    fun `test that invoke wraps the value in stars for containing`() {
        assertThat(underTest(SyncExclusionCondition.Containing, "draft")).isEqualTo("*draft*")
    }

    @Test
    fun `test that invoke returns the value verbatim for matching exactly`() {
        assertThat(underTest(SyncExclusionCondition.MatchingExactly, "Thumbs.db"))
            .isEqualTo("Thumbs.db")
    }

    @Test
    fun `test that invoke returns the value verbatim for matching wildcard`() {
        assertThat(underTest(SyncExclusionCondition.MatchingWildcard, "*.sb-????????-??????"))
            .isEqualTo("*.sb-????????-??????")
    }

    @Test
    fun `test that invoke does not trim the value it builds a pattern from`() {
        assertThat(underTest(SyncExclusionCondition.EndingWith, ".tmp "))
            .isEqualTo("*.tmp ")
    }

    @Test
    fun `test that only matching wildcard allows wildcards`() {
        val allowed = SyncExclusionCondition.entries.filter { it.allowsWildcards }

        assertThat(allowed).containsExactly(SyncExclusionCondition.MatchingWildcard)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SyncExclusionCondition::class)
    fun `test that a wildcard free value round trips through every condition`(
        condition: SyncExclusionCondition,
    ) {
        val pattern = underTest(condition, "draft")

        assertThat(rebuild(underTest(pattern))).isEqualTo(pattern)
    }

    @Test
    fun `test that extensionPattern turns a typed extension into a glob`() {
        assertThat(underTest.extensionPattern("mp3")).isEqualTo("*.mp3")
    }

    @Test
    fun `test that extensionPattern strips the leading dot so a dotted value cannot duplicate a rule`() {
        assertThat(underTest.extensionPattern(".mp3"))
            .isEqualTo(underTest.extensionPattern("mp3"))
    }

    @Test
    fun `test that extensionPattern keeps the inner dots of a compound extension`() {
        assertThat(underTest.extensionPattern("tar.gz")).isEqualTo("*.tar.gz")
    }

    @Test
    fun `test that extensionPattern does not trim the typed extension`() {
        assertThat(underTest.extensionPattern("mp3 ")).isEqualTo("*.mp3 ")
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", ".", "..", "..mp3", "...tar.gz"])
    fun `test that extensionPattern returns null when no chip could carry the value back`(
        extension: String,
    ) {
        assertThat(underTest.extensionPattern(extension)).isNull()
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["mp3", ".mp3", "tar.gz", "sb-????????-??????", "mp3 ", "a*b"])
    fun `test that extensionPattern round trips through invoke`(extension: String) {
        val pattern = requireNotNull(underTest.extensionPattern(extension))

        assertThat(underTest(pattern))
            .isEqualTo(SyncExclusionDisplay.Extension(extension.removePrefix(".")))
    }

    private fun condition(
        condition: SyncExclusionCondition,
        value: String,
    ) = SyncExclusionDisplay.Condition(condition, value)

    private fun rebuild(display: SyncExclusionDisplay): String? = when (display) {
        is SyncExclusionDisplay.Extension -> underTest.extensionPattern(display.extension)
        is SyncExclusionDisplay.Condition -> underTest(display.condition, display.value)
    }
}
