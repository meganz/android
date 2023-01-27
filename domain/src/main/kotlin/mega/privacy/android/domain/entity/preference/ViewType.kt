package mega.privacy.android.domain.entity.preference

/**
 * Enum class that defines the different view types available for the user to select
 * @property id The assigned ID for a specific view type
 */
enum class ViewType(val id: Int) {
    /**
     * Follow a List View structure
     */
    LIST(0),

    /**
     * Follow a Grid View structure
     */
    GRID(1);

    companion object {
        /**
         * Return the corresponding [ViewType] with a given id
         *
         * @param id The ID to retrieve a specific [ViewType]
         * @return the [ViewType], or null if no id matches
         */
        operator fun invoke(id: Int?) = values().firstOrNull { it.id == id }
    }
}