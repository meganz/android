package mega.privacy.android.domain.entity.preference

/**
 * Scope of the sorting order preference chosen by the user
 * @property id The assigned ID for a specific preference
 */
enum class SortingPreference(val id: Int) {
    /**
     * Each folder keeps its own sorting order
     */
    PerFolder(0),

    /**
     * The same sorting order is used for all folders
     */
    AllFolders(1);

    companion object {
        /**
         * Return the corresponding [SortingPreference] with a given id
         *
         * @param id The ID to retrieve a specific [SortingPreference]
         * @return the [SortingPreference], or null if no id matches
         */
        operator fun invoke(id: Int?) = values().firstOrNull { it.id == id }
    }
}
