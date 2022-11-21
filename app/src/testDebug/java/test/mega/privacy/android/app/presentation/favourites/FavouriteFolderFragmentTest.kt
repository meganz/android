package test.mega.privacy.android.app.presentation.favourites

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.favourites.FavouriteFolderFragment
import mega.privacy.android.app.presentation.favourites.FavouritesViewHolder
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.node.TypedFolderNode
import nz.mega.sdk.MegaNode
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.di.TestWrapperModule
import test.mega.privacy.android.app.launchFragmentInHiltContainer

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FavouriteFolderFragmentTest {
    companion object {
        const val KEY_PARENT_HANDLE_ARGUMENT = "parentHandle"
    }

    @get: Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        initData()
        hiltRule.inject()
    }

    @Test
    fun test_that_the_recycler_view_is_displayed_when_children_of_current_folder_is_not_empty() {
        launchFragmentInHiltContainer<FavouriteFolderFragment>(
            fragmentArgs = Bundle().apply {
                putLong(KEY_PARENT_HANDLE_ARGUMENT, 1)
            }
        )

        // Check the recycle view if is visible.
        recycleView().check(matches(isDisplayed()))
        // Check the empty layout if is gone
        emptyView().check(matches(not(isDisplayed())))
        // Check the loading view if is gone.
        loadingView().check(matches(not(isDisplayed())))
    }

    @Test
    fun test_that_the_empty_view_is_displayed_when_children_of_current_folder_is_empty() {
        runBlocking {
            whenever(FavouritesTestModule.getFavouriteFolderInfo(1)).thenReturn(
                flowOf(FavouriteFolderInfo(emptyList(), "test name", 1, 1))
            )
        }
        launchFragmentInHiltContainer<FavouriteFolderFragment>(
            fragmentArgs = Bundle().apply {
                putLong(KEY_PARENT_HANDLE_ARGUMENT, 1)
            }
        )

        // Check the empty view if is visible.
        emptyView().check(matches(isDisplayed()))
        // Check the recycle view if is gone.
        recycleView().check(matches(not(isDisplayed())))
        // Check the loading view if is gone.
        loadingView().check(matches(not(isDisplayed())))
    }

    @Test
    fun test_that_clicked_three_dot_and_the_snack_bar_shows_error_message_when_offline() {
        launchFragmentInHiltContainer<FavouriteFolderFragment>(
            fragmentArgs = Bundle().apply {
                putLong(KEY_PARENT_HANDLE_ARGUMENT, 1)
            }
        )

        val threeDotClicked = object : ViewAction {
            override fun getConstraints(): Matcher<View>? = null

            override fun getDescription(): String = "three dot is clicked"

            override fun perform(uiController: UiController?, view: View?) {
                view?.run {
                    findViewById<ImageView>(R.id.item_three_dots).performClick()
                }
            }

        }
        recycleView().perform(
            RecyclerViewActions.actionOnItemAtPosition<FavouritesViewHolder>(
                0,
                threeDotClicked
            )
        )

        snackBarView().check(matches(withText(R.string.error_server_connection_problem)))
    }

    private fun initData() {
        val node = mock<MegaNode>()
        whenever(node.isFolder).thenReturn(true)
        whenever(node.isInShare).thenReturn(true)
        whenever(node.name).thenReturn("testName.txt")
        whenever(node.label).thenReturn(MegaNode.NODE_LBL_RED)
        whenever(node.size).thenReturn(1000L)
        val typedFolderNode = mock<TypedFolderNode>()
        val favourites = listOf(
            typedFolderNode
        )
        val favouriteFolderInfo = FavouriteFolderInfo(
            favourites,
            "test folder",
            1,
            1
        )
        val favourite = mock<FavouriteFolder> {
            on { typedNode }.thenReturn(typedFolderNode)
        }
        whenever(favourite.labelColour).thenReturn(R.color.salmon_400_salmon_300)

        runBlocking {
            whenever(TestWrapperModule.fetchNodeWrapper(anyOrNull())).thenReturn(node)

            whenever(FavouritesTestModule.getFavouriteFolderInfo(anyOrNull())).thenReturn(
                flowOf(favouriteFolderInfo)
            )
            whenever(FavouritesTestModule.stringUtilWrapper.getFolderInfo(any(), any())).thenReturn(
                "")
            whenever(FavouritesTestModule.favouriteMapper(
                any(),
                any(),
                any(),
                any(),
                any())).thenReturn(
                favourite
            )
            whenever(FavouritesTestModule.getThumbnail(1)).thenReturn(null)

            whenever(FavouritesTestModule.megaUtilWrapper.availableOffline(anyOrNull(),
                anyOrNull())).thenReturn(true)
        }
    }

    private fun emptyView() = onView(withId(R.id.empty_hint))
    private fun recycleView() = onView(withId(R.id.file_list_view_browser))
    private fun loadingView() = onView(withId(R.id.favourite_progressbar))
    private fun snackBarView() = onView(withId(com.google.android.material.R.id.snackbar_text))
}