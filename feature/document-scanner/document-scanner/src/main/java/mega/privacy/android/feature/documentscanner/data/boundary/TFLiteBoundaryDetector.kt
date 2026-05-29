package mega.privacy.android.feature.documentscanner.data.boundary

import mega.privacy.android.feature.documentscanner.domain.boundary.DocumentBoundaryDetector
import mega.privacy.android.feature.documentscanner.domain.entity.DetectionResult
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Document boundary detector backed by a TFLite UNet (midv-500-models, MIT).
 *
 * Pipeline:
 *  1. Rotate the analysis frame (Y plane) to display orientation
 *  2. Downsample + replicate Y across RGB into the [INPUT_SIZE]×[INPUT_SIZE]
 *     float input tensor with ImageNet normalisation
 *  3. Run TFLite inference → mask of shape [1, [INPUT_SIZE], [INPUT_SIZE], 1]
 *  4. Threshold the mask, find the largest connected foreground component
 *  5. Pick the four extreme corners of that component (TL = min(x+y), etc.)
 *  6. Convert pixel coords back to normalised [0,1] in the original frame space
 *
 * The model file is downloaded on first scanner entry and cached by
 * [ScannerModelProvider] — it is not bundled in the APK. Callers must ensure
 * [ScannerModelProvider.cachedModelFile] returns non-null before invoking
 * [detect]; the detector treats a present model file as a precondition.
 *
 * Inference uses the LiteRT GPU delegate when supported (≈ 20–25 ms / frame
 * on a Pixel 6) and falls back to CPU with [NUM_THREADS] threads otherwise.
 *
 * **Threading.** [detect] is blocking (model mmap on first call, then
 * inference) and is **not** thread-safe: it writes shared reusable buffers
 * ([inputBuffer], [outputBuffer], [outputTensor]) with no synchronisation, by
 * design, to avoid per-frame allocations. Callers must invoke it from a single
 * background thread — in this feature that is CameraX's `ImageAnalysis`
 * executor — and never from the main thread or concurrently. The interpreter
 * is loaded on the first [detect] call (not at injection time, because the
 * Hilt-managed singleton is constructed before the model is guaranteed to be
 * on disk) and held for the process lifetime.
 */
@Singleton
class TFLiteBoundaryDetector @Inject constructor(
    private val modelProvider: ScannerModelProvider,
    private val grayFrameRotator: GrayFrameRotator,
    private val largestComponentFinder: LargestComponentFinder,
    private val boundaryGuard: BoundaryGuard,
) : DocumentBoundaryDetector {

    // Lazy-loaded on the first detect() call rather than `by lazy { ... }`:
    // the singleton is constructed before the model is on disk, and a
    // nullable var leaves room for a future lifecycle-owned release()/reload.
    private var interpreter: Interpreter? = null

    // Held to keep the JNI delegate alive for the interpreter's lifetime.
    private var gpuDelegate: GpuDelegate? = null

    private val inputBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())

    private val outputBuffer: FloatArray = FloatArray(INPUT_SIZE * INPUT_SIZE)
    private val outputTensor: Array<Array<Array<FloatArray>>> = Array(1) {
        Array(INPUT_SIZE) { Array(INPUT_SIZE) { FloatArray(1) } }
    }

    /**
     * Returns the ready interpreter, lazy-loading it on the first call.
     *
     * Returns null only if interpreter creation itself fails (e.g. the cached
     * file is corrupt or the GPU delegate construction throws repeatedly).
     * The presence of the model file is a precondition: callers must call
     * [ScannerModelProvider.ensureModelReady] before invoking [detect].
     */
    private fun obtainInterpreter(): Interpreter? {
        interpreter?.let { return it }
        val modelFile = checkNotNull(modelProvider.cachedModelFile()) {
            "Model file missing — call ensureModelReady() before detect()"
        }
        return loadInterpreter(modelFile)?.also { interpreter = it }
    }

    private fun loadInterpreter(modelFile: File): Interpreter? = try {
        val options = Interpreter.Options()
        val gpuOk = tryAddGpuDelegate(options)
        if (!gpuOk) {
            options.setNumThreads(NUM_THREADS)
        }
        Interpreter(loadModelFile(modelFile), options).also {
            Timber.d("[DocScanner][load] TFLite interpreter loaded (${if (gpuOk) "GPU" else "CPU x$NUM_THREADS"})")
        }
    } catch (e: Exception) {
        Timber.e(e, "[DocScanner][load] Failed to load TFLite model")
        // If the delegate was attached but Interpreter() threw, release it
        // here so the JNI handle doesn't leak.
        gpuDelegate?.close()
        gpuDelegate = null
        null
    }

    private fun tryAddGpuDelegate(options: Interpreter.Options): Boolean = try {
        val compatList = CompatibilityList()
        if (!compatList.isDelegateSupportedOnThisDevice) {
            Timber.d("[DocScanner][load] GPU delegate not supported on this device")
            false
        } else {
            val delegate = GpuDelegate(compatList.bestOptionsForThisDevice)
            gpuDelegate = delegate
            options.addDelegate(delegate)
            true
        }
    } catch (t: Throwable) {
        Timber.w(t, "[DocScanner][load] GPU delegate init failed, falling back to CPU")
        false
    }

    private fun loadModelFile(modelFile: File): MappedByteBuffer =
        FileInputStream(modelFile).use { fis ->
            fis.channel.map(FileChannel.MapMode.READ_ONLY, 0, modelFile.length())
        }

    override fun detect(
        grayBytes: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        timestamp: Long,
    ): DetectionResult? {
        val interp = obtainInterpreter() ?: return null

        val rotated = grayFrameRotator.rotate(grayBytes, width, height, rotationDegrees)

        fillInputTensor(rotated.bytes, rotated.width, rotated.height)

        try {
            interp.run(inputBuffer, outputTensor)
        } catch (e: Exception) {
            Timber.e(e, "[DocScanner][infer] TFLite inference failed")
            return null
        }

        flattenOutput(outputTensor, outputBuffer)

        val largest = largestComponentFinder.findLargest(
            mask = outputBuffer,
            width = INPUT_SIZE,
            height = INPUT_SIZE,
            threshold = MASK_THRESHOLD,
            minComponentPixels = MIN_COMPONENT_PIXELS,
        ) ?: return null

        if (!isAcceptedQuad(largest)) return null

        val boundary = toNormalisedBoundary(largest)
        logBoundaryIfVerbose(boundary)

        return DetectionResult(
            boundary = boundary,
            frameTimestamp = timestamp,
            frameWidth = rotated.width,
            frameHeight = rotated.height,
        )
    }

    private fun isAcceptedQuad(corners: LargestComponentFinder.ExtremeCorners): Boolean {
        val evaluated = boundaryGuard.evaluate(
            tl = corners.tl, tr = corners.tr, br = corners.br, bl = corners.bl,
            componentSize = corners.componentSize,
            maskWidth = INPUT_SIZE,
            maskHeight = INPUT_SIZE,
        )
        val verdictLabel = when (val v = evaluated.verdict) {
            BoundaryGuard.Verdict.Accepted -> "ACCEPT"
            is BoundaryGuard.Verdict.Rejected -> v.label(evaluated)
        }
        Timber.d(
            "[DocScanner][guard] mask=%d quad=%d fill=%.2f widthR=%.2f heightR=%.2f cover=%.2f → %s".format(
                corners.componentSize, evaluated.quadArea, evaluated.fillRatio,
                evaluated.widthRatio, evaluated.heightRatio, evaluated.maskCoverage,
                verdictLabel,
            ),
        )
        return evaluated.verdict is BoundaryGuard.Verdict.Accepted
    }

    private fun BoundaryGuard.Verdict.Rejected.label(metrics: BoundaryGuard.EvaluatedQuad): String =
        when (reason) {
            BoundaryGuard.RejectReason.ZERO_AREA -> "quadArea=0"
            BoundaryGuard.RejectReason.FILL_RATIO ->
                "fillRatio=%.2f<%.2f".format(metrics.fillRatio, BoundaryGuard.MIN_FILL_RATIO)
            BoundaryGuard.RejectReason.WIDTH_RATIO ->
                "widthRatio=%.2f<%.2f".format(metrics.widthRatio, BoundaryGuard.MIN_OPPOSITE_SIDE_RATIO)
            BoundaryGuard.RejectReason.HEIGHT_RATIO ->
                "heightRatio=%.2f<%.2f".format(metrics.heightRatio, BoundaryGuard.MIN_OPPOSITE_SIDE_RATIO)
        }

    /**
     * Maps pixel-space extreme corners to normalised [0, 1] frame coords so
     * the overlay and warp stages can stay resolution-agnostic.
     */
    private fun toNormalisedBoundary(
        corners: LargestComponentFinder.ExtremeCorners,
    ): DocumentBoundary = DocumentBoundary(
        topLeft = Point(corners.tl[0].toFloat() / INPUT_SIZE, corners.tl[1].toFloat() / INPUT_SIZE),
        topRight = Point(corners.tr[0].toFloat() / INPUT_SIZE, corners.tr[1].toFloat() / INPUT_SIZE),
        bottomRight = Point(corners.br[0].toFloat() / INPUT_SIZE, corners.br[1].toFloat() / INPUT_SIZE),
        bottomLeft = Point(corners.bl[0].toFloat() / INPUT_SIZE, corners.bl[1].toFloat() / INPUT_SIZE),
        // TFLite output is per-pixel; treat any successful + accepted
        // detection as confident.
        confidence = 1f,
    )

    private fun logBoundaryIfVerbose(boundary: DocumentBoundary) {
        if (!VERBOSE_DETECT) return
        Timber.d(
            "[DocScanner][detect] TL=(%.2f,%.2f) TR=(%.2f,%.2f) BR=(%.2f,%.2f) BL=(%.2f,%.2f)".format(
                boundary.topLeft.x, boundary.topLeft.y,
                boundary.topRight.x, boundary.topRight.y,
                boundary.bottomRight.x, boundary.bottomRight.y,
                boundary.bottomLeft.x, boundary.bottomLeft.y,
            ),
        )
    }

    /**
     * Resamples [bytes] (gray, [w]×[h]) into INPUT_SIZE×INPUT_SIZE, replicates
     * the Y value across RGB, and applies ImageNet normalisation as the model
     * was trained.
     */
    private fun fillInputTensor(bytes: ByteArray, w: Int, h: Int) {
        inputBuffer.rewind()
        val xScale = w.toFloat() / INPUT_SIZE
        val yScale = h.toFloat() / INPUT_SIZE
        for (ty in 0 until INPUT_SIZE) {
            val srcY = (ty * yScale).toInt().coerceAtMost(h - 1)
            for (tx in 0 until INPUT_SIZE) {
                val srcX = (tx * xScale).toInt().coerceAtMost(w - 1)
                val v = (bytes[srcY * w + srcX].toInt() and 0xFF) / 255f
                inputBuffer.putFloat((v - IMAGENET_MEAN[0]) / IMAGENET_STD[0])
                inputBuffer.putFloat((v - IMAGENET_MEAN[1]) / IMAGENET_STD[1])
                inputBuffer.putFloat((v - IMAGENET_MEAN[2]) / IMAGENET_STD[2])
            }
        }
        inputBuffer.rewind()
    }

    private fun flattenOutput(
        nested: Array<Array<Array<FloatArray>>>,
        flat: FloatArray,
    ) {
        for (y in 0 until INPUT_SIZE) {
            val row = nested[0][y]
            for (x in 0 until INPUT_SIZE) {
                flat[y * INPUT_SIZE + x] = row[x][0]
            }
        }
    }

    private companion object {
        // Flip to true for per-frame TL/TR/BR/BL coordinate dumps. Off by
        // default because the [guard] log already conveys what we need at
        // 5 Hz without flooding logcat.
        private const val VERBOSE_DETECT = false

        const val INPUT_SIZE = 512
        const val NUM_THREADS = 4
        const val MASK_THRESHOLD = 0.5f

        // 5% of the mask area — rejects speckle. Lives here because the
        // 512×512 frame size is also defined here; LargestComponentFinder
        // stays size-agnostic and takes this as a parameter.
        const val MIN_COMPONENT_PIXELS = (INPUT_SIZE * INPUT_SIZE) / 20

        val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
