package mega.privacy.android.feature.documentscanner.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.entity.ScanSession
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation of [ScanSessionRepository].
 */
@Singleton
class DefaultScanSessionRepository @Inject constructor() : ScanSessionRepository {

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
        _session.update { session ->
            val pages = session.pages
                .filter { it.id != pageId }
                .mapIndexed { index, page -> page.copy(order = index) }
            session.copy(pages = pages)
        }
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
        _session.update { session ->
            session.copy(
                pages = session.pages.map {
                    if (it.id == pageId) newPage.copy(order = it.order) else it
                }
            )
        }
    }

    override suspend fun clearSession() {
        _session.value = newSession()
    }

    override suspend fun getPages(): List<ScannedPage> = _session.value.pages
}
