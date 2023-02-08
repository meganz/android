package mega.privacy.android.domain.usecase.sync

/**
 * Sets the path to local folder that the user has selected
 */
fun interface SetSyncLocalPath {

    operator fun invoke(path: String)
}

