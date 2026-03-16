package mega.privacy.android.feature.pdfviewer.search

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import javax.inject.Inject

/**
 * Factory for creating [PdfSearchEngine] instances.
 *
 * Allows the ViewModel to obtain a search engine without depending on the concrete
 * [PdfSearchEngineImpl] class, enabling unit tests to inject a fake implementation.
 */
interface PdfSearchEngineFactory {

    /**
     * Creates a new search engine instance.
     *
     * @param context Android context (used by the real implementation for PdfiumCore)
     * @return A new [PdfSearchEngine] instance
     */
    fun create(context: Context): PdfSearchEngine
}

/**
 * Default implementation that creates a [PdfSearchEngineImpl] using the given context.
 */
internal class DefaultPdfSearchEngineFactory @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : PdfSearchEngineFactory {

    override fun create(context: Context): PdfSearchEngine =
        PdfSearchEngineImpl(context, dispatcher = defaultDispatcher)
}
