package mega.privacy.android.data.filewrapper

import androidx.annotation.Keep

/**
 * Metadata for a single child entry returned by a batch directory scan.
 *
 * Fields are annotated with [@JvmField] so that C++ JNI code can access them via
 * [GetFieldID] directly (Kotlin generates private backing fields + getters by default,
 * which causes [GetFieldID] to return `nullptr` for a non-void-method signature).
 *
 * The class is annotated with [@Keep] to prevent R8 from removing or renaming it or
 * any of its fields during minification — the C++ side references the class and fields
 * by literal JNI strings at runtime.
 *
 * JNI class path: `"mega/privacy/android/data/filewrapper/ChildMetadata"`
 * (top-level class — no `$` separator).
 */
@Keep
data class ChildMetadata(
    @JvmField val uri: String,
    @JvmField val name: String,
    @JvmField val isFolder: Boolean,
    @JvmField val size: Long,
    @JvmField val lastModified: Long,
    /** Resolved filesystem path, or null if the URI authority is not ExternalStorageProvider. */
    @JvmField val path: String?,
)
