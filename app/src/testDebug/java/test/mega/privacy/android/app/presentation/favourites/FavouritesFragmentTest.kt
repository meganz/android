package test.mega.privacy.android.app.presentation.favourites

import android.view.View
import android.widget.ImageView
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.presentation.favourites.FavouritesFragment
import mega.privacy.android.app.presentation.favourites.adapter.FavouritesViewHolder
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedFolderNode
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.di.TestInitialiseUseCases
import test.mega.privacy.android.app.di.TestMapperModule
import test.mega.privacy.android.app.di.TestSortOrderUseCases
import test.mega.privacy.android.app.di.TestWrapperModule
import test.mega.privacy.android.app.launchFragmentInHiltContainer
import test.mega.privacy.android.app.testFragment

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FavouritesFragmentTest {
    @get: Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        initData()
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
        emptyView().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // Check the recycle view if is gone.
        recycleView().check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
        // Check the loading view if is gone.
        loadingView().check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
    }

    @Test
    fun test_that_the_recycler_view_is_displayed_when_favourites_is_not_empty() {
        launchFragmentInHiltContainer<FavouritesFragment>()
        // Check the recycle view if is visible.
        recycleView().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // Check the empty layout if is gone
        emptyView().check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
        // Check the loading view if is gone.
        loadingView().check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
    }

    @Test
    fun test_that_clicked_folder_item_and_navigate_to_favourite_folder_fragment() {
        val mockNavController = mock<NavController>()

        val scenario = launchFragmentInHiltContainer<FavouritesFragment>()
        scenario?.onActivity { activity ->
            Navigation.setViewNavController(
                activity.testFragment<FavouritesFragment>().requireView(), mockNavController
            )
        }

        recycleView().perform(RecyclerViewActions.actionOnItemAtPosition<FavouritesViewHolder>(1,
            ViewActions.click()))
        verify(mockNavController).navigate(
            HomepageFragmentDirections.actionHomepageFragmentToFavouritesFolderFragment(
                0
            )
        )
    }

    @Test
    fun test_that_clicked_three_dot_and_the_snack_bar_shows_error_message_when_offline() {
        val connected = MutableStateFlow(true)
        whenever(TestInitialiseUseCases.monitorConnectivity()).thenReturn(connected)
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

        connected.tryEmit(false)
        recycleView().perform(RecyclerViewActions.actionOnItemAtPosition<FavouritesViewHolder>(1,
            threeDotClicked))

        snackBarView().check(ViewAssertions.matches(ViewMatchers.withText(R.string.error_server_connection_problem)))
    }

    private fun initData() {
        val node = mock<MegaNode>()
        val typedNode = mock<TypedFolderNode>()
        whenever(node.isFolder).thenReturn(false)
        whenever(node.isInShare).thenReturn(true)
        whenever(typedNode.name).thenReturn("testName")
        whenever(node.name).thenReturn("testName")
        whenever(node.label).thenReturn(MegaNode.NODE_LBL_RED)
        whenever(node.size).thenReturn(1000L)
        val favourite = mock<FavouriteFolder>()
        whenever(favourite.labelColour).thenReturn(R.color.salmon_400_salmon_300)
        whenever(favourite.typedNode).thenReturn(typedNode)
        val list = listOf(typedNode)
        runBlocking {
            whenever(TestSortOrderUseCases.getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(TestMapperModule.sortOrderIntMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(
                MegaApiJava.ORDER_DEFAULT_ASC)
            whenever(FavouritesTestModule.getAllFavourites()).thenReturn(
                flowOf(list)
            )
            whenever(FavouritesTestModule.stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
            whenever(FavouritesTestModule.favouriteMapper(
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )).thenReturn(
                favourite)
            whenever(FavouritesTestModule.getThumbnail(1)).thenReturn(null)
            whenever(TestWrapperModule.fetchNodeWrapper(any())).thenReturn(node)
        }
    }

    private fun emptyView() = Espresso.onView(ViewMatchers.withId(R.id.empty_hint))
    private fun recycleView() = Espresso.onView(ViewMatchers.withId(R.id.file_list_view_browser))
    private fun loadingView() = Espresso.onView(ViewMatchers.withId(R.id.favourite_progressbar))
    private fun snackBarView() =
        Espresso.onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
}