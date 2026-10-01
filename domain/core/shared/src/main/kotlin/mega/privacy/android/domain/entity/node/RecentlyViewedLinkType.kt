package mega.privacy.android.domain.entity.node

/**
 * Domain enum representing the type of a recently viewed MEGA deep link.
 *
 * Used by [ViewedLink] to differentiate between file and folder deep links.
 */
enum class RecentlyViewedLinkType {

    /**
     * A file opened via a MEGA deep link (e.g. `https://mega.nz/file/...`).
     * Persisted as type_id 1 in the recently_viewed_link table.
     */
    FileLink,

    /**
     * A folder opened via a MEGA deep link (e.g. `https://mega.nz/folder/...`).
     * Persisted as type_id 2 in the recently_viewed_link table.
     */
    FolderLink,
}
