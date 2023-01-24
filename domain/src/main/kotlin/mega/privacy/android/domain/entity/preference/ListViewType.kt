package mega.privacy.android.domain.entity.preference

/**
 * Enum class that defines the different list view types available for the user to select
 * @property id The assigned ID for a specific list view type
 */
enum class ListViewType(val id: Int) {
    /**
     * All lists will follow a List View structure
     */
    LIST(0),

    /**
     * All lists will follow a Grid View structure
     */
    GRID(1);

    companion object {
        /**
         * Return the corresponding [ListViewType] with a given id
         *
         * @param id The ID to retrieve a specific [ListViewType]
         * @return the [ListViewType], or null if no id matches
         */
        operator fun invoke(id: Int?) = values().firstOrNull { it.id == id }
    }
}