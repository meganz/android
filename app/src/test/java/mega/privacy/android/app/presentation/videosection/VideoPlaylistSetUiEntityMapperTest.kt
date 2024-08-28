package mega.privacy.android.app.presentation.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.videosection.mapper.VideoPlaylistSetUiEntityMapper
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistSetUiEntity
import mega.privacy.android.domain.entity.set.UserSet
import nz.mega.sdk.MegaSet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlaylistSetUiEntityMapperTest {
    private lateinit var underTest: VideoPlaylistSetUiEntityMapper

    private val testId = 1L
    private val testTitle = "Video playlist title"

    @BeforeAll
    fun setUp() {
        underTest = VideoPlaylistSetUiEntityMapper()
    }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly`() =
        runTest {
            assertMappedVideoPlaylistSetUiEntity(
                videoPlaylistSetUiEntity = underTest(createUserSet())
            )
        }

    private fun assertMappedVideoPlaylistSetUiEntity(
        videoPlaylistSetUiEntity: VideoPlaylistSetUiEntity,
    ) {
        videoPlaylistSetUiEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${videoPlaylistSetUiEntity::class.simpleName}",
                { assertThat(it.id).isEqualTo(testId) },
                { assertThat(it.title).isEqualTo(testTitle) },
                { assertThat(it.isSelected).isEqualTo(false) }
            )
        }
    }

    private fun createUserSet(): UserSet = object : UserSet {
        override val id: Long = testId
        override val name: String = testTitle
        override val type: Int = MegaSet.SET_TYPE_PLAYLIST
        override val cover: Long? = null
        override val creationTime: Long = 0
        override val modificationTime: Long = 0
        override val isExported: Boolean = false
        override fun equals(other: Any?): Boolean {
            val otherSet = other as? UserSet ?: return false
            return id == otherSet.id
                    && name == otherSet.name
                    && cover == otherSet.cover
                    && modificationTime == otherSet.modificationTime
                    && isExported == otherSet.isExported
        }
    }
}