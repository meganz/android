package mega.privacy.android.feature.sync.domain.entity

/**
 * Why the SDK refuses a `.megaignore` line, and with it the whole file.
 *
 * The SDK's loader abandons the file on the first line it cannot parse, so every reason here is
 * fatal to the file rather than to the one line: none of the rules take effect until the line is
 * corrected. That is why a reason is worth surfacing at all.
 *
 * These are the rejections that can be recognised from the line's text. A malformed regular
 * expression body is refused by the SDK too, but only when it builds the matcher, and this app
 * deliberately does not evaluate regex bodies, so such a line is not reported here.
 */
enum class InvalidLineReason {
    /**
     * The line does not begin with `+` or `-`. The class letter is mandatory and has to be the
     * line's very first byte, so a line indented with whitespace lands here as well.
     */
    MissingExclusionClass,

    /**
     * The line begins with whitespace and then a `#`. The SDK reads a comment marker only at the
     * first byte, so the line is a syntax error rather than the comment it looks like.
     */
    CommentNotAtLineStart,

    /**
     * A character that is neither a valid flag nor the `:` separator turned up in the flag region
     * between the class letter and the separator.
     *
     * The flags are read once each and only in the order target, type, strategy, so this covers an
     * unknown flag letter, a duplicated one and the right ones in the wrong order — and equally a
     * space or a stray word, since anything the flag reader does not consume has to be the
     * separator. The line does have a `:` somewhere after it; a line with none at all is
     * [MissingSeparator].
     */
    UnexpectedFlagCharacter,

    /**
     * The line has a class letter but no `:` at all, so it has no pattern.
     */
    MissingSeparator,

    /**
     * The line begins with `e`, which routes it to the size directives, but it names neither
     * `exclude-smaller` nor `exclude-larger`.
     */
    UnknownDirective,

    /**
     * A size directive's value is not a bare count of bytes with an optional `k`, `m`, `g` or `t`
     * suffix. A sign, a fraction, an unknown suffix and trailing junk all land here.
     */
    MalformedSizeValue,

    /**
     * A size directive's value is followed by whitespace and no unit suffix. The SDK reads the
     * character after the digits as the suffix and only skips whitespace once it has one, so a
     * trailing space is a syntax error while `100k ` is not.
     */
    IllegalTrailingWhitespace,

    /**
     * A size directive's value does not fit the unsigned 64-bit limit the SDK holds it in, either
     * as written or once its unit suffix has been applied.
     */
    SizeValueOutOfRange,

    /**
     * This line makes the two size bounds equal, which the SDK rejects.
     *
     * The check runs as each line is applied rather than on the bounds the file ends up with, so
     * the offending line is this one even when a later line would have moved a bound apart again.
     * Both bounds start wide open — `0` and the unsigned 64-bit maximum — so a lone
     * `exclude-larger:0` is enough to trigger it.
     */
    EqualSizeBounds,
}
