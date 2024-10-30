package mega.privacy.android.app.extensions

import java.text.Normalizer

/**
 * Normalize a sequence of char values.
 */
internal fun String.normalize() =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")