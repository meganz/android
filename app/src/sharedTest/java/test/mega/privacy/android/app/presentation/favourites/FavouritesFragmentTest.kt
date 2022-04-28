package test.mega.privacy.android.app.presentation.favourites

import android.view.View
import android.widget.ImageView
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.presentation.favourites.FavouritesFragment
import mega.privacy.android.app.presentation.favourites.FavouritesViewHolder
import nz.mega.sdk.MegaNode
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.launchFragmentInHiltContainer
import test.mega.privacy.android.app.testFragment

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FavouritesFragmentTest {
    @get: Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun test_that_the_empty_view_is_displayed_when_favourites_is_empty() {
        runBlocking {
            whenever(FavouritesTestModule.getAllFavourites()).thenReturn(
                flowOf(emptyList()))
        }
        launchFragmentInHiltContainer<FavouritesFragment>()
        // Check the empty view if is visible.
        emptyView().check(matches(isDisplayed()))
        // Check the recycle view if is gone.
        recycleView().check(matches(not(isDisplayed())))
        // Check the loading view if is gone.
        loadingView().check(matches(not(isDisplayed())))
    }

    @Test
    fun test_that_the_recycler_view_is_displayed_when_favourites_is_not_empty() {
        val node = mock<MegaNode>()
        val favouriteInfo = FavouriteInfo(node, false, 0, 0)
        whenever(node.name).thenReturn("test name")
        val favourites = listOf(
            favouriteInfo
        )
        runBlocking {
            whenever(FavouritesTestModule.getAllFavourites()).thenReturn(
                flowOf(favourites))
        }
        launchFragmentInHiltContainer<FavouritesFragment>()
        // Check the recycle view if is visible.
        recycleView().check(matches(isDisplayed()))
        // Check the empty layout if is gone
        emptyView().check(matches(not(isDisplayed())))
        // Check the loading view if is gone.
        loadingView().check(matches(not(isDisplayed())))
    }

    @Test
    fun test_that_clicked_folder_item_and_navigate_to_favourite_folder_fragment() {
        val mockNavController = mock<NavController>()
        val node = mock<MegaNode>()
        val favouriteInfo = FavouriteInfo(node, false, 0, 0)
        whenever(node.name).thenReturn("test folder")
        whenever(node.isFolder).thenReturn(true)
        whenever(node.isInShare).thenReturn(true)
        whenever(FavouritesTestModule.stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
        val favourites = listOf(
            favouriteInfo
        )
        runBlocking {
            whenever(FavouritesTestModule.getAllFavourites()).thenReturn(
                flowOf(favourites)
            )
        }

        val scenario = launchFragmentInHiltContainer<FavouritesFragment>()
        scenario?.onActivity { activity ->
            Navigation.setViewNavController(
                activity.testFragment<FavouritesFragment>().requireView(), mockNavController
            )
        }

        recycleView().perform(actionOnItemAtPosition<FavouritesViewHolder>(0, click()))
        verify(mockNavController).navigate(
            HomepageFragmentDirections.actionHomepageFragmentToFavouritesFolderFragment(
                0
            )
        )
    }

    @Test
    fun test_that_clicked_three_dot_and_the_snack_bar_shows_error_message_when_offline() {
        val node = mock<MegaNode>()
        val favouriteInfo = FavouriteInfo(node, false, 0, 0)
        whenever(node.name).thenReturn("test folder")
        whenever(FavouritesTestModule.megaUtilWrapper.isOnline(any())).thenReturn(false)
        val favourites = listOf(
            favouriteInfo
        )
        runBlocking {
            whenever(FavouritesTestModule.getAllFavourites()).thenReturn(
                flowOf(favourites)
            )
        }

        launchFragmentInHiltContainer<FavouritesFragment>()

        val threeDotClicked = object : ViewAction {
            override fun getConstraints(): Matcher<View>? = null

            override fun getDescription(): String = "three dot is clicked"

            override fun perform(uiController: UiController?, view: View?) {
                view?.run {
                    findViewById<ImageView>(R.id.item_three_dots).performClick()
                }
            }

        }
        recycleView().perform(actionOnItemAtPosition<FavouritesViewHolder>(0, threeDotClicked))

        snackBarView().check(matches(withText(R.string.error_server_connection_problem)))
    }

    private fun emptyView() = onView(withId(R.id.empty_hint))
    private fun recycleView() = onView(withId(R.id.file_list_view_browser))
    private fun loadingView() = onView(withId(R.id.favourite_progressbar))
    private fun snackBarView() = onView(withId(com.google.android.material.R.id.snackbar_text))
}