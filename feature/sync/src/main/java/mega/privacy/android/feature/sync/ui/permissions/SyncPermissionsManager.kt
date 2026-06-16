package mega.privacy.android.feature.sync.ui.permissions

/**
 * Transitional alias kept while call sites migrate to
 * [mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager].
 *
 * The implementation was moved to :shared:sync so the cloud explorer can reuse it. Existing
 * imports of this type keep compiling through this alias; they are migrated (and this file
 * removed) in a follow-up clean-up.
 */
typealias SyncPermissionsManager =
        mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
