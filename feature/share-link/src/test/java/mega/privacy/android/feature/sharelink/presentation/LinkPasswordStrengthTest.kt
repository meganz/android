package mega.privacy.android.feature.sharelink.presentation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class LinkPasswordStrengthTest {

    @Test
    fun `test that the two weakest grades both collapse to Weak`() {
        assertThat(PasswordStrength.VERY_WEAK.toLinkPasswordStrength())
            .isEqualTo(LinkPasswordStrength.Weak)
        assertThat(PasswordStrength.WEAK.toLinkPasswordStrength())
            .isEqualTo(LinkPasswordStrength.Weak)
    }

    @Test
    fun `test that MEDIUM collapses to Moderate`() {
        assertThat(PasswordStrength.MEDIUM.toLinkPasswordStrength())
            .isEqualTo(LinkPasswordStrength.Moderate)
    }

    @Test
    fun `test that the two strongest grades both collapse to Strong`() {
        assertThat(PasswordStrength.GOOD.toLinkPasswordStrength())
            .isEqualTo(LinkPasswordStrength.Strong)
        assertThat(PasswordStrength.STRONG.toLinkPasswordStrength())
            .isEqualTo(LinkPasswordStrength.Strong)
    }

    @Test
    fun `test that INVALID collapses to no level`() {
        // INVALID is not a grade of strength — the SDK reports it for a password it will not accept
        // at all — so it must not be shown as the weakest level.
        assertThat(PasswordStrength.INVALID.toLinkPasswordStrength()).isNull()
    }

    @ParameterizedTest
    @EnumSource(PasswordStrength::class)
    fun `test that every SDK grade maps without throwing`(strength: PasswordStrength) {
        // Guards the mapping against a new SDK grade being added and silently falling through.
        strength.toLinkPasswordStrength()
    }

    @Test
    fun `test that the screen offers exactly three levels`() {
        assertThat(LinkPasswordStrength.entries).hasSize(3)
    }

    @Test
    fun `test that only the weakest grade is refused for a link`() {
        assertThat(PasswordStrength.VERY_WEAK.isTooWeakForLink).isTrue()
        assertThat(PasswordStrength.INVALID.isTooWeakForLink).isTrue()
    }

    @Test
    fun `test that every grade above the weakest is accepted for a link`() {
        // Legacy parity: the old password screen refused PASSWORD_STRENGTH_VERYWEAK alone.
        assertThat(PasswordStrength.WEAK.isTooWeakForLink).isFalse()
        assertThat(PasswordStrength.MEDIUM.isTooWeakForLink).isFalse()
        assertThat(PasswordStrength.GOOD.isTooWeakForLink).isFalse()
        assertThat(PasswordStrength.STRONG.isTooWeakForLink).isFalse()
    }

    @Test
    fun `test that a grade not read yet does not refuse the password`() {
        val ungraded: PasswordStrength? = null

        assertThat(ungraded.isTooWeakForLink).isFalse()
    }

    @Test
    fun `test that each level carries a distinct label`() {
        val labels = LinkPasswordStrength.entries.map { it.labelRes }

        assertThat(labels).containsNoDuplicates()
    }
}
