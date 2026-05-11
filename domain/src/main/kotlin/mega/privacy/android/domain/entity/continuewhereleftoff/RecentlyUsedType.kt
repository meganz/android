package mega.privacy.android.domain.entity.continuewhereleftoff

/**
 * Domain enum representing the type of recently used item in the
 * "Continue Where You Left Off" feature.
 *
 * Each entry maps to a row in the `recently_used_type` database table.
 */
enum class RecentlyUsedType {
    PDF,
    Video,
    Audio,
    TextEditor,
}
