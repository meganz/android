package mega.privacy.android.domain.entity.account

/**
 * The user action that raised a storage quota warning. Each trigger carries its own daily
 * allowance.
 */
enum class StorageQuotaWarningTrigger {

    /**
     * The user logged in, or the account was reloaded.
     */
    LoginOrReload,

    /**
     * The user finished uploading files successfully.
     */
    UploadSuccess,
}
