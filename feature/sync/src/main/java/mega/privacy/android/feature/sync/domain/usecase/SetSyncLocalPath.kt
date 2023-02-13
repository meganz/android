package mega.privacy.android.feature.sync.domain.usecase

/**
 * Sets the path to local folder that the user has selected
 */
fun interface SetSyncLocalPath {

    operator fun invoke(path: String)
}

