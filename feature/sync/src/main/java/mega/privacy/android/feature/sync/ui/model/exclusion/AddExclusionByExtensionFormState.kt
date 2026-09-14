package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * The by-extension body of the Add exclusion screen. Extensions are typed into one field as a
 * comma-separated list, so [value] is the raw text and [extensions] the entries read out of it.
 */
internal data class AddExclusionByExtensionFormState(
    val value: String = "",
    val error: AddExclusionExtensionError? = null,
) {
    /**
     * The extensions the field names, in typing order, trimmed and lower-cased.
     *
     * Case is folded because the SDK matches extensions case-insensitively while de-duplication
     * compares patterns exactly, so "MP3" and "mp3" would otherwise become two rules excluding the
     * same files. A leading dot is left alone for the pattern mapper, which refuses "..mp3";
     * stripping one here would walk that input past it. The empty stretch a trailing separator
     * leaves is dropped rather than reported as a blank extension.
     */
    val extensions: List<String>
        get() = value.split(*SEPARATORS)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    val showsHelper: Boolean
        get() = error == null

    private companion object {
        // a Chinese, Japanese or Arabic keyboard gives its own comma for the comma key, and the
        // helper text is translated to match, so reading only the ASCII one would take a whole
        // list as a single extension
        val SEPARATORS = charArrayOf(',', '，', '、', '،')
    }
}

/** Why the extensions field is showing an error. */
internal sealed interface AddExclusionExtensionError {

    /** One of the entries holds a character an extension cannot carry. */
    data object Invalid : AddExclusionExtensionError

    /** [extension] is already excluded by a rule the file holds. */
    data class Duplicate(val extension: String) : AddExclusionExtensionError
}
