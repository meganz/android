package mega.privacy.android.feature.clouddrive.presentation.offline

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineNodeUtilTest {

    @Test
    fun `test that thumbnailData is null for a folder`() {
        val result = offlineFileInformation(isFolder = true).thumbnailData
        assertThat(result).isNull()
    }

    @Test
    fun `test that thumbnailData uses the cached thumbnail uri when available`() {
        val result = offlineFileInformation(
            thumbnail = "file:///cache/thumb.jpg",
            absolutePath = "/offline/image.jpg",
            fileTypeInfo = imageType,
        ).thumbnailData

        assertThat(result).isEqualTo(ThumbnailUriRequest(UriPath("file:///cache/thumb.jpg")))
    }

    @Test
    fun `test that thumbnailData falls back to the local original file for an image node`() {
        val result = offlineFileInformation(
            thumbnail = null,
            absolutePath = "/offline/image.jpg",
            fileTypeInfo = imageType,
        ).thumbnailData

        assertThat(result).isEqualTo(ThumbnailUriRequest(UriPath("/offline/image.jpg")))
    }

    @Test
    fun `test that thumbnailData falls back to the local original file for a video node`() {
        val result = offlineFileInformation(
            thumbnail = null,
            absolutePath = "/offline/video.mp4",
            fileTypeInfo = videoType,
        ).thumbnailData

        assertThat(result).isEqualTo(ThumbnailUriRequest(UriPath("/offline/video.mp4")))
    }

    @Test
    fun `test that thumbnailData falls back to an SDK request for a non image or video node`() {
        val result = offlineFileInformation(
            handle = "123",
            thumbnail = null,
            absolutePath = "/offline/document.pdf",
            fileTypeInfo = PdfFileTypeInfo,
        ).thumbnailData

        assertThat(result).isEqualTo(ThumbnailRequest(NodeId(123L)))
    }

    @Test
    fun `test that thumbnailData falls back to an SDK request when the local path is empty`() {
        val result = offlineFileInformation(
            handle = "123",
            thumbnail = null,
            absolutePath = "",
            fileTypeInfo = imageType,
        ).thumbnailData

        assertThat(result).isEqualTo(ThumbnailRequest(NodeId(123L)))
    }

    @Test
    fun `test that thumbnailData is null when there is no local source and the handle is invalid`() {
        val result = offlineFileInformation(
            handle = "-1",
            thumbnail = null,
            absolutePath = "",
            fileTypeInfo = imageType,
        ).thumbnailData

        assertThat(result).isNull()
    }

    private val imageType = StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
    private val videoType =
        VideoFileTypeInfo(mimeType = "video/mp4", extension = "mp4", duration = 10.seconds)

    private fun offlineFileInformation(
        handle: String = "123",
        isFolder: Boolean = false,
        thumbnail: String? = null,
        absolutePath: String = "",
        fileTypeInfo: FileTypeInfo? = null,
    ) = OfflineFileInformation(
        nodeInfo = OtherOfflineNodeInformation(
            id = 1,
            path = "/offline",
            name = "name",
            handle = handle,
            isFolder = isFolder,
            lastModifiedTime = null,
            parentId = 0,
        ),
        fileTypeInfo = fileTypeInfo,
        thumbnail = thumbnail,
        absolutePath = absolutePath,
    )
}
