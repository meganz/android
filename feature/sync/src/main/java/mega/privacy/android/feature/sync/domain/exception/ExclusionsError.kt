package mega.privacy.android.feature.sync.domain.exception

/**
 * Why a sync's `.megaignore` could not be read or written.
 *
 * These are deliberately distinct rather than one generic failure: each one asks something
 * different of the user. A lost SAF grant needs the folder re-picking, a provider that refuses
 * dotfiles cannot host a sync at all, and a transient IO error is worth simply retrying.
 *
 * An absent file is not represented here. A sync that has never been given exclusions
 * legitimately has none, so reading one yields an empty model rather than a failure.
 */
sealed class ExclusionsError(message: String, cause: Throwable?) : Exception(message, cause) {

    /**
     * The file is there but its content could not be read.
     */
    class ReadFailed(cause: Throwable? = null) :
        ExclusionsError("Could not read the exclusions file", cause)

    /**
     * The sync root itself cannot be reached, so neither read nor write can proceed until the
     * user restores it. A revoked SAF grant is the common cause, but an unmounted card or a
     * folder deleted outside the app land here too.
     */
    class NoPermission(cause: Throwable? = null) :
        ExclusionsError("No longer permitted to access the exclusions file", cause)

    /**
     * The storage provider backing the sync root refuses to hold a file whose name begins with a
     * dot, which makes `.megaignore` unwritable for that root. Retrying cannot help.
     */
    class ProviderRejectedDotfile(cause: Throwable? = null) :
        ExclusionsError("The storage provider rejected creating a dotfile", cause)

    /**
     * The sync root can be read but not written, so the exclusions can be shown and never saved.
     * A grant taken with read only access, or a provider that hands out read only trees, is the
     * common cause.
     *
     * Distinct from [NoPermission], which says the root cannot be reached at all: this one leaves
     * the rules readable, so they are still worth showing.
     */
    class NoWriteAccess(cause: Throwable? = null) :
        ExclusionsError("No write access to the exclusions file", cause)

    /**
     * The file was reachable but the new content could not be written to it.
     */
    class WriteFailed(cause: Throwable? = null) :
        ExclusionsError("Could not write the exclusions file", cause)
}
