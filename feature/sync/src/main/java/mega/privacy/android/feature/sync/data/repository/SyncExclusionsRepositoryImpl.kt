package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreFileMapper
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

    override suspend fun getExclusions(localRoot: UriPath): Result<ParsedMegaIgnore> =
        withContext(ioDispatcher) {
            try {
                val file = fileGateway.getChildByName(localRoot, MEGA_IGNORE_FILE_NAME)
                    ?: return@withContext emptyOrUnreachable(localRoot)
                val content = fileGateway.getInputStream(file)
                    ?.use { stream -> stream.readBytes().decodeStrictly() }
                    ?: return@withContext failedRead(localRoot, null)
                Result.success(megaIgnoreFileMapper(content))
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
     * Distinguishes a sync that has no exclusions yet from one whose root we can no longer see.
     *
     * [FileGateway.getChildByName] answers `null` for both, swallowing a revoked grant, an
     * unmounted card and a deleted folder alike. Reporting those as "no exclusions" would show an
     * empty list for a sync that has rules, and the next save would then wipe them.
     *
     * A sync root is legitimately either a bare filesystem path or a content URI, so the probe
     * must be the [UriPath] overload of [FileGateway.canReadUri] - the only one that resolves a
     * missing scheme. Probing a bare path as a plain string reports every such root as
     * unreadable, which would turn an absent file into a spurious error.
     */
    private suspend fun emptyOrUnreachable(localRoot: UriPath): Result<ParsedMegaIgnore> =
        if (fileGateway.canReadUri(localRoot)) {
            Result.success(ParsedMegaIgnore(entries = emptyList()))
        } else {
            unreachableRoot(localRoot, null)
        }

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
            ?: return if (fileGateway.canReadUri(localRoot)) {
                rejectedDotfile(localRoot)
            } else {
                unreachableRoot(localRoot, null)
            }
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
