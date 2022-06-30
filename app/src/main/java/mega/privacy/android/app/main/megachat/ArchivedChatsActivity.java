package mega.privacy.android.app.main.megachat;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.Util.hideKeyboard;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ChatBottomSheetDialogFragment;
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

@AndroidEntryPoint
public class ArchivedChatsActivity extends PasscodeActivity implements MegaChatRequestListenerInterface, MegaRequestListenerInterface {

    @Inject
    GetChatChangesUseCase getChatChangesUseCase;

    Toolbar tB;
    ActionBar aB;
    FrameLayout fragmentContainer;
    RecentChatsFragment archivedChatsFragment;
    FloatingActionButton fab;

    private BadgeDrawerArrowDrawable badgeDrawable;

    public long selectedChatItemId;

    DisplayMetrics outMetrics;

    MenuItem searchMenuItem;
    SearchView searchView;

    ArchivedChatsActivity archivedChatsActivity;

    String querySearch = "";
    boolean isSearchExpanded = false;
    boolean pendingToOpenSearchView = false;

    private ChatBottomSheetDialogFragment bottomSheetDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }

        archivedChatsActivity = this;
        checkChatChanges();

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();

        display.getMetrics(outMetrics);

        setContentView(R.layout.activity_chat_explorer);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_chat_explorer);
        fab = (FloatingActionButton) findViewById(R.id.fab_chat_explorer);
        fab.setVisibility(View.GONE);

        //Set toolbar
        tB = (Toolbar) findViewById(R.id.toolbar_chat_explorer);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        if (aB != null) {
            aB.setTitle(getString(R.string.archived_chats_title_section).toUpperCase());
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        } else {
            Timber.w("aB is null");
        }

        badgeDrawable = new BadgeDrawerArrowDrawable(this, R.color.red_600_red_300,
                R.color.white_dark_grey, R.color.white_dark_grey);

        updateNavigationToolbarIcon();

        if (archivedChatsFragment == null) {
            archivedChatsFragment = RecentChatsFragment.newInstance();
        }

        if (savedInstanceState != null) {
            querySearch = savedInstanceState.getString("querySearch", "");
            isSearchExpanded = savedInstanceState.getBoolean("isSearchExpanded", isSearchExpanded);

            if (isSearchExpanded) {
                pendingToOpenSearchView = true;
            }
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container_chat_explorer, archivedChatsFragment, "archivedChatsFragment");
        ft.commitNow();
    }


    public void showChatPanel(MegaChatListItem chat) {
        Timber.d("showChatPanel");

        if (chat == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedChatItemId = chat.getChatId();
        bottomSheetDialogFragment = new ChatBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("querySearch", querySearch);
        outState.putBoolean("isSearchExpanded", isSearchExpanded);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected");

        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_archived_chats, menu);

        searchMenuItem = menu.findItem(R.id.action_search);

        searchView = (SearchView) searchMenuItem.getActionView();

        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setHint(getString(R.string.hint_action_search));
        View v = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        v.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

        if (searchView != null) {
            searchView.setIconifiedByDefault(true);
        }

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                isSearchExpanded = true;
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                isSearchExpanded = false;
                archivedChatsFragment.closeSearch();
                supportInvalidateOptionsMenu();
                return true;
            }
        });

        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Timber.d("Query: %s", query);
                hideKeyboard(archivedChatsActivity, 0);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                querySearch = newText;
                archivedChatsFragment.filterChats(newText, true);
                return true;
            }
        });

        if (pendingToOpenSearchView) {
            String query = querySearch;
            searchMenuItem.expandActionView();
            searchView.setQuery(query, false);
            pendingToOpenSearchView = false;
        }

        ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();
        if (archivedChats != null && !archivedChats.isEmpty()) {
            searchMenuItem.setVisible(true);
        } else {
            searchMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public void closeSearchView() {
        if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
            searchMenuItem.collapseActionView();
        }
    }

    public void showSnackbar(String s) {
        Timber.d("showSnackbar: %s", s);
        showSnackbar(fragmentContainer, s);
    }

    public void updateNavigationToolbarIcon() {
        int numberUnread = megaChatApi.getUnreadChats();

        if (numberUnread == 0) {
            aB.setHomeAsUpIndicator(Util.isDarkMode(this) ? R.drawable.ic_arrow_back_white : R.drawable.ic_arrow_back_black);
        } else {
            badgeDrawable.setProgress(1.0f);

            if (numberUnread > Constants.MAX_BADGE_NUM) {
                badgeDrawable.setText(Constants.MAX_BADGE_NUM + "+");
            } else {
                badgeDrawable.setText(numberUnread + "");
            }

            aB.setHomeAsUpIndicator(badgeDrawable);
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.d("onRequestFinish(CHAT)");

        if (request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM) {
            long chatHandle = request.getChatHandle();
            MegaChatRoom chat = megaChatApi.getChatRoom(chatHandle);

            String chatTitle = getTitleChat(chat);

            if (chatTitle == null) {
                chatTitle = "";
            } else if (!chatTitle.isEmpty() && chatTitle.length() > 60) {
                chatTitle = chatTitle.substring(0, 59) + "...";
            }

            if (!chatTitle.isEmpty() && chat.isGroup() && !chat.hasCustomTitle()) {
                chatTitle = "\"" + chatTitle + "\"";
            }

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getFlag()) {
                    Timber.d("Chat archived");
                    showSnackbar(getString(R.string.success_archive_chat, chatTitle));
                } else {
                    Timber.d("Chat unarchived");
                    showSnackbar(getString(R.string.success_unarchive_chat, chatTitle));
                }
            } else {
                if (request.getFlag()) {
                    Timber.e("ERROR WHEN ARCHIVING CHAT %s", e.getErrorString());
                    showSnackbar(getString(R.string.error_archive_chat, chatTitle));
                } else {
                    Timber.e("ERROR WHEN UNARCHIVING CHAT %s", e.getErrorString());
                    showSnackbar(getString(R.string.error_unarchive_chat, chatTitle));
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            long requestNumber = request.getNumber();
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", requestNumber);
            int errorCode = e.getErrorCode();
            if (errorCode == MegaError.API_OK && requestNumber == MegaContactRequest.INVITE_ACTION_ADD) {
                showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
            } else if (errorCode == MegaError.API_EEXIST) {
                Timber.w("%s is already a contact", request.getEmail());
                showSnackbar(getString(R.string.context_contact_already_exists, request.getEmail()));
            } else if (errorCode == MegaError.API_EARGS && requestNumber == MegaContactRequest.INVITE_ACTION_ADD) {
                Timber.w("No need to add yourself.");
                showSnackbar(getString(R.string.error_own_email_as_contact));
            } else {
                Timber.w("Invite error.");
                showSnackbar(getString(R.string.general_error));
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    /**
     * Receive changes to OnChatListItemUpdate and make the necessary changes
     */
    private void checkChatChanges() {
        Disposable chatSubscription = getChatChangesUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(result -> result instanceof GetChatChangesUseCase.Result.OnChatListItemUpdate)
                .map(result -> (GetChatChangesUseCase.Result.OnChatListItemUpdate) result)
                .subscribe((next) -> {
                    MegaChatListItem item = next.component1();
                    if (item != null) {
                        Timber.d("Chat ID: %s", item.getChatId());
                        if (archivedChatsFragment != null && archivedChatsFragment.isAdded()) {
                            archivedChatsFragment.listItemUpdate(item);
                        }

                        if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
                            updateNavigationToolbarIcon();
                        }
                    }
                }, Timber::e);

        composite.add(chatSubscription);
    }
}
