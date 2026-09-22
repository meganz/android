package mega.privacy.android.domain.usecase

import javax.inject.Inject

/**
 * Default create trace string implementation of [CreateTraceString]
 */
internal class DefaultCreateTraceString @Inject constructor() : CreateTraceString {
    override suspend fun invoke(
        trace: List<StackTraceElement>,
        loggingClasses: List<String>,
    ) = trace
        .firstOrNull { it.className !in loggingClasses }
        // JVM stack frame layout: in release builds the file name collapses to "SourceFile", so the
        // class name is what lets retrace map the frame back through mapping.txt.
        ?.let { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
}