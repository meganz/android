package mega.privacy.android.feature.videoeditor.presentation.editor.export

import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.Muxer

/**
 * A [Muxer.Factory] decorator that injects extra [Metadata.Entry]s into every
 * muxer it creates, so the exported file keeps "library" metadata the
 * transformer would otherwise drop — e.g. the source's GPS location.
 *
 * The entries are added right after the delegate muxer is created, before any
 * track is added or the underlying writer is started, which is when the default
 * `FrameworkMuxer` (backed by `MediaMuxer.setLocation`) expects them. Entry
 * types the default muxer doesn't understand are silently ignored.
 */
@UnstableApi
internal class MetadataMuxerFactory(
    private val delegate: Muxer.Factory,
    private val metadataEntries: List<Metadata.Entry>,
) : Muxer.Factory {

    override fun create(path: String): Muxer {
        val muxer = delegate.create(path)
        metadataEntries.forEach(muxer::addMetadataEntry)
        return muxer
    }

    override fun getSupportedSampleMimeTypes(trackType: Int) =
        delegate.getSupportedSampleMimeTypes(trackType)

    override fun supportsWritingNegativeTimestampsInEditList() =
        delegate.supportsWritingNegativeTimestampsInEditList()
}
