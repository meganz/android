package mega.privacy.android.feature.videoeditor.presentation.editor.export

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.container.Mp4LocationData
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultMuxer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.feature.videoeditor.domain.entity.VideoMetadata
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.buildMediaItem
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.composeEffects
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import java.io.File
import javax.inject.Inject

/**
 * Wraps a Media3 [Transformer] export. Combines the trim ([buildMediaItem]) and
 * the per-tool effect contributions ([composeEffects]), runs the transformer to
 * a file in the app cache, and emits progress then the output URI as a stream of
 * [ExportEvent]s. The source's GPS location is re-attached via
 * [MetadataMuxerFactory].
 *
 * Collection drives the encode: cancelling the collector stops the transformer
 * (on the Main thread, via the producer's `finally`), and a failure surfaces as
 * the flow throwing. The output is left in the cache for the caller (the upload
 * flow consumes it and is responsible for removing it).
 */
@UnstableApi
class VideoExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Encodes [state] (with [tools]) to a cache file, emitting progress then the
     * output URI. [sourceMetadata] — read once by the caller when the source was
     * loaded — supplies the GPS location re-embedded into the output; pass null
     * to leave the clip anonymous.
     */
    fun export(
        state: EditorState,
        tools: List<EditorTool>,
        sourceMetadata: VideoMetadata?,
    ): Flow<ExportEvent> = channelFlow {
        // Clear any leftover output from a prior run off the main thread
        val outputFile = withContext(ioDispatcher) {
            File(context.cacheDir, EXPORT_FILE_NAME).apply { delete() }
        }
        val completion = CompletableDeferred<Unit>()

        val transformer = withContext(mainDispatcher) {
            val builder = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        completion.complete(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        completion.completeExceptionally(exportException)
                    }
                })

            locationEntriesFor(sourceMetadata).takeIf { it.isNotEmpty() }?.let { entries ->
                builder.setMuxerFactory(MetadataMuxerFactory(DefaultMuxer.Factory(), entries))
            }
            val transformer = builder.build()

            val editedMediaItem = EditedMediaItem.Builder(buildMediaItem(state))
                .setEffects(composeEffects(state, tools))
                .build()

            transformer.start(editedMediaItem, outputFile.absolutePath)
            transformer
        }

        val progressJob = launch(mainDispatcher) {
            val holder = ProgressHolder()
            while (isActive && !completion.isCompleted) {
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    trySend(ExportEvent.Progress(holder.progress.coerceIn(0, 100)))
                }
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }

        try {
            completion.await()
            send(ExportEvent.Progress(100))
            send(ExportEvent.Completed(Uri.fromFile(outputFile)))
        } finally {
            progressJob.cancel()
            // Stop the transformer on every exit path. It must be cancelled on
            // the thread it was created on (Main), under NonCancellable so it
            // still runs when this coroutine is itself being cancelled.
            withContext(NonCancellable + mainDispatcher) { transformer.cancel() }
        }
    }

    /**
     * The muxer-writable metadata entries derived from the source — currently
     * just the GPS location, which the default muxer maps onto
     * `MediaMuxer.setLocation`.
     */
    private fun locationEntriesFor(metadata: VideoMetadata?) = buildList {
        val latitude = metadata?.latitude
        val longitude = metadata?.longitude
        if (latitude != null && longitude != null) {
            add(Mp4LocationData(latitude, longitude))
        }
    }

    private companion object {
        const val EXPORT_FILE_NAME = "video-editor-export.mp4"
        const val PROGRESS_POLL_INTERVAL_MS = 150L
    }
}
