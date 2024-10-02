package mega.privacy.android.app.main.megachat.chat.explorer;

import static mega.privacy.android.app.main.FileExplorerActivity.CHAT_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.arch.extensions.ViewExtensionsKt;
import mega.privacy.android.app.components.PositionDividerItemDecoration;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.main.CheckScrollInterface;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.main.megachat.chatAdapters.MegaChipChatExplorerAdapter;
import mega.privacy.android.app.main.megachat.chatAdapters.MegaListChatExplorerAdapter;
import mega.privacy.android.app.main.model.chat.explorer.ChatExplorerUiState;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import timber.log.Timber;

@AndroidEntryPoint
public class ChatExplorerFragment extends Fragment implements CheckScrollInterface {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    private ChatExplorerFragment chatExplorerFragment;

    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private Context context;
    private ActionBar aB;
    private RecyclerView listView;
    private MegaListChatExplorerAdapter adapterList;

    private LinearLayoutManager mLayoutManager;

    private int lastFirstVisiblePosition;

    //Empty screen
    private TextView emptyTextView;
    private LinearLayout emptyLayout;
    private ImageView emptyImageView;
    private RelativeLayout contentLayout;
    private ProgressBar progressBar;

    private float density;
    private DisplayMetrics outMetrics;
    private Display display;

    private AppBarLayout addLayout;
    private Button newGroupButton;
    private RecyclerView addedList;
    private MegaChipChatExplorerAdapter adapterAdded;
    private LinearLayoutManager addedLayoutManager;

    private PositionDividerItemDecoration positionDividerItemDecoration;
    private SimpleDividerItemDecoration simpleDividerItemDecoration;

    private ChatExplorerViewModel viewModel;

    private ChatExplorerUiState uiState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
            if (context instanceof ChatExplorerActivity) {
                ((ChatExplorerActivity) context).checkChatChanges();
            } else if (context instanceof FileExplorerActivity) {
                ((FileExplorerActivity) context).checkChatChanges();
            }
        }

        chatExplorerFragment = this;
    }

    public static ChatExplorerFragment newInstance() {
        Timber.d("newInstance");
        ChatExplorerFragment fragment = new ChatExplorerFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.chat_recent_explore, container, false);

        contentLayout = v.findViewById(R.id.content_layout_chat_explorer);
        progressBar = v.findViewById(R.id.progressbar_chat_explorer);

        addLayout = v.findViewById(R.id.linear_layout_add);
        addedList = v.findViewById(R.id.contact_adds_recycler_view);
        newGroupButton = v.findViewById(R.id.new_group_button);
        if (context instanceof ChatExplorerActivity || context instanceof FileExplorerActivity) {
            addLayout.setVisibility(View.VISIBLE);
            addedLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            addedList.setLayoutManager(addedLayoutManager);
            addedList.setItemAnimator(new DefaultItemAnimator());
            addedList.setClipToPadding(false);

            if (context instanceof ChatExplorerActivity) {
                newGroupButton.setOnClickListener((ChatExplorerActivity) context);
            } else if (context instanceof FileExplorerActivity) {
                newGroupButton.setOnClickListener((FileExplorerActivity) context);
            }
            setFirstLayoutVisibility(View.VISIBLE);
        } else {
            addLayout.setVisibility(View.GONE);
        }

        listView = v.findViewById(R.id.chat_recent_list_view);

        mLayoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setItemAnimator(noChangeRecyclerViewItemAnimator());
        listView.setClipToPadding(false);
        listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, 0);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        emptyLayout = v.findViewById(R.id.linear_empty_layout_chat_recent);
        emptyTextView = v.findViewById(R.id.empty_text_chat_recent);

        String textToShow = context.getString(R.string.recent_chat_empty);
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'" +
                    ColorUtils.getColorHexString(requireActivity(), R.color.grey_300_grey_600)
                    + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'" +
                    ColorUtils.getColorHexString(requireActivity(), R.color.grey_900_grey_100)
                    + "\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
        }
        Spanned resultB = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
        emptyTextView.setText(resultB);

        LinearLayout.LayoutParams emptyTextViewParams2 = (LinearLayout.LayoutParams) emptyTextView.getLayoutParams();
        emptyTextViewParams2.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics));
        emptyTextView.setLayoutParams(emptyTextViewParams2);

        emptyImageView = v.findViewById(R.id.empty_image_view_recent);
        emptyImageView.setImageResource(R.drawable.empty_chat_message_portrait);

        megaChatApi.signalPresenceActivity();

        viewModel = new ViewModelProvider(this).get(ChatExplorerViewModel.class);

        resetSearch();

        collectFlows();

        setChats();

        return v;
    }

    @Override
    public void checkScroll() {
        if (listView == null) return;

        boolean canScroll = listView.canScrollVertically(SCROLLING_UP_DIRECTION) || !uiState.getSelectedItems().isEmpty();
        boolean addLayoutVisible = (addLayout != null && addLayout.getVisibility() == View.VISIBLE);
        float elevation = getResources().getDimension(R.dimen.toolbar_elevation);

        if (context instanceof FileExplorerActivity) {
            if (canScroll) {
                addLayout.setBackgroundColor(ColorUtils.getColorForElevation(context, elevation));
            } else {
                addLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
            }
            ((FileExplorerActivity) context).changeActionBarElevation(canScroll, CHAT_FRAGMENT);
        } else if (context instanceof ChatExplorerActivity && addLayoutVisible) {
            addLayout.setElevation(canScroll ? elevation : 0);

            AppBarLayout aB = ((ChatExplorerActivity) context).findViewById(R.id.app_bar_layout_chat_explorer);
            aB.setElevation(canScroll ? elevation : 0);
        }
    }

    private void setFirstLayoutVisibility(int visibility) {
        newGroupButton.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            addedList.setVisibility(View.GONE);
        } else if (visibility == View.GONE) {
            addedList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (megaChatApi != null) {
            if (context instanceof ChatExplorerActivity) {
                ((ChatExplorerActivity) context).composite.clear();
            } else if (context instanceof FileExplorerActivity) {
                ((FileExplorerActivity) context).composite.clear();
            }
        }
    }

    private void collectFlows() {
        ViewExtensionsKt.collectFlow(
                this,
                viewModel.getUiState(),
                Lifecycle.State.STARTED,
                uiState -> {
                    if (this.uiState != null && this.uiState.getItems() != uiState.getItems() && !uiState.isItemUpdated()) {
                        ArrayList<ChatExplorerListItem> items = new ArrayList<>(uiState.getItems());
                        if (adapterList == null) {
                            Timber.w("AdapterList is NULL");
                            adapterList = new MegaListChatExplorerAdapter(context, chatExplorerFragment, items, listView);
                        } else {
                            adapterList.setItems(items);
                        }

                        if (adapterAdded == null) {
                            adapterAdded = new MegaChipChatExplorerAdapter(
                                    context,
                                    chatExplorerFragment,
                                    new ArrayList<>(uiState.getSelectedItems())
                            );
                        } else {
                            adapterAdded.setItems(new ArrayList<>(uiState.getSelectedItems()));
                        }

                        addedList.setAdapter(adapterAdded);

                        if (!uiState.getSelectedItems().isEmpty()) {
                            int selectedItemCount = 0;
                            for (ChatExplorerListItem item : uiState.getItems()) {
                                if (item.isSelected()) {
                                    selectedItemCount++;
                                    int position = adapterList.getPosition(item);
                                    if (position != -1) {
                                        adapterList.toggleSelection(position);
                                    }
                                }

                                if (selectedItemCount == uiState.getSelectedItems().size()) break;
                            }
                            if (context instanceof FileExplorerActivity) {
                                ((FileExplorerActivity) context).hideTabs(true, CHAT_FRAGMENT);
                            }
                        }

                        setFinalViews(uiState.getItems());
                    }
                    this.uiState = uiState;
                    return Unit.INSTANCE;
                });

        ViewExtensionsKt.collectFlow(
                this,
                viewModel.getSearchUiState(),
                Lifecycle.State.STARTED,
                searchUiState -> {
                    if (adapterList != null) {
                        adapterList.setSearchSelectedItems(searchUiState.getSelectedItems());
                        if (adapterList.isSearchEnabled()) {
                            adapterList.setItems(new ArrayList<>(searchUiState.getItems()));
                            setListVisibility();
                        }
                    }
                    return Unit.INSTANCE;
                });
    }

    public void setChats() {
        Timber.d("setChats");

        emptyTextView.setVisibility(View.GONE);
        contentLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        viewModel.getChats();
    }

    public ArrayList<ChatExplorerListItem> getAddedChats() {
        if (!uiState.getSelectedItems().isEmpty()) {
            return new ArrayList<>(uiState.getSelectedItems());
        }
        return null;
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);
        megaChatApi.signalPresenceActivity();

        if (adapterList == null || adapterList.getItemCount() <= 0) {
            return;
        }

        ChatExplorerListItem item = adapterList.getItem(position);

        if (adapterList.isSearchEnabled()) {
            if (context instanceof ChatExplorerActivity) {
                ((ChatExplorerActivity) context).collapseSearchView();
            } else if (context instanceof FileExplorerActivity) {
                ((FileExplorerActivity) context).collapseSearchView();
            }
            if (!adapterList.getItems().equals(uiState.getItems())) {
                adapterList.setItems(new ArrayList<>(uiState.getItems()));
            }
            int togglePosition = adapterList.getPosition(item);
            adapterList.toggleSelection(togglePosition);
        } else {
            adapterList.toggleSelection(position);
        }


        if (item != null && !uiState.getSelectedItems().contains(item)) {
            viewModel.addSelectedItem(item);

            if (uiState.getSelectedItems().size() == 1) {
                checkScroll();
            }

            adapterAdded.setItems(new ArrayList<>(uiState.getSelectedItems()));
            setFirstLayoutVisibility(View.GONE);

            if (context instanceof ChatExplorerActivity) {
                ((ChatExplorerActivity) context).showFabButton(true);
                ((ChatExplorerActivity) context).setToolbarSubtitle(getString(R.string.selected_items, uiState.getSelectedItems().size()));
            } else if (context instanceof FileExplorerActivity) {
                if (uiState.getSelectedItems().size() == 1) {
                    ((FileExplorerActivity) context).hideTabs(true, CHAT_FRAGMENT);
                }

                ((FileExplorerActivity) context).showFabButton(true);
                ((FileExplorerActivity) context).setToolbarSubtitle(getString(R.string.selected_items, uiState.getSelectedItems().size()));
            }
        } else if (uiState.getSelectedItems().contains(item)) {
            deleteItem(item);

            if (uiState.getSelectedItems().isEmpty()) {
                checkScroll();
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity) activity).getSupportActionBar();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity) context).getSupportActionBar();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if (listView.getLayoutManager() != null) {
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, listView.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    public void onPause() {
        Timber.d("onPause");
        lastFirstVisiblePosition = ((LinearLayoutManager) listView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        super.onPause();
    }

    @Override
    public void onResume() {
        Timber.d("lastFirstVisiblePosition: %s", lastFirstVisiblePosition);
        if (lastFirstVisiblePosition > 0) {
            (listView.getLayoutManager()).scrollToPosition(lastFirstVisiblePosition);
        } else {
            (listView.getLayoutManager()).scrollToPosition(0);
        }
        lastFirstVisiblePosition = 0;
        super.onResume();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Timber.d("onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
            listView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
        }
    }

    private long getMegaContactHandle(ContactItemUiState contact) {
        long handle = -1;
        if (contact != null) {
            if (contact.getUser() != null && contact.getUser().getHandle() != -1) {
                handle = contact.getUser().getHandle();
            } else if (contact.getContact() != null && contact.getContact().getEmail() != null) {
                handle = contact.getContact().getUserId();
            }
        }
        return handle;
    }

    public void updateLastGreenContact(long userhandle, String formattedDate) {
        if (adapterList != null && adapterList.getItems() != null) {
            ListIterator<ChatExplorerListItem> itrReplace = adapterList.getItems().listIterator();
            while (itrReplace.hasNext()) {
                ChatExplorerListItem itemToUpdate = itrReplace.next();
                if (itemToUpdate != null) {
                    if (itemToUpdate.getContactItem() != null) {
                        if (getMegaContactHandle(itemToUpdate.getContactItem()) == userhandle) {
                            itemToUpdate.getContactItem().setLastGreen(formattedDate);
                            adapterList.updateItemContactStatus(itrReplace.nextIndex() - 1);
                            viewModel.updateItemLastGreenDateByContact(itemToUpdate.getContactItem(), formattedDate);
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void deleteItemPosition(int position) {
        ChatExplorerListItem item = adapterAdded.getItem(position);
        if (adapterList != null) {
            int positionItem = adapterList.getPosition(item);
            if (positionItem != -1) {
                adapterList.toggleSelection(positionItem);
            }
        }
        deleteItem(item);
    }

    private void deleteItem(ChatExplorerListItem item) {
        if (item != null) {
            viewModel.removeSelectedItem(item);
            adapterAdded.setItems(new ArrayList<>(uiState.getSelectedItems()));

            if (!uiState.getSelectedItems().isEmpty()) {
                setFirstLayoutVisibility(View.GONE);
                if (context instanceof ChatExplorerActivity) {
                    ((ChatExplorerActivity) context).showFabButton(true);
                    ((ChatExplorerActivity) context).setToolbarSubtitle(getString(R.string.selected_items, uiState.getSelectedItems().size()));
                } else if (context instanceof FileExplorerActivity) {
                    ((FileExplorerActivity) context).showFabButton(true);
                    ((FileExplorerActivity) context).setToolbarSubtitle(getString(R.string.selected_items, uiState.getSelectedItems().size()));
                }
            } else {
                setFirstLayoutVisibility(View.VISIBLE);
                if (context instanceof ChatExplorerActivity) {
                    ((ChatExplorerActivity) context).showFabButton(false);
                    ((ChatExplorerActivity) context).setToolbarSubtitle(null);
                } else if (context instanceof FileExplorerActivity) {
                    ((FileExplorerActivity) context).hideTabs(false, CHAT_FRAGMENT);
                    ((FileExplorerActivity) context).showFabButton(false);
                    ((FileExplorerActivity) context).setToolbarSubtitle(null);
                }
            }
        }
    }

    private void setFinalViews(List<ChatExplorerListItem> items) {
        int position;
        if (!items.isEmpty()) {
            position = (int) items.stream().filter(ChatExplorerListItem::isRecent).count();
        } else {
            position = -1;
        }
        positionDividerItemDecoration = new PositionDividerItemDecoration(context, outMetrics, position);
        simpleDividerItemDecoration = new SimpleDividerItemDecoration(context);
        listView.addItemDecoration(positionDividerItemDecoration);
        listView.setAdapter(adapterList);

        if (adapterAdded.getItemCount() == 0) {
            setFirstLayoutVisibility(View.VISIBLE);

            if (context instanceof ChatExplorerActivity) {
                ((ChatExplorerActivity) context).setToolbarSubtitle(null);
            } else if (context instanceof FileExplorerActivity) {
                ((FileExplorerActivity) context).setToolbarSubtitle(null);
            }
        } else {
            setFirstLayoutVisibility(View.GONE);

            if (context instanceof ChatExplorerActivity) {
                ((ChatExplorerActivity) context).showFabButton(true);
                ((ChatExplorerActivity) context).setToolbarSubtitle(getString(R.string.selected_items, uiState.getSelectedItems().size()));
            } else if (context instanceof FileExplorerActivity) {
                ((FileExplorerActivity) context).showFabButton(true);
                ((FileExplorerActivity) context).setToolbarSubtitle(getString(R.string.selected_items, uiState.getSelectedItems().size()));
            }
        }

        if (context instanceof ChatExplorerActivity) {
            ((ChatExplorerActivity) context).isPendingToOpenSearchView();
        } else if (context instanceof FileExplorerActivity) {
            ((FileExplorerActivity) context).isPendingToOpenSearchView();
        }
        contentLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        setListVisibility();
    }

    public void enableSearch(boolean enable) {
        if (enable) {
            listView.removeItemDecoration(positionDividerItemDecoration);
            listView.addItemDecoration(simpleDividerItemDecoration);
            listView.invalidateItemDecorations();
            search("");
            if (addLayout.getVisibility() == View.VISIBLE) {
                addLayout.setVisibility(View.GONE);
            }
            if (adapterList != null && !adapterList.isSearchEnabled()) {
                adapterList.setSearchEnabled(enable);
            }
            if (context instanceof ChatExplorerActivity) {
                ((ChatExplorerActivity) context).showFabButton(false);
            } else if (context instanceof FileExplorerActivity) {
                ((FileExplorerActivity) context).showFabButton(false);
            }
        } else {
            resetSearch();
            listView.removeItemDecoration(simpleDividerItemDecoration);
            listView.addItemDecoration(positionDividerItemDecoration);
            listView.invalidateItemDecorations();

            if (addLayout.getVisibility() == View.GONE) {
                addLayout.setVisibility(View.VISIBLE);
            }

            if (adapterList != null && adapterList.isSearchEnabled()) {
                adapterList.setSearchEnabled(enable);
                if (!adapterList.getItems().equals(uiState.getItems())) {
                    adapterList.setItems(new ArrayList<>(uiState.getItems()));
                }
            }

            if (adapterAdded != null && adapterAdded.getItemCount() == 0) {
                setFirstLayoutVisibility(View.VISIBLE);
            } else {
                setFirstLayoutVisibility(View.GONE);

                if (context instanceof ChatExplorerActivity) {
                    ((ChatExplorerActivity) context).showFabButton(true);
                } else if (context instanceof FileExplorerActivity) {
                    ((FileExplorerActivity) context).showFabButton(true);
                }
            }
        }
        setListVisibility();
    }

    private void resetSearch() {
        viewModel.search(null);
    }

    private void setListVisibility() {
        if (adapterList == null) return;
        if (adapterList.getItemCount() == 0) {
            Timber.d("adapterList.getItemCount() == 0");
            listView.setVisibility(View.GONE);
            addLayout.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            Timber.d("adapterList.getItemCount() NOT = 0");
            listView.setVisibility(View.VISIBLE);
            if (!adapterList.isSearchEnabled()) {
                addLayout.setVisibility(View.VISIBLE);
            }
            emptyLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Clears all the selected items.
     */
    public void clearSelections() {
        if (adapterList != null) {
            adapterList.clearSelections();
        }

        adapterAdded = null;

        viewModel.clearSelections();
    }

    public void search(String s) {
        viewModel.search(s);
    }
}
