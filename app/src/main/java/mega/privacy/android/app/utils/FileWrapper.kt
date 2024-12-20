package mega.privacy.android.app.utils

import androidx.annotation.Keep
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.transfers.GetFileDescriptorWrapperFromUriPathUseCase

/**
 * Class used by SDK via JNI to get access to the native file descriptor (fd) and metadata of a file or folder. It's used to access files and folders from a content uri or path in a platform agnostic way.
 * This is a sync version of [mega.privacy.android.domain.entity.file.FileDescriptorWrapper] to avoid suspended functions. It should be used by native code only.
 */
@Keep
class FileWrapper(
    val uri: String,
    val name: String,
    val isFolder: Boolean,
    private val getChildrenUrisFunction: () -> List<UriPath>,
    private val getDetachedFileDescriptorFunction: (write: Boolean) -> Int?,
) {
    @Keep
    fun getFileDescriptor(write: Boolean) =
        getDetachedFileDescriptorFunction(write)

    @Keep
    fun getChildrenUris(): List<String> = getChildrenUrisFunction().map { it.value }

    companion object {

        /**
         * As this is used by native code, we can't use dependency injection directly, we need static methods
         */
        fun initializeFactory(
            getFileWrapperFromUriPathUseCase: Lazy<GetFileDescriptorWrapperFromUriPathUseCase>,
            ioDispatcher: CoroutineDispatcher,
        ) {
            FileWrapper.getFileWrapperFromUriPathUseCase = getFileWrapperFromUriPathUseCase
            FileWrapper.ioDispatcher = ioDispatcher
        }

        private lateinit var getFileWrapperFromUriPathUseCase: Lazy<GetFileDescriptorWrapperFromUriPathUseCase>

        private lateinit var ioDispatcher: CoroutineDispatcher

        /**
         * Returns [FileWrapper] from [uriPath] string.
         * @param uriPath Usually the content uri of the file, but it can be a path also
         */
        @JvmStatic
        @Keep
        fun getFromUri(uriPath: String) =
            runBlocking(ioDispatcher) {
                getFileWrapperFromUriPathUseCase.get().invoke(UriPath(uriPath))
                    ?.let { asyncWrapper ->
                        FileWrapper(
                            uri = asyncWrapper.uriPath.value,
                            name = asyncWrapper.name,
                            isFolder = asyncWrapper.isFolder,
                            getDetachedFileDescriptorFunction = {
                                runBlocking(ioDispatcher) {
                                    asyncWrapper.getDetachedFileDescriptor(it)
                                }
                            },
                            getChildrenUrisFunction = {
                                runBlocking(ioDispatcher) {
                                    asyncWrapper.getChildrenUris()
                                }
                            }
                        )
                    }
            }
    }
}