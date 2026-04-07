package mega.privacy.android.domain.entity.recentactions

/**
 * Domain enum representing the type of a recently used item.
 *
 * Each entry maps to a row in the `recently_used_type` database table.
 * This enum is shared across "Continue Where You Left Off" feature.
 */
enum class RecentlyUsedType {

    /**
     * A file opened via a MEGA deep link (e.g. `https://mega.nz/file/...`).
     * Maps to type_id 5 (`file_link`) in the database.
     */
    FileLink,

    /**
     * A folder opened via a MEGA deep link (e.g. `https://mega.nz/folder/...`).
     * Maps to type_id 6 (`folder_link`) in the database.
     */
    FolderLink,
}