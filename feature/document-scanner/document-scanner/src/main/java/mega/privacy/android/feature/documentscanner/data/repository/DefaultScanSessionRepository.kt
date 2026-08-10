package mega.privacy.android.feature.documentscanner.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.documentscanner.data.capture.DocumentImageStorage
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.entity.ScanSession
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation of [ScanSessionRepository]. Page metadata lives in
 * memory; the backing image files are deleted through [DocumentImageStorage] as
 * pages are removed / replaced / the session is cleared, so storage does not leak.
 */
@Singleton
internal class DefaultScanSessionRepository @Inject constructor(
    private val documentImageStorage: DocumentImageStorage,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ScanSessionRepository {

    private val _session = MutableStateFlow(newSession())

    private fun newSession() = ScanSession(
        id = UUID.randomUUID().toString(),
        pages = emptyList(),
        captureMode = CaptureMode.AUTO,
        createdAt = System.currentTimeMillis(),
    )

    override fun getSession(): Flow<ScanSession> = _session.asStateFlow()

    override suspend fun addPage(page: ScannedPage) {
        _session.update { session ->
            val pages = session.pages + page.copy(order = session.pages.size)
            session.copy(pages = pages)
        }
    }

    override suspend fun removePage(pageId: String) {
        var removed: ScannedPage? = null
        _session.update { session ->
            removed = session.pages.firstOrNull { it.id == pageId }
            val pages = session.pages
                .filter { it.id != pageId }
                .mapIndexed { index, page -> page.copy(order = index) }
            session.copy(pages = pages)
        }
        removed?.let { deleteFiles(it) }
    }

    override suspend fun reorderPages(fromIndex: Int, toIndex: Int) {
        _session.update { session ->
            val pages = session.pages
            if (fromIndex !in pages.indices || toIndex !in pages.indices) return@update session
            val mutable = pages.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)
            session.copy(
                pages = mutable.mapIndexed { index, page -> page.copy(order = index) }
            )
        }
    }

    override suspend fun replacePage(pageId: String, newPage: ScannedPage) {
        var replaced: ScannedPage? = null
        _session.update { session ->
            replaced = session.pages.firstOrNull { it.id == pageId }
            session.copy(
                pages = session.pages.map {
                    if (it.id == pageId) newPage.copy(order = it.order) else it
                }
            )
        }
        replaced?.let { deleteFiles(it) }
    }

    override suspend fun clearSession() {
        _session.value = newSession()
        withContext(ioDispatcher + NonCancellable) { documentImageStorage.deleteAll() }
    }

    override suspend fun getPages(): List<ScannedPage> = _session.value.pages

    // NonCancellable: the page is already gone from state, so the file cleanup must
    // still run even if the caller's scope is cancelled — otherwise the file leaks.
    private suspend fun deleteFiles(page: ScannedPage) =
        withContext(ioDispatcher + NonCancellable) {
            documentImageStorage.delete(page.imageUri)
            documentImageStorage.delete(page.thumbnailUri)
        }
}
