package mega.privacy.android.feature.sync.domain.exception

/**
 * Thrown when the storage provider backing a sync root refuses to create a
 * file whose name begins with a dot, which makes `.megaignore` unwritable
 * for that root.
 */
class ProviderRejectedDotfileException :
    RuntimeException("The storage provider rejected creating a dotfile")
