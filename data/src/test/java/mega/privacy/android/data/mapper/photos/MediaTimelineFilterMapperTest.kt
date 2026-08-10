package mega.privacy.android.data.mapper.photos

import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Category
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Granularity
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Location
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaGroupNodesByDateFilter
import nz.mega.sdk.MegaHandleList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MediaTimelineFilterMapperTest {

    private val mediaTimelineGranularityIntMapper = mock<MediaTimelineGranularityIntMapper>()
    private val mediaTimelineCategoryIntMapper = mock<MediaTimelineCategoryIntMapper>()
    private val mediaTimelineSensitivityIntMapper = mock<MediaTimelineSensitivityIntMapper>()
    private val mediaTimelineLocationIntMapper = mock<MediaTimelineLocationIntMapper>()
    private val megaHandleListMapper = mock<MegaHandleListMapper>()

    private val underTest = MediaTimelineFilterMapper(
        mediaTimelineGranularityIntMapper = mediaTimelineGranularityIntMapper,
        mediaTimelineCategoryIntMapper = mediaTimelineCategoryIntMapper,
        mediaTimelineSensitivityIntMapper = mediaTimelineSensitivityIntMapper,
        mediaTimelineLocationIntMapper = mediaTimelineLocationIntMapper,
        megaHandleListMapper = megaHandleListMapper,
    )

    @Test
    fun `test that granularity category sensitivity and location are applied to the sdk filter`() {
        val filter = MediaTimelineFilter(
            granularity = Granularity.Month,
            category = Category.All,
            location = Location.CloudDriveAndVault,
            sensitivity = Sensitivity.ShowAll,
        )
        whenever(mediaTimelineGranularityIntMapper(Granularity.Month)).thenReturn(GRANULARITY)
        whenever(mediaTimelineCategoryIntMapper(Category.All)).thenReturn(CATEGORY)
        whenever(mediaTimelineSensitivityIntMapper(Sensitivity.ShowAll)).thenReturn(SENSITIVITY)
        whenever(mediaTimelineLocationIntMapper(Location.CloudDriveAndVault)).thenReturn(LOCATION)

        val sdkFilter = mock<MegaGroupNodesByDateFilter>()
        mockStatic(MegaGroupNodesByDateFilter::class.java).use { mockedStatic ->
            mockedStatic.`when`<MegaGroupNodesByDateFilter> {
                MegaGroupNodesByDateFilter.createInstance()
            }.thenReturn(sdkFilter)

            underTest(filter)

            verify(sdkFilter).byGranularity(GRANULARITY)
            verify(sdkFilter).byCategory(CATEGORY)
            verify(sdkFilter).bySensitivity(SENSITIVITY)
            verify(sdkFilter).byLocation(LOCATION)
        }
    }

    @Test
    fun `test that include location handles are applied and location scope is not when includeLocationHandles is set`() {
        val filter = MediaTimelineFilter(
            granularity = Granularity.Day,
            category = Category.All,
            location = Location.CloudDriveAndVault,
            sensitivity = Sensitivity.ShowAll,
            includeLocationHandles = listOf(NodeId(PRIMARY_HANDLE), NodeId(SECONDARY_HANDLE)),
        )
        val handleList = mock<MegaHandleList>()
        whenever(megaHandleListMapper(listOf(PRIMARY_HANDLE, SECONDARY_HANDLE)))
            .thenReturn(handleList)

        val sdkFilter = mock<MegaGroupNodesByDateFilter>()
        mockStatic(MegaGroupNodesByDateFilter::class.java).use { mockedStatic ->
            mockedStatic.`when`<MegaGroupNodesByDateFilter> {
                MegaGroupNodesByDateFilter.createInstance()
            }.thenReturn(sdkFilter)

            underTest(filter)

            verify(sdkFilter).byLocationHandles(handleList)
            verify(sdkFilter, never()).byLocation(any())
            verify(sdkFilter, never()).byExcludeLocationHandles(any())
        }
    }

    @Test
    fun `test that location scope and exclude location handles are applied when excludeLocationHandles is set`() {
        val filter = MediaTimelineFilter(
            granularity = Granularity.Day,
            category = Category.All,
            location = Location.CloudDriveAndVault,
            sensitivity = Sensitivity.ShowAll,
            excludeLocationHandles = listOf(NodeId(PRIMARY_HANDLE), NodeId(SECONDARY_HANDLE)),
        )
        whenever(mediaTimelineLocationIntMapper(Location.CloudDriveAndVault)).thenReturn(LOCATION)
        val handleList = mock<MegaHandleList>()
        whenever(megaHandleListMapper(listOf(PRIMARY_HANDLE, SECONDARY_HANDLE)))
            .thenReturn(handleList)

        val sdkFilter = mock<MegaGroupNodesByDateFilter>()
        mockStatic(MegaGroupNodesByDateFilter::class.java).use { mockedStatic ->
            mockedStatic.`when`<MegaGroupNodesByDateFilter> {
                MegaGroupNodesByDateFilter.createInstance()
            }.thenReturn(sdkFilter)

            underTest(filter)

            verify(sdkFilter).byLocation(LOCATION)
            verify(sdkFilter).byExcludeLocationHandles(handleList)
            verify(sdkFilter, never()).byLocationHandles(any())
        }
    }

    private companion object {
        private const val GRANULARITY = 2
        private const val CATEGORY = 13
        private const val SENSITIVITY = 0
        private const val LOCATION = 1
        private const val PRIMARY_HANDLE = 100L
        private const val SECONDARY_HANDLE = 200L
    }
}
