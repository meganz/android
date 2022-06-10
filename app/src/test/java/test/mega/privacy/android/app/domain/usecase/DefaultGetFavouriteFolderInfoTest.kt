package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.repository.FavouritesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetFavouriteFolderInfo
import mega.privacy.android.app.domain.usecase.GetFavouriteFolderInfo
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DefaultGetFavouriteFolderInfoTest {
    lateinit var underTest: GetFavouriteFolderInfo
    private val favouritesRepository = mock<FavouritesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetFavouriteFolderInfo(favouritesRepository)
        whenever(favouritesRepository.monitorNodeChange()).thenReturn(flowOf(false))
    }

    @Test
    fun `test that children of current folder is not empty`() {
        runTest {
            val list = mock<List<FavouriteInfo>>()
            val favouriteFolderInfo = FavouriteFolderInfo(
                list,
                "",
                1,
                1
            )
            whenever(favouritesRepository.getChildren(1)).thenReturn(
                favouriteFolderInfo
            )
            underTest(1).collect {
                assertTrue((it ?: return@collect).children.isNotEmpty())
            }

        }
    }

    @Test
    fun `test that children of current folder is empty`() {
        runTest {
            val favouriteFolderInfo = FavouriteFolderInfo(
                emptyList(),
                "",
                1,
                1
            )
            whenever(favouritesRepository.getChildren(1)).thenReturn(
                favouriteFolderInfo
            )
            underTest(1).collect {
                assertTrue((it ?: return@collect).children.isEmpty())
            }
        }
    }
}