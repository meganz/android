package mega.privacy.android.feature.sync.domain.repository

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore

/**
 * Reads and writes the `.megaignore` sitting directly under a sync's local root.
 *
 * Both operations take an already-resolved [UriPath] rather than a sync id, so that a caller
 * doing a load-then-save cycle enumerates folder pairs once instead of once per call: resolving
 * a sync id to its local root goes through `getFolderPairs`, which queries the SDK for every
 * pair it returns.
 *
 * Failures are always an
 * [mega.privacy.android.feature.sync.domain.exception.ExclusionsError], never a bare throwable,
 * so callers can tell a lost permission from a refused dotfile from a transient IO error.
 */
internal interface SyncExclusionsRepository {

    /**
     * Reads the exclusions of the sync rooted at [localRoot].
     *
     * A root with no `.megaignore` yet is not an error: it yields an empty [ParsedMegaIgnore],
     * which is what a fresh sync legitimately has. Reading never creates the file.
     */
    suspend fun getExclusions(localRoot: UriPath): Result<ParsedMegaIgnore>

    /**
     * Replaces the exclusions of the sync rooted at [localRoot], creating the file when the sync
     * does not have one yet.
     */
    suspend fun setExclusions(localRoot: UriPath, exclusions: ParsedMegaIgnore): Result<Unit>
}
