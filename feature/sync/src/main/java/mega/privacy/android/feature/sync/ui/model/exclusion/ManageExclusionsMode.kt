package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * Which purpose the Manage exclusions screen is currently serving.
 */
internal enum class ManageExclusionsMode {

    /** Edits the `.megaignore` of an existing folder pair, whose Save writes it. */
    Manage,

    /**
     * Edits a model for a pair that does not exist yet, whose Save hands the model back to the
     * setup screen instead of writing.
     */
    Review,
}
