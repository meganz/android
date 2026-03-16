package mega.privacy.android.feature.pdfviewer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.pdfviewer.search.DefaultPdfSearchEngineFactory
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngineFactory

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PdfViewerModule {

    @Binds
    abstract fun bindPdfSearchEngineFactory(impl: DefaultPdfSearchEngineFactory): PdfSearchEngineFactory
}
