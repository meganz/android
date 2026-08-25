package mega.privacy.android.feature.sharelink.presentation

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The three password-strength levels the Link settings screen shows.
 *
 * The SDK grades a password on five levels, but the design and the webclient both show three. The
 * collapse lives here, in the presentation layer, so [PasswordStrength] and
 * `GetPasswordStrengthUseCase` stay as they are — the finer grading is still there for any other
 * screen that wants it.
 *
 * @property labelRes The label shown under the password field.
 */
internal enum class LinkPasswordStrength(@StringRes val labelRes: Int) {
    Weak(sharedR.string.password_strength_weak),
    Moderate(sharedR.string.password_strength_moderate),
    Strong(sharedR.string.password_strength_strong),
}

/**
 * Collapses the SDK's five grades into the three the design shows, or null when there is no label to
 * show.
 *
 * The two weakest grades fold together and so do the two strongest, leaving MEDIUM as the middle on
 * its own. INVALID is not a grade of strength — the SDK reports it for a password it will not accept
 * at all — so it yields no label rather than being forced into [Weak].
 */
internal fun PasswordStrength.toLinkPasswordStrength(): LinkPasswordStrength? = when (this) {
    PasswordStrength.VERY_WEAK, PasswordStrength.WEAK -> LinkPasswordStrength.Weak
    PasswordStrength.MEDIUM -> LinkPasswordStrength.Moderate
    PasswordStrength.GOOD, PasswordStrength.STRONG -> LinkPasswordStrength.Strong
    PasswordStrength.INVALID -> null
}

/**
 * Whether a link must not be protected with a password of this strength.
 *
 * Matches what the legacy password screen enforced: only the very weakest grade is refused, so
 * anything the SDK grades above it can still be used. Because the SDK grades every password shorter
 * than eight characters as [PasswordStrength.VERY_WEAK], this is in practice an eight-character
 * minimum plus a floor on entropy.
 *
 * A null strength is a grade that has not been read yet, or one the SDK failed to give; neither is
 * evidence the password is weak, so neither blocks.
 */
internal val PasswordStrength?.isTooWeakForLink: Boolean
    get() = this == PasswordStrength.VERY_WEAK || this == PasswordStrength.INVALID
