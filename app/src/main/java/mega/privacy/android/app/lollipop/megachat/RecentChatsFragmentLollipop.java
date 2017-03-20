package mega.privacy.android.app.lollipop.megachat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;

public class RecentChatsFragmentLollipop extends Fragment implements View.OnClickListener {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    DatabaseHandler dbH;

    Context context;
    ActionBar aB;
    RecyclerView listView;
    MegaListChatLollipopAdapter adapterList;
    RelativeLayout mainRelativeLayout;

    RecyclerView.LayoutManager mLayoutManager;

    ArrayList<MegaChatListItem> chats;

    int lastFirstVisiblePosition;

    //Empty screen
    TextView emptyTextView;
    LinearLayout emptyLayout;
    TextView emptyTextViewInvite;
    ImageView emptyImageView;
    Button inviteButton;
    int chatStatus;

    boolean chatEnabled = true;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    private ActionMode actionMode;

    public void activateActionMode(){
        log("activateActionMode");
        if (!adapterList.isMultipleSelect()){
            adapterList.setMultipleSelect(true);
            actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
        }
    }

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
            }
        }
        else{
            log("Chat not enabled!");
            chatEnabled=false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        if(aB!=null){
            aB.setTitle(getString(R.string.section_chat));
            aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
            ((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
        }

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.chat_recent_tab, container, false);

        listView = (RecyclerView) v.findViewById(R.id.chat_recent_list_view);

        listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
        mLayoutManager = new MegaLinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setHasFixedSize(true);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setClipToPadding(false);

        emptyLayout = (LinearLayout) v.findViewById(R.id.linear_empty_layout_chat_recent);
        emptyTextViewInvite = (TextView) v.findViewById(R.id.empty_text_chat_recent_invite);
        emptyTextViewInvite.setWidth(Util.scaleWidthPx(236, outMetrics));
        emptyTextView = (TextView) v.findViewById(R.id.empty_text_chat_recent);

        LinearLayout.LayoutParams emptyTextViewParams1 = (LinearLayout.LayoutParams)emptyTextViewInvite.getLayoutParams();
        emptyTextViewParams1.setMargins(0, Util.scaleHeightPx(50, outMetrics), 0, Util.scaleHeightPx(24, outMetrics));
        emptyTextViewInvite.setLayoutParams(emptyTextViewParams1);

        LinearLayout.LayoutParams emptyTextViewParams2 = (LinearLayout.LayoutParams)emptyTextView.getLayoutParams();
        emptyTextViewParams2.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics));
        emptyTextView.setLayoutParams(emptyTextViewParams2);

        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_view_chat);

        inviteButton = (Button) v.findViewById(R.id.invite_button);
        inviteButton.setOnClickListener(this);

        mainRelativeLayout = (RelativeLayout) v.findViewById(R.id.main_relative_layout);

        if(chatEnabled){
            log("Chat ENABLED");
            //Get chat status
            chatStatus = megaChatApi.getOnlineStatus();
            log("chatStatus: "+chatStatus);

            if(aB!=null){
                log("My own status is: "+chatStatus);
                switch(chatStatus){
                    case MegaChatApi.STATUS_ONLINE:{
                        aB.setSubtitle(getString(R.string.online_status));
                        break;
                    }
                    case MegaChatApi.STATUS_AWAY:{
                        aB.setSubtitle(getString(R.string.away_status));
                        break;
                    }
                    case MegaChatApi.STATUS_BUSY:{
                        aB.setSubtitle(getString(R.string.busy_status));
                        break;
                    }
                    case MegaChatApi.STATUS_OFFLINE:{
                        aB.setSubtitle(getString(R.string.offline_status));
                        break;
                    }
                    default:{
                        aB.setSubtitle(getString(R.string.recovering_info));
                        break;
                    }

                }
            }

            if(megaChatApi.isSignalActivityRequired()){
                megaChatApi.signalPresenceActivity();
            }

            this.setChats();
        }
        else{
            log("Chat DISABLED");
            if(Util.isOnline(context)){
                showDisableChatScreen();
            }
            else{
                showNoConnectionScreen();
            }
        }

        return v;
    }

    public static RecentChatsFragmentLollipop newInstance() {
        log("newInstance");
        RecentChatsFragmentLollipop fragment = new RecentChatsFragmentLollipop();
        return fragment;
    }

    public void setChats(){
        log("setChats");
//        ArrayList<MegaChatRoom> temporalChats = megaChatApi.getChatRooms();
//        chats.clear();
//
//        for(int i=0;i<chats.size();i++){
//            MegaChatRoom chat = chats.get(i);
//            int privilege = chat.getOwnPrivilege();
//            if(privilege==MegaChatRoom.PRIV_RM){
//                log("Borradooooo");
//            }
//            else if(privilege==MegaChatRoom.PRIV_UNKNOWN){
//                log("Unknow");
//            }
//            else{
//                log("otro privilegio");
//                chats.add(chat);
//            }
//        }

        if(isAdded()){
            if(chatEnabled){
                if(chats!=null){
                    chats.clear();
                }
                else{
                    chats = new ArrayList<MegaChatListItem>();
                }

                int initState = megaChatApi.getInitState();
                log("Init state is: "+initState);
                chats = megaChatApi.getActiveChatListItems();

                log("chats no: "+chats.size());

                //Order by last interaction
                Collections.sort(chats, new Comparator<MegaChatListItem> (){

                    public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                        long timestamp1 = c1.getLastTimestamp();
                        long timestamp2 = c2.getLastTimestamp();

                        long result = timestamp2 - timestamp1;
                        return (int)result;
                    }
                });

                if (adapterList == null){
                    adapterList = new MegaListChatLollipopAdapter(context, this, chats, listView, MegaListChatLollipopAdapter.ADAPTER_RECENT_CHATS);
                }
                else{
                    adapterList.setChats(chats);
                }
                listView.setAdapter(adapterList);
                adapterList.setPositionClicked(-1);

                if (adapterList.getItemCount() == 0){
                    log("adapterList.getItemCount() == 0");
                    listView.setVisibility(View.GONE);
                    emptyLayout.setVisibility(View.VISIBLE);
                }
                else{
                    log("adapterList.getItemCount() NOT = 0");
                    listView.setVisibility(View.VISIBLE);
                    emptyLayout.setVisibility(View.GONE);
                }
            }
            else{
                if(Util.isOnline(context)){
                    showDisableChatScreen();
                }
                else{
                    showNoConnectionScreen();
                }
            }
        }
    }

    public void showDisableChatScreen(){

        listView.setVisibility(View.GONE);
        ((ManagerActivityLollipop)context).hideFabButton();
        emptyTextViewInvite.setText(getString(R.string.recent_chat_empty_enable_chat));
        inviteButton.setText(getString(R.string.recent_chat_enable_chat_button));
        emptyTextView.setText(R.string.recent_chat_enable_chat);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    public void showNoConnectionScreen(){

        listView.setVisibility(View.GONE);
        ((ManagerActivityLollipop)context).hideFabButton();
        emptyTextViewInvite.setText(getString(R.string.recent_chat_empty_no_connection_title));
        inviteButton.setVisibility(View.GONE);
        emptyTextView.setText(R.string.recent_chat_empty_no_connection_text);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        log("onClick");

        switch (v.getId()) {
            case R.id.invite_button:{
                if(chatEnabled){
                    ((ManagerActivityLollipop)context).chooseAddContactDialog(false);
                    if(megaChatApi.isSignalActivityRequired()){
                        megaChatApi.signalPresenceActivity();
                    }
                }
                else{
                    ChatController chatController = new ChatController(context);
                    log("onCLick: enableChat");
                    chatController.enableChat();
                    getActivity().supportInvalidateOptionsMenu();
                    chatEnabled=!chatEnabled;
                    ((ManagerActivityLollipop)context).enableChat();
//                    setChats();
                }

                break;
            }
        }
    }

    /////Multiselect/////
    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<MegaChatListItem> chats = adapterList.getSelectedChats();

            if(megaChatApi.isSignalActivityRequired()){
                megaChatApi.signalPresenceActivity();
            }

            switch(item.getItemId()){
                case R.id.cab_menu_select_all:{
                    selectAll();
                    actionMode.invalidate();
                    break;
                }
                case R.id.cab_menu_unselect_all:{
                    clearSelections();
                    hideMultipleSelect();
                    actionMode.invalidate();
                    break;
                }
                case R.id.cab_menu_mute:{
                    clearSelections();
                    hideMultipleSelect();
                    ChatController chatC = new ChatController(context);
                    chatC.muteChats(chats);
//                    setChats();
                    break;
                }
                case R.id.cab_menu_archive:{
                    clearSelections();
                    hideMultipleSelect();
                    //Archive
                    Toast.makeText(context, "Not yet implemented! Archive: "+chats.size()+" chats",Toast.LENGTH_SHORT).show();
                    break;
                }
                case R.id.cab_menu_delete:{
                    clearSelections();
                    hideMultipleSelect();
                    //Delete
                    Toast.makeText(context, "Not yet implemented! Delete: "+chats.size()+" chats",Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.recent_chat_action, menu);
            ((ManagerActivityLollipop)context).hideFabButton();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            clearSelections();
            adapterList.setMultipleSelect(false);
            ((ManagerActivityLollipop)context).showFabButton();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<MegaChatListItem> selected = adapterList.getSelectedChats();

            if (selected.size() != 0) {
                menu.findItem(R.id.cab_menu_mute).setVisible(true);
                menu.findItem(R.id.cab_menu_archive).setVisible(true);
                menu.findItem(R.id.cab_menu_delete).setVisible(true);

                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                if(selected.size()==adapterList.getItemCount()){
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                }
                else if(selected.size()==1){
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    unselect.setTitle(getString(R.string.action_unselect_one));
                    unselect.setVisible(true);
                }
                else{
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                }

            }
            else{
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }

            return false;
        }

    }

    public boolean showSelectMenuItem(){
        if (adapterList != null){
            return adapterList.isMultipleSelect();
        }

        return false;
    }

    /*
     * Clear all selected items
     */
    private void clearSelections() {
        if(adapterList.isMultipleSelect()){
            adapterList.clearSelections();
        }
    }

    private void updateActionModeTitle() {
        if (actionMode == null || getActivity() == null) {
            return;
        }
        List<MegaChatListItem> chats = adapterList.getSelectedChats();

        actionMode.setTitle(context.getString(R.string.selected_items, chats.size()));

        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            e.printStackTrace();
            log("oninvalidate error");
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        log("hideMultipleSelect");
        adapterList.setMultipleSelect(false);
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void selectAll() {
        if (adapterList != null) {
            if (adapterList.isMultipleSelect()) {
                adapterList.selectAll();
            } else {
                adapterList.setMultipleSelect(true);
                adapterList.selectAll();

                actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
            }

            updateActionModeTitle();
        }
    }

    public void itemClick(int position) {
        log("itemClick");
        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }
        if (adapterList.isMultipleSelect()){
            adapterList.toggleSelection(position);
            List<MegaChatListItem> chats = adapterList.getSelectedChats();
            if (chats.size() > 0){
                updateActionModeTitle();
            }
        }
        else{
            log("open chat");
            Intent intent = new Intent(context, ChatActivityLollipop.class);
            intent.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra("CHAT_ID", chats.get(position).getChatId());
            this.startActivity(intent);
        }
    }
    /////END Multiselect/////

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

    public void listItemUpdate(MegaChatListItem item) {
        log("listItemUpdate: "+item.getTitle());

        if(!isAdded()){
            log("return!");
            return;
        }

        if(!(((ManagerActivityLollipop)context).getDrawerItem()== ManagerActivityLollipop.DrawerItem.CHAT)){
            log("not CHAT shown!");
            return;
        }

        if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_STATUS)){
            log("listItemUpdate: Change status: MegaChatListItem.CHANGE_TYPE_STATUS");

            if(item!=null) {

                if(!(item.isGroup())){
                    long chatHandleToUpdate = item.getChatId();
                    int indexToReplace = -1;
                    ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                    while (itrReplace.hasNext()) {
                        MegaChatListItem chat = itrReplace.next();
                        if (chat != null) {
                            if (chat.getChatId() == chatHandleToUpdate) {
                                indexToReplace = itrReplace.nextIndex() - 1;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (indexToReplace != -1) {
                        log("Index to replace: " + indexToReplace);
                        log("Item status: "+item.getOnlineStatus());
                        chats.set(indexToReplace, item);
                        onStatusChange(indexToReplace);
                    }
                }
            }
        }
        else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS)){
            log("listItemUpdate: Change participants");
            MegaChatRoom chatToCheck = megaChatApi.getChatRoom(item.getChatId());
            updateCacheForNonContacts(chatToCheck);
        }
        else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_VISIBILITY)){
            log("listItemUpdate: Change visibility");
        }
        else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)){
            log("listItemUpdate: Change unread count: "+item.getTitle());
            if(item!=null){

                if (adapterList == null || adapterList.getItemCount()==0){
                    setChats();
                }
                else{
                    long chatHandleToUpdate = item.getChatId();
                    int indexToReplace = -1;
                    ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                    while (itrReplace.hasNext()) {
                        MegaChatListItem chat = itrReplace.next();
                        if(chat!=null){
                            if(chat.getChatId()==chatHandleToUpdate){
                                indexToReplace = itrReplace.nextIndex()-1;
                                break;
                            }
                        }
                        else{
                            break;
                        }
                    }
                    if(indexToReplace!=-1){
                        log("Index to replace: "+indexToReplace);
                        log("Unread count: "+item.getUnreadCount());

                        chats.set(indexToReplace, item);
                        if(item.getUnreadCount()==0){
                            onUnreadCountChange(indexToReplace, false);
                        }
                        else{
                            onUnreadCountChange(indexToReplace, true);
                        }

                    }
                }
            }
        }
        else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_LAST_TS)){
            log("Change last ts: "+item.getChanges());

            long chatHandleToUpdate = item.getChatId();
            int indexToReplace = -1;
            ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
            while (itrReplace.hasNext()) {
                MegaChatListItem chat = itrReplace.next();
                if(chat!=null){
                    if(chat.getChatId()==chatHandleToUpdate){
                        indexToReplace = itrReplace.nextIndex()-1;
                        break;
                    }
                }
                else{
                    break;
                }
            }
            if(indexToReplace!=-1){
                log("Index to replace: "+indexToReplace);
                chats.set(indexToReplace, item);
                if(indexToReplace==0){
                    onLastTsChange(indexToReplace, false);
                }
                else{
                    onLastTsChange(indexToReplace, true);
                }
            }

        }
        else if((item.hasChanged(MegaChatListItem.CHANGE_TYPE_TITLE))){
            log("listItemUpdate: Change title: "+item.getTitle());

            if(item!=null){

                if (adapterList == null || adapterList.getItemCount()==0){
                    setChats();
                }
                else{
                    long chatHandleToUpdate = item.getChatId();
                    int indexToReplace = -1;
                    ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                    while (itrReplace.hasNext()) {
                        MegaChatListItem chat = itrReplace.next();
                        if(chat!=null){
                            if(chat.getChatId()==chatHandleToUpdate){
                                indexToReplace = itrReplace.nextIndex()-1;
                                break;
                            }
                        }
                        else{
                            break;
                        }
                    }
                    if(indexToReplace!=-1){
                        log("Index to replace: "+indexToReplace);
                        log("New title: "+item.getTitle());

                        chats.set(indexToReplace, item);
                        onTitleChange(indexToReplace);
                    }
                }
            }
        }
        else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_LAST_MSG)){
            log("listItemUpdate: Change last message: "+item.getChanges());

            if(item!=null){

                if (adapterList == null || adapterList.getItemCount()==0){
                    setChats();
                }
                else{
                    long chatHandleToUpdate = item.getChatId();
                    int indexToReplace = -1;
                    ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                    while (itrReplace.hasNext()) {
                        MegaChatListItem chat = itrReplace.next();
                        if(chat!=null){
                            if(chat.getChatId()==chatHandleToUpdate){
                                indexToReplace = itrReplace.nextIndex()-1;
                                break;
                            }
                        }
                        else{
                            break;
                        }
                    }
                    if(indexToReplace!=-1){
                        log("Index to replace: "+indexToReplace);
                        log("New title: "+item.getTitle());

                        chats.set(indexToReplace, item);
                        onLastMessageChange(indexToReplace);
                    }
                }
            }


        }
        else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_CLOSED)){
            log("listItemUpdate: Change closed: MegaChatListItem.CHANGE_TYPE_CLOSED");

            //Keep in the list of chats by now - then remove
//            if(item!=null){
//
//                if (adapterList.getItemCount()!=0){
//
//                    long chatHandleToRemove = item.getChatId();
//                    int indexToRemove = -1;
//                    ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
//                    while (itrReplace.hasNext()) {
//                        MegaChatListItem chat = itrReplace.next();
//                        if(chat!=null){
//                            if(chat.getChatId()==chatHandleToRemove){
//                                indexToRemove = itrReplace.nextIndex()-1;
//                                break;
//                            }
//                        }
//                        else{
//                            break;
//                        }
//                    }
//                    if(indexToRemove!=-1){
//                        log("Index to replace: "+indexToRemove);
//                        chats.remove(indexToRemove);
//
//                        adapterList.removeChat(chats, indexToRemove);
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
        }
        else{
            log("listItemUpdate: Other change: "+item.getChanges());
            if(item!=null){
                log("New chat: "+item.getTitle());
                setChats();
                MegaChatRoom chatToCheck = megaChatApi.getChatRoom(item.getChatId());
                updateCacheForNonContacts(chatToCheck);
            }
            else{
                log("The chat is NULL");
            }
        }
    }

    public void setStatus() {
        log("setStatus");
        if(chatEnabled) {
            chatStatus = megaChatApi.getOnlineStatus();
            log("chatStatus: " + chatStatus);

            onlineStatusUpdate(chatStatus);
        }
    }

    public void onlineStatusUpdate(int status) {
        log("onlineStatusUpdate: "+status);

        chatStatus = status;

        if (isAdded()) {
            if(aB!=null){

                switch(status){
                    case MegaChatApi.STATUS_ONLINE:{
                        aB.setSubtitle(getString(R.string.online_status));
                        break;
                    }
                    case MegaChatApi.STATUS_AWAY:{
                        aB.setSubtitle(getString(R.string.away_status));
                        break;
                    }
                    case MegaChatApi.STATUS_BUSY:{
                        aB.setSubtitle(getString(R.string.busy_status));
                        break;
                    }
                    case MegaChatApi.STATUS_OFFLINE:{
                        aB.setSubtitle(getString(R.string.offline_status));
                        break;
                    }
                }
            }
        }
    }

    public void onStatusChange(int position){
        log("onStatusChange: "+position);

        adapterList.setStatus(position, null);
    }

    public void onTitleChange(int position){
        log("onTitleChange");

        adapterList.setTitle(position, null);

        interactionUpdate(position);
    }

    public void onUnreadCountChange(int position, boolean updateOrder){
        log("onUnreadCountChange");

        adapterList.setPendingMessages(position, null);

        if(updateOrder){
            interactionUpdate(position);
        }
    }

    public void onLastTsChange(int position, boolean updateOrder){
        log("onLastTsChange");

        adapterList.setTs(position, null);

        if(updateOrder){
            interactionUpdate(position);
        }
    }

    public void onLastMessageChange(int position){
        log("onLastMessageChange");

        adapterList.setLastMessage(position, null);

//        if(updateOrder){
//            interactionUpdate(position);
//        }
    }

    public void showMuteIcon(MegaChatListItem item){
        log("showMuteIcon");

        long chatHandleToUpdate = item.getChatId();
        int indexToReplace = -1;
        ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
        while (itrReplace.hasNext()) {
            MegaChatListItem chat = itrReplace.next();
            if(chat!=null){
                if(chat.getChatId()==chatHandleToUpdate){
                    indexToReplace = itrReplace.nextIndex()-1;
                    break;
                }
            }
            else{
                break;
            }
        }
        if(indexToReplace!=-1){
            log("Index to replace: "+indexToReplace);
            if(adapterList!=null){
                adapterList.showMuteIcon(indexToReplace);
            }
        }
    }

    public void interactionUpdate(int position){
        log("interactionUpdate");
        MegaChatListItem chat = chats.remove(position);
        chats.add(0, chat);
        adapterList.notifyItemMoved(position, 0);
        if(lastFirstVisiblePosition==position){
            log("Interaction - change lastFirstVisiblePosition");
            lastFirstVisiblePosition=0;
        }
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

    public void updateStatus (int status){
        if(aB!=null){

            switch(status){
                case MegaChatApi.STATUS_ONLINE:{
                    aB.setSubtitle(getString(R.string.online_status));
                    break;
                }
                case MegaChatApi.STATUS_AWAY:{
                    aB.setSubtitle(getString(R.string.away_status));
                    break;
                }
                case MegaChatApi.STATUS_BUSY:{
                    aB.setSubtitle(getString(R.string.busy_status));
                    break;
                }
                case MegaChatApi.STATUS_OFFLINE:{
                    aB.setSubtitle(getString(R.string.offline_status));
                    break;
                }
            }

        }
    }

    public void updateCacheForNonContacts(MegaChatRoom chatToCheck){
        if(chatToCheck!=null){
            long peers = chatToCheck.getPeerCount();
            for(int i=0; i<peers; i++){
//                    long peerHandle = chatToCheck.getPeerHandle(i);
                String fullName = getParticipantFullName(chatToCheck, i);
                if(fullName!=null){
                    if(fullName.trim().length()<=0){
                        log("Ask for name!");
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context);
                        megaChatApi.getUserFirstname(chatToCheck.getPeerHandle(i), listener);
                        megaChatApi.getUserLastname(chatToCheck.getPeerHandle(i), listener);
                    }
                    else{
                        log("Exists name!");
                    }
                }
                else{
                    log("Ask for name!");
                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context);
                    megaChatApi.getUserFirstname(chatToCheck.getPeerHandle(i), listener);
                    megaChatApi.getUserLastname(chatToCheck.getPeerHandle(i), listener);
                }
            }
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
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, listView.getLayoutManager().onSaveInstanceState());
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        log("onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
            listView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
        }
    }

    private static void log(String log) {
        Util.log("RecentChatsFragmentLollipop", log);
    }
}
