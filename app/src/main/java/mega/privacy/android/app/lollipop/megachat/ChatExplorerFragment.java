package mega.privacy.android.app.lollipop.megachat;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChipChatExplorerAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatExplorerAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

public class ChatExplorerFragment extends Fragment {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    DatabaseHandler dbH;

    Context context;
    ActionBar aB;
    RecyclerView listView;
    MegaListChatExplorerAdapter adapterList;
    RelativeLayout mainRelativeLayout;

    LinearLayoutManager mLayoutManager;

    ArrayList<MegaChatListItem> chats;
    ArrayList<MegaChatListItem> archievedChats;
    ArrayList<MegaContactAdapter> contacts;
    ArrayList<ChatExplorerListItem> items;
    ArrayList<ChatExplorerListItem> addedItems;

    int lastFirstVisiblePosition;

    //Empty screen
    TextView emptyTextView;
    LinearLayout emptyLayout;
    TextView emptyTextViewInvite;
    ImageView emptyImageView;
    int chatStatus;
    Button inviteButton;

    boolean chatEnabled = true;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    AppBarLayout addLayout;
    Button newGroupButton;
    RecyclerView addedList;
    MegaChipChatExplorerAdapter adapterAdded;
    LinearLayoutManager addedLayoutManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        if(Util.isChatEnabled()){
            chatEnabled=true;
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
                if (context instanceof ChatExplorerActivity) {
                    megaChatApi.addChatListener((ChatExplorerActivity) context);
                }
                else if (context instanceof FileExplorerActivityLollipop) {
//                    megaChatApi.addChatListener((FileExplorerActivityLollipop) context);
                }
            }
        }
        else{
            log("Chat not enabled!");
            chatEnabled=false;
        }
    }


    public static ChatExplorerFragment newInstance() {
        log("newInstance");
        ChatExplorerFragment fragment = new ChatExplorerFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.chat_recent_tab, container, false);

        addLayout = (AppBarLayout) v.findViewById(R.id.linear_layout_add);
        addedList = (RecyclerView) v.findViewById(R.id.contact_adds_recycler_view);
        newGroupButton = (Button) v.findViewById(R.id.new_group_button);
        if (context instanceof ChatExplorerActivity) {
            addLayout.setVisibility(View.VISIBLE);
            addedLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            addedList.setLayoutManager(addedLayoutManager);
            addedList.setHasFixedSize(true);
            addedList.setItemAnimator(new DefaultItemAnimator());
            addedList.setClipToPadding(false);

            newGroupButton.setOnClickListener((ChatExplorerActivity) context);
            setFirstLayoutVisibility(View.VISIBLE);
        }
        else {
            addLayout.setVisibility(View.GONE);
        }

        listView = (RecyclerView) v.findViewById(R.id.chat_recent_list_view);

        listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
        mLayoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setHasFixedSize(true);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setClipToPadding(false);
        listView.setPadding(0,Util.scaleHeightPx(8, outMetrics),0, 0);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (context instanceof FileExplorerActivityLollipop) {
                    if (listView.canScrollVertically(-1)) {
                        ((FileExplorerActivityLollipop) context).changeActionBarElevation(true);
                    } else {
                        ((FileExplorerActivityLollipop) context).changeActionBarElevation(false);
                    }
                }
                else if (context instanceof ChatExplorerActivity && addLayout != null && addLayout.getVisibility() == View.VISIBLE) {
                    if (listView.canScrollVertically(-1)) {
                        addLayout.setElevation(Util.px2dp(4, outMetrics));
                    } else {
                        addLayout.setElevation(0);
                    }
                }
            }
        });

        emptyLayout = (LinearLayout) v.findViewById(R.id.linear_empty_layout_chat_recent);
        emptyTextViewInvite = (TextView) v.findViewById(R.id.empty_text_chat_recent_invite);
        emptyTextViewInvite.setWidth(Util.scaleWidthPx(236, outMetrics));
        emptyTextView = (TextView) v.findViewById(R.id.empty_text_chat_recent);

        String textToShow = String.format(context.getString(R.string.recent_chat_empty));
        try{
            textToShow = textToShow.replace("[A]", "<font color=\'#7a7a7a\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'#000000\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        }
        catch (Exception e){}
        Spanned resultB = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultB = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            resultB = Html.fromHtml(textToShow);
        }
        emptyTextView.setText(resultB);

        LinearLayout.LayoutParams emptyTextViewParams1 = (LinearLayout.LayoutParams)emptyTextViewInvite.getLayoutParams();
        emptyTextViewParams1.setMargins(0, Util.scaleHeightPx(50, outMetrics), 0, Util.scaleHeightPx(24, outMetrics));
        emptyTextViewInvite.setLayoutParams(emptyTextViewParams1);

        LinearLayout.LayoutParams emptyTextViewParams2 = (LinearLayout.LayoutParams)emptyTextView.getLayoutParams();
        emptyTextViewParams2.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics));
        emptyTextView.setLayoutParams(emptyTextViewParams2);

        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_view_recent);
        emptyImageView.setImageResource(R.drawable.ic_empty_chat_list);

        mainRelativeLayout = (RelativeLayout) v.findViewById(R.id.main_relative_layout);
        inviteButton = (Button) v.findViewById(R.id.invite_button);
        inviteButton.setVisibility(View.GONE);

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        this.setChats();

        return v;
    }

    void setFirstLayoutVisibility (int visibility) {
        newGroupButton.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            addedList.setVisibility(View.GONE);
        }
        else if (visibility == View.GONE) {
            addedList.setVisibility(View.VISIBLE);
        }
    }

    public MegaContactAdapter getContact(MegaChatListItem chat) {
        long handle = chat.getPeerHandle();
        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(handle);
        MegaUser user = megaApi.getContact(userHandleEncoded);

//        Maybe the contact is not my contact already
        if (user == null) {
            log("Chat "+chat.getTitle()+" with PeerHandle: "+handle+" is NULL");
            return null;
        }

        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(handle+""));
        String fullName = "";
        if(contactDB!=null){
            ContactController cC = new ContactController(context);
            fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), user.getEmail());
        }
        else{
            fullName = user.getEmail();
        }

        if (handle != -1) {
            int userStatus = megaChatApi.getUserOnlineStatus(handle);
            if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                log("Request last green for user");
                megaChatApi.requestLastGreen(handle, null);
            }
        }

        return new MegaContactAdapter(contactDB, user, fullName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (megaChatApi != null) {
            if (context instanceof ChatExplorerActivity) {
                megaChatApi.removeChatListener((ChatExplorerActivity) context);
            }
//            else if (context instanceof FileExplorerActivityLollipop) {
//                megaChatApi.removeChatListener((FileExplorerActivityLollipop) context);
//            }
        }
    }

    public void setChats(){
        log("setChats");

        if(isAdded()){
            if (items != null) {
                items.clear();
            }
            else {
                items = new ArrayList<>();
            }

            if (addedItems != null) {
                addedItems.clear();
            }
            else {
                addedItems = new ArrayList<>();
            }

            if(chats!=null){
                chats.clear();
            }
            else{
                chats = new ArrayList<>();
            }

            if (archievedChats != null) {
                archievedChats.clear();
            }
            else {
                archievedChats =  new ArrayList<>();
            }

            if (contacts != null) {
                contacts.clear();
            }
            else {
                contacts = new ArrayList<>();
            }

            int initState = megaChatApi.getInitState();
            log("Init state is: "+initState);
            chats = megaChatApi.getActiveChatListItems();
            archievedChats = megaChatApi.getArchivedChatListItems();
            getVisibleMEGAContacts();

            for (MegaChatListItem chat : chats) {
                if (chat.isGroup()) {
                    items.add(new ChatExplorerListItem(chat));
                }
                else {
                    items.add(new ChatExplorerListItem(chat, getContact(chat)));
                }
            }

            for (MegaChatListItem archieved : archievedChats) {
                if (archieved.isGroup()) {
                    items.add(new ChatExplorerListItem(archieved));
                }
                else {
                    items.add(new ChatExplorerListItem(archieved, getContact(archieved)));
                }
            }

            for (MegaContactAdapter contact : contacts) {
                if (contact.getMegaUser() != null) {
                    MegaChatRoom chat = megaChatApi.getChatRoomByUser(contact.getMegaUser().getHandle());
                    if (chat == null) {
                        if (contact.getMegaUser() != null) {
                            long handle = contact.getMegaUser().getHandle();
                            if (handle != -1) {
                                int userStatus = megaChatApi.getUserOnlineStatus(handle);
                                if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                                    log("Request last green for user");
                                    megaChatApi.requestLastGreen(handle, (ChatExplorerActivity) context);
                                }
                            }
                        }
                        items.add(new ChatExplorerListItem(contact));
                    }
                }
            }

            log("items no: "+items.size());

            //Order by title
            Collections.sort(items, new Comparator<ChatExplorerListItem> (){

                public int compare(ChatExplorerListItem c1, ChatExplorerListItem c2) {
                    String n1 = c1.getTitle();
                    String n2 = c2.getTitle();

                    int res = String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
                    if (res == 0) {
                        res = n1.compareTo(n2);
                    }
                    return res;
                }
            });


            if (adapterAdded == null) {
                adapterAdded = new MegaChipChatExplorerAdapter(context, this, addedItems);
            }
            else {
                adapterAdded.setItems(addedItems);
            }

            addedList.setAdapter(adapterAdded);

            if (adapterAdded.getItemCount() == 0) {
                setFirstLayoutVisibility(View.VISIBLE);
            }
            else {
                setFirstLayoutVisibility(View.GONE);
            }

            if (adapterList == null){
                log("adapterList is NULL");
                adapterList = new MegaListChatExplorerAdapter(context, this, items);
            }
            else{
                adapterList.setItems(items);
            }

            listView.setAdapter(adapterList);

            if (adapterList.getItemCount() == 0){
                log("adapterList.getItemCount() == 0");
                listView.setVisibility(View.GONE);
                addLayout.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
            }
            else{
                log("adapterList.getItemCount() NOT = 0");
                listView.setVisibility(View.VISIBLE);
                addLayout.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
            }
        }
    }

    void getVisibleMEGAContacts () {
        ArrayList<MegaUser> contactsMEGA = megaApi.getContacts();
        for (int i=0;i<contactsMEGA.size();i++){
            log("contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
            if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                String fullName = "";
                if(contactDB!=null){
                    ContactController cC = new ContactController(context);
                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                }
                else{
                    fullName = contactsMEGA.get(i).getEmail();
                }

                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                contacts.add(megaContactAdapter);
            }
        }
    }

    public ArrayList<ChatExplorerListItem> getAddedChats () {

        if (addedItems != null) {
            return addedItems;
        }

        return null;
    }

//    public ArrayList<MegaChatListItem> getSelectedChats() {
//        log("getSelectedChats");
//
//        if(adapterList!=null){
//            return adapterList.getSelectedChats();
//        }
//        return null;
//    }

    /*
     * Clear all selected items
     */
//    public void clearSelections() {
//        log("clearSelections");
//        adapterList.clearSelections();
//    }
//
//    public void selectAll() {
//        if (adapterList != null) {
//            adapterList.selectAll();
//        }
//    }

    public void itemClick(int position) {
        log("itemClick");
        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if (adapterList != null && adapterList.getItemCount() > 0) {
            ChatExplorerListItem item = adapterList.getItem(position);

            addedItems.add(item);
            adapterAdded.setItems(addedItems);
            setFirstLayoutVisibility(View.GONE);
            items.remove(item);
            adapterList.setItems(items);

            if(context instanceof  ChatExplorerActivity){
                ((ChatExplorerActivity)context).showFabButton(true);
            }
            else{
                ((FileExplorerActivityLollipop)context).showFabButton(true);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)context).getSupportActionBar();
    }

    public String getParticipantFullName(MegaChatRoom chat, long i){
        String participantFirstName = chat.getPeerFirstname(i);
        String participantLastName = chat.getPeerLastname(i);

        if(participantFirstName==null){
            participantFirstName="";
        }
        if(participantLastName == null){
            participantLastName="";
        }

        if (participantFirstName.trim().length() <= 0){
            log("Participant1: "+participantFirstName);
            return participantLastName;
        }
        else{
            log("Participant2: "+participantLastName);
            return participantFirstName + " " + participantLastName;
        }
    }

//    @Override
//    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom item) {
//        log("onChatRoomUpdate");
//
////        MegaChatRoom chatUpdated = megaChatApi.getChatRoom(chat.getChatId());
////        if(chatUpdated!=null){
////            log("chat updated: "+chat.getTitle());
////            log("unread count: "+chat.getUnreadCount());
////            log("change type: "+chat.getChanges());
////        }
//
//        if(item.hasChanged(MegaChatRoom.CHANGE_TYPE_UNREAD_COUNT)){
//            log("CHANGE_TYPE_UNREAD_COUNT for the chat: "+item.getChatId());
//            if(item!=null){
//
//                if (adapterList == null || adapterList.getItemCount()==0){
//                    setChats();
//                }
//                else{
//                    long chatHandleToUpdate = item.getChatId();
//                    int indexToReplace = -1;
//                    ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
//                    while (itrReplace.hasNext()) {
//                        MegaChatListItem chat = itrReplace.next();
//                        if(chat!=null){
//                            if(chat.getChatId()==chatHandleToUpdate){
//                                indexToReplace = itrReplace.nextIndex()-1;
//                                break;
//                            }
//                        }
//                        else{
//                            break;
//                        }
//                    }
//                    if(indexToReplace!=-1){
//                        log("Index to replace: "+indexToReplace);
//                        MegaChatListItem chatToReplace = megaChatApi.getChatListItem(item.getChatId());
//                        log("Unread count: "+chatToReplace.getUnreadCount());
//                        chats.set(indexToReplace, chatToReplace);
//
//                        adapterList.modifyChat(chats, indexToReplace);
//                        adapterList.setPositionClicked(-1);
//
//                        if (adapterList.getItemCount() == 0){
//                            log("adapterList.getItemCount() == 0");
//                            listView.setVisibility(View.GONE);
//                            emptyLayout.setVisibility(View.VISIBLE);
//                        }
//                        else{
//                            log("adapterList.getItemCount() NOT = 0");
//                            listView.setVisibility(View.VISIBLE);
//                            emptyLayout.setVisibility(View.GONE);
//                        }
//                    }
//                }
//            }
//        }
//    }

//    @Override
//    public void onViewStateRestored(Bundle savedInstanceState) {
//        log("onViewStateRestored");
//        super.onViewStateRestored(savedInstanceState);
//
//        if(savedInstanceState != null)
//        {
//            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
//            listView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
//        }
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if(listView.getLayoutManager()!=null){
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, listView.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    public void onPause() {
        log("onPause");
        lastFirstVisiblePosition = ((LinearLayoutManager)listView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        super.onPause();
    }

    @Override
    public void onResume() {
        log("onResume: lastFirstVisiblePosition " +lastFirstVisiblePosition);
        if(lastFirstVisiblePosition>0){
            (listView.getLayoutManager()).scrollToPosition(lastFirstVisiblePosition);
        }else{
            (listView.getLayoutManager()).scrollToPosition(0);
        }
        lastFirstVisiblePosition=0;
        super.onResume();
    }

    public int getItemCount(){
        if(adapterList != null){
            return adapterList.getItemCount();
        }
        return 0;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        log("onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
            listView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
        }
    }

    public long getChatIdFrom(){
        if(context instanceof  ChatExplorerActivity){
            return ((ChatExplorerActivity)context).chatIdFrom;
        }
        return -1;
    }

    public long getMegaContactHandle (MegaContactAdapter contact) {
        long handle = -1;
        if (contact != null) {
            if (contact.getMegaUser() != null && contact.getMegaUser().getHandle() != -1) {
                handle = contact.getMegaUser().getHandle();
            }
            else if (contact.getMegaContactDB() != null && contact.getMegaContactDB().getMail() != null) {
                handle = Long.parseLong(contact.getMegaContactDB().getHandle());
            }
        }
        return handle;
    }

    public void updateLastGreenContact (long userhandle, String formattedDate) {
        ListIterator<ChatExplorerListItem> itrReplace = items.listIterator();
        while (itrReplace.hasNext()) {
            ChatExplorerListItem itemToUpdate = itrReplace.next();
            if (itemToUpdate != null) {
                if (itemToUpdate.getContact() != null) {
                    if (getMegaContactHandle(itemToUpdate.getContact()) == userhandle) {
                        itemToUpdate.getContact().setLastGreen(formattedDate);
                        break;
                    }
                }
                else {
                    continue;
                }
            } else {
                break;
            }
        }

        int indexToReplace = -1;
        if(adapterList!=null && adapterList.getItems() != null){
            ListIterator<ChatExplorerListItem> itrReplace2 = adapterList.getItems().listIterator();
            while (itrReplace2.hasNext()) {
                ChatExplorerListItem itemToUpdate = itrReplace2.next();
                if (itemToUpdate != null) {
                    if (itemToUpdate.getContact() != null) {
                        if (getMegaContactHandle(itemToUpdate.getContact()) == userhandle) {
                            itemToUpdate.getContact().setLastGreen(formattedDate);
                            indexToReplace = itrReplace2.nextIndex() - 1;
                            break;
                        }
                    }
                    else {
                        continue;
                    }
                } else {
                    break;
                }
            }
            if (indexToReplace != -1) {
                adapterList.updateItemContactStatus(indexToReplace);
            }
        }
    }

    public void deleteItem(int position) {
        ChatExplorerListItem item = adapterAdded.getItem(position);
        if (item != null) {
            addedItems.remove(item);
            adapterAdded.setItems(addedItems);
            items.add(item);
            Collections.sort(items, new Comparator<ChatExplorerListItem> (){

                public int compare(ChatExplorerListItem c1, ChatExplorerListItem c2) {
                    String n1 = c1.getTitle();
                    String n2 = c2.getTitle();

                    int res = String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
                    if (res == 0) {
                        res = n1.compareTo(n2);
                    }
                    return res;
                }
            });
            adapterList.setItems(items);

            if (addedItems.size() > 0) {
                setFirstLayoutVisibility(View.GONE);
                if(context instanceof  ChatExplorerActivity){
                    ((ChatExplorerActivity)context).showFabButton(true);
                }
                else{
                    ((FileExplorerActivityLollipop)context).showFabButton(true);
                }
            }
            else {
                setFirstLayoutVisibility(View.VISIBLE);
                if(context instanceof  ChatExplorerActivity){
                    ((ChatExplorerActivity)context).showFabButton(false);
                }
                else{
                    ((FileExplorerActivityLollipop)context).showFabButton(false);
                }
            }
        }
    }

    private static void log(String log) {
        Util.log("ChatExplorerFragment", log);
    }
}
