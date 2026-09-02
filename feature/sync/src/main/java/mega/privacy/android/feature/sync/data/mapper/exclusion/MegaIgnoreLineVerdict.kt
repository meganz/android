package mega.privacy.android.feature.sync.data.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import java.math.BigInteger

/**
 * What the SDK's filter loader would do with one `.megaignore` line.
 *
 * This is deliberately not the same question as "can this app model the line". A regex rule and a
 * size bound wider than a [Long] are both [Accepted] here while remaining unmodellable, so that
 * nothing mistakes "we cannot edit this" for "the sync is ignoring this file".
 */
internal sealed interface MegaIgnoreLineVerdict {

    /**
     * The SDK parses the line, or skips it as a comment or a blank.
     */
    data object Accepted : MegaIgnoreLineVerdict

    /**
     * The SDK parses the line and it overwrites one of the two size bounds.
     *
     * The value is held as a [BigInteger] because the SDK keeps it unsigned, so a legal bound can
     * exceed [Long.MAX_VALUE]. Such a bound still has to take part in the equal bounds check even
     * though no [mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry.SizeLargerEntry]
     * can carry it.
     */
    data class MovesSizeBound(val bound: SizeBound, val bytes: BigInteger) : MegaIgnoreLineVerdict

    /**
     * The SDK abandons the file at this line.
     */
    data class Rejected(val reason: InvalidLineReason) : MegaIgnoreLineVerdict

    companion object {

        /**
         * The SDK holds both size bounds as unsigned 64-bit values, so this is the largest one it
         * can represent and also the upper bound's starting value.
         */
        val UNSIGNED_64_MAX: BigInteger = BigInteger.ONE.shiftLeft(64) - BigInteger.ONE
    }
}

/**
 * Which end of the size range a directive sets.
 */
internal enum class SizeBound {
    /**
     * `exclude-smaller`, the SDK's `lower`.
     */
    Lower,

    /**
     * `exclude-larger`, the SDK's `upper`.
     */
    Upper,
}
