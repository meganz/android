package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreFileMapper
import mega.privacy.android.feature.sync.domain.entity.ExclusionsSnapshot
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.exception.ExclusionsError
import mega.privacy.android.feature.sync.domain.repository.SyncExclusionsRepository
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal class SyncExclusionsRepositoryImpl @Inject constructor(
    private val fileGateway: FileGateway,
    private val megaIgnoreFileMapper: MegaIgnoreFileMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncExclusionsRepository {

    override suspend fun getExclusions(localRoot: UriPath): Result<ExclusionsSnapshot> =
        withContext(ioDispatcher) {
            try {
                val file = fileGateway.getChildByName(localRoot, MEGA_IGNORE_FILE_NAME)
                    ?: return@withContext emptyOrUnreachable(localRoot)
                val content = fileGateway.getInputStream(file)
                    ?.use { stream -> stream.readBytes().decodeStrictly() }
                    ?: return@withContext failedRead(localRoot, null)
                Result.success(snapshotOf(localRoot, megaIgnoreFileMapper(content)))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (denial: SecurityException) {
                unreachableRoot(localRoot, denial)
            } catch (error: Exception) {
                failedRead(localRoot, error)
            }
        }

    override suspend fun setExclusions(
        localRoot: UriPath,
        exclusions: ParsedMegaIgnore,
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (!fileGateway.canWriteUri(localRoot)) {
                return@withContext readOnlyOrUnreachable(localRoot)
            }
            val file = (fileGateway.getChildByName(localRoot, MEGA_IGNORE_FILE_NAME)
                ?.let { Result.success(it) }
                ?: createExclusionsFile(localRoot))
                .getOrElse { return@withContext Result.failure(it) }
            val written = fileGateway.saveTextOnContentUri(
                file.value,
                megaIgnoreFileMapper(exclusions),
            )
            if (written) Result.success(Unit) else failedWrite(localRoot, null)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (denial: SecurityException) {
            unreachableRoot(localRoot, denial)
        } catch (error: Exception) {
            failedWrite(localRoot, error)
        }
    }

    /**
     * Answers with [answer] while the sync root is still there, and with
     * [ExclusionsError.NoPermission] once it is not.
     *
     * Every failure this class can report other than an unreachable root has to be qualified this
     * way, because the calls that produce them cannot tell the two apart: [FileGateway]'s child
     * lookup and file creation both answer `null` for a revoked grant, an unmounted card and a
     * deleted folder just as they do for a folder that is simply empty or hostile to dotfiles.
     *
     * A sync root is legitimately either a bare filesystem path or a content URI, so the probe
     * must be the [UriPath] overload of `canReadUri` - the only one that resolves a missing
     * scheme. Probing a bare path as a plain string reports every such root as unreadable, which
     * would turn an absent file into a spurious error.
     */
    private suspend fun <T> ifRootReachable(
        localRoot: UriPath,
        answer: suspend () -> Result<T>,
    ): Result<T> = if (fileGateway.canReadUri(localRoot)) {
        answer()
    } else {
        unreachableRoot(localRoot, null)
    }

    /**
     * Distinguishes a sync that has no exclusions yet from one whose root we can no longer see.
     *
     * Reporting an unreachable root as "no exclusions" would show an empty list for a sync that
     * has rules, and the next save would then wipe them.
     */
    private suspend fun emptyOrUnreachable(localRoot: UriPath): Result<ExclusionsSnapshot> =
        ifRootReachable(localRoot) {
            Result.success(snapshotOf(localRoot, ParsedMegaIgnore(entries = emptyList())))
        }

    /**
     * Pairs what was read with whether it could be written back.
     *
     * The probe is the sync root rather than the file, because a sync that has no `.megaignore`
     * yet needs to know whether one could be created there, and a root is what a SAF grant is
     * taken over anyway.
     *
     * A probe that throws answers false rather than failing the read: the rules are already in
     * hand at this point, and hiding them over a question about saving them would lose more than
     * it protects.
     */
    private suspend fun snapshotOf(localRoot: UriPath, exclusions: ParsedMegaIgnore) =
        ExclusionsSnapshot(
            exclusions = exclusions,
            isWritable = try {
                fileGateway.canWriteUri(localRoot)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Timber.w(error, "Could not probe write access to $localRoot")
                false
            },
        )

    /**
     * Creates the file for a sync that does not have one yet.
     *
     * A provider that refuses a leading dot may strip it rather than fail, and
     * [FileGateway.createChildFileSync] answers with that mangled child once its own rename
     * fallback has also been refused. Comparing the name it actually holds is what tells the two
     * apart; the mangled child is then removed, because leaving it behind would have the sync
     * engine upload a stray file and every retry would add another beside it.
     */
    private suspend fun createExclusionsFile(localRoot: UriPath): Result<UriPath> {
        val created = fileGateway.createChildFileSync(localRoot, MEGA_IGNORE_FILE_NAME, false)
            ?: return ifRootReachable(localRoot) { rejectedDotfile(localRoot) }
        return when (fileGateway.getFileNameFromUri(created.value)) {
            MEGA_IGNORE_FILE_NAME -> Result.success(created)
            null -> failedWrite(localRoot, null)
            else -> {
                fileGateway.deleteIfItIsAFileSync(created)
                rejectedDotfile(localRoot)
            }
        }
    }

    /**
     * Decodes UTF-8 without substituting replacement characters.
     *
     * A lenient decode turns bytes this app cannot read into U+FFFD, and the next save writes
     * those substitutions back over the user's rules. Refusing to read a file we cannot
     * reproduce faithfully is the safer half of that trade.
     */
    private fun ByteArray.decodeStrictly(): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(this))
        .toString()

    private fun <T> unreachableRoot(localRoot: UriPath, cause: SecurityException?): Result<T> {
        Timber.w(cause, "Cannot reach the sync root $localRoot")
        return Result.failure(ExclusionsError.NoPermission(cause))
    }

    private fun <T> failedRead(localRoot: UriPath, cause: Throwable?): Result<T> {
        Timber.w(cause, "Could not read the exclusions file under $localRoot")
        return Result.failure(ExclusionsError.ReadFailed(cause))
    }

    /**
     * Tells a root that only grants reading from one that has gone altogether.
     *
     * Both answer false to the write probe: a revoked grant, a deleted folder and an unmounted
     * card resolve to no document at all, which reads as not writable. Reporting those as read
     * only would have the user shown their rules as uneditable while the read path, on the very
     * same state, asks them to re-pick the folder.
     */
    private suspend fun <T> readOnlyOrUnreachable(localRoot: UriPath): Result<T> =
        ifRootReachable(localRoot) { noWriteAccess(localRoot) }

    private fun <T> noWriteAccess(localRoot: UriPath): Result<T> {
        Timber.w("The sync root $localRoot is readable but not writable")
        return Result.failure(ExclusionsError.NoWriteAccess())
    }

    private fun <T> failedWrite(localRoot: UriPath, cause: Throwable?): Result<T> {
        Timber.w(cause, "Could not write the exclusions file under $localRoot")
        return Result.failure(ExclusionsError.WriteFailed(cause))
    }

    private fun <T> rejectedDotfile(localRoot: UriPath): Result<T> {
        Timber.w("The provider backing $localRoot rejected creating $MEGA_IGNORE_FILE_NAME")
        return Result.failure(ExclusionsError.ProviderRejectedDotfile())
    }

    private companion object {
        const val MEGA_IGNORE_FILE_NAME = ".megaignore"
    }
}
