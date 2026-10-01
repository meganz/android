package mega.privacy.android.domain.entity.preference

/**
 * Scope of the view mode preference chosen by the user
 * @property id The assigned ID for a specific preference
 */
enum class ViewModePreference(val id: Int) {
    /**
     * Each folder keeps its own view mode
     */
    PerFolder(0),

    /**
     * The same view mode is used for all folders
     */
    AllFolders(1);

    companion object {
        /**
         * Return the corresponding [ViewModePreference] with a given id
         *
         * @param id The ID to retrieve a specific [ViewModePreference]
         * @return the [ViewModePreference], or null if no id matches
         */
        operator fun invoke(id: Int?) = values().firstOrNull { it.id == id }
    }
}
