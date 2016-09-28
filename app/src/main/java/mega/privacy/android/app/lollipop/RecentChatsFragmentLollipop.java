package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.lollipop.tempMegaChatClasses.ChatRoom;
import mega.privacy.android.app.lollipop.tempMegaChatClasses.RecentChat;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatRoomList;

public class RecentChatsFragmentLollipop extends Fragment implements RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, View.OnClickListener {

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    Context context;
    ActionBar aB;
    RecyclerView listView;
    MegaListChatLollipopAdapter adapterList;
    GestureDetectorCompat detector;
    TextView emptyTextView;
    RecyclerView.LayoutManager mLayoutManager;

    ArrayList<ChatRoom> chats;
    RecentChat recentChat;

    RelativeLayout emptyLayout;
    LinearLayout buttonsEmptyLayout;
    Button inviteButton;
    Button getStartedButton;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    private ActionMode actionMode;

    private class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        public void onLongPress(MotionEvent e) {
            log("onLongPress");
            View view = listView.findChildViewUnder(e.getX(), e.getY());
            int position = listView.getChildLayoutPosition(view);

            // handle long press
            if (!adapterList.isMultipleSelect()){
                adapterList.setMultipleSelect(true);

                actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());

                itemClick(position);
            }

            super.onLongPress(e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        recentChat = new RecentChat();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.chat_recent_tab, container, false);

        listView = (RecyclerView) v.findViewById(R.id.chat_recent_list_view);
        listView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
        listView.setClipToPadding(false);;

        listView.addItemDecoration(new SimpleDividerItemDecoration(context));
        mLayoutManager = new MegaLinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        //Just onClick implemented
        listView.addOnItemTouchListener(this);
        listView.setItemAnimator(new DefaultItemAnimator());

        emptyLayout = (RelativeLayout) v.findViewById(R.id.empty_layout_chat_recent);
        emptyTextView = (TextView) v.findViewById(R.id.empty_text_chat_recent);

        RelativeLayout.LayoutParams emptyTextViewParams = (RelativeLayout.LayoutParams)emptyTextView.getLayoutParams();
        emptyTextViewParams.setMargins(Util.scaleWidthPx(39, outMetrics), Util.scaleHeightPx(95, outMetrics), Util.scaleWidthPx(39, outMetrics), 0);
        emptyTextView.setLayoutParams(emptyTextViewParams);

        buttonsEmptyLayout = (LinearLayout) v.findViewById(R.id.empty_buttons_layout_recent_chat);
        RelativeLayout.LayoutParams buttonsEmptyLayoutParams = (RelativeLayout.LayoutParams)buttonsEmptyLayout.getLayoutParams();
        buttonsEmptyLayoutParams.setMargins(Util.scaleWidthPx(39, outMetrics), Util.scaleHeightPx(49, outMetrics), 0, 0);
        buttonsEmptyLayout.setLayoutParams(buttonsEmptyLayoutParams);

        inviteButton = (Button) v.findViewById(R.id.invite_button);
        LinearLayout.LayoutParams inviteButtonParams = (LinearLayout.LayoutParams)inviteButton.getLayoutParams();
        inviteButtonParams.setMargins(0, Util.scaleHeightPx(4, outMetrics), 0, Util.scaleHeightPx(4, outMetrics));
        inviteButton.setLayoutParams(inviteButtonParams);
        inviteButton.setOnClickListener(this);

        getStartedButton = (Button) v.findViewById(R.id.get_started_button);
        LinearLayout.LayoutParams getStartedButtonParams = (LinearLayout.LayoutParams)getStartedButton.getLayoutParams();
        getStartedButtonParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(4, outMetrics), 0, Util.scaleHeightPx(4, outMetrics));
        getStartedButton.setLayoutParams(getStartedButtonParams);
        getStartedButton.setOnClickListener(this);

        chats = recentChat.getRecentChats();

        MegaChatRoomList chatList = megaChatApi.getChatRooms();
        if(chatList!=null){
            log("Chat size: :"+chatList.size());
        }
        else{
            log("Chat NULL");
        }

        for(int i=0;i<chatList.size();i++)
        {
            log("Chat size: :"+chatList.size());

            MegaChatRoom chatRoom = chatList.get(i);
            log("ChatRoom title: "+chatRoom.getTitle());
            log("ChatRoom handle: "+chatRoom.getChatId());
//            MegaChatListItem chatListItem = chatList.get(i);
//            megaChatApi.get
//            megaChatApi.getMessages(chatRoom.getChatId(), 50);
//            log("Chat: "+chatRoom.getMessages);
        }

        if (adapterList == null){
            adapterList = new MegaListChatLollipopAdapter(context, this, chats, listView, MegaListChatLollipopAdapter.ADAPTER_RECENT_CHATS);
        }
        else{
            adapterList.setChats(chats);
        }

        adapterList.setPositionClicked(-1);
        listView.setAdapter(adapterList);

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

        return v;
    }

    public static RecentChatsFragmentLollipop newInstance() {
        log("newInstance");
        RecentChatsFragmentLollipop fragment = new RecentChatsFragmentLollipop();
        return fragment;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        detector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Override
    public void onClick(View v) {
        log("onClick");
        switch (v.getId()) {
            case R.id.invite_button:{
                Toast.makeText(context, "INVITE!!!",Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.get_started_button:{
                Toast.makeText(context, "Get Started!!",Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    /////Multiselect/////
    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            List<ChatRoom> chats = adapterList.getSelectedChats();

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
                    //Mute
                    Toast.makeText(context, "Mute: "+chats.size()+" chats",Toast.LENGTH_SHORT).show();
                    break;
                }
                case R.id.cab_menu_archive:{
                    clearSelections();
                    hideMultipleSelect();
                    //Archive
                    Toast.makeText(context, "Archive: "+chats.size()+" chats",Toast.LENGTH_SHORT).show();
                    break;
                }
                case R.id.cab_menu_delete:{
                    clearSelections();
                    hideMultipleSelect();
                    //Delete
                    Toast.makeText(context, "Delete: "+chats.size()+" chats",Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.recent_chat_action, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            adapterList.setMultipleSelect(false);
            clearSelections();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<ChatRoom> selected = adapterList.getSelectedChats();

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
        updateActionModeTitle();
    }

    private void updateActionModeTitle() {
        if (actionMode == null || getActivity() == null) {
            return;
        }
        List<ChatRoom> chats = adapterList.getSelectedChats();

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
    void hideMultipleSelect() {
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
        if (adapterList.isMultipleSelect()){
            adapterList.toggleSelection(position);
            List<ChatRoom> chats = adapterList.getSelectedChats();
            if (chats.size() > 0){
                updateActionModeTitle();
//                adapterList.notifyDataSetChanged();
            }
            else{
                hideMultipleSelect();
            }
        }
        else{
            log("open chat one to one");
            Intent intent = new Intent(context, ChatActivityLollipop.class);
            intent.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
            String myMail = ((ManagerActivityLollipop) context).getMyAccountInfo().getMyUser().getEmail();
            intent.putExtra("CHAT_ID", position);
            intent.putExtra("MY_MAIL", myMail);
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

    public void resetAdapter(){
        log("resetAdapter");
        if(adapterList!=null){
            adapterList.setPositionClicked(-1);
        }
    }

    private static void log(String log) {
        Util.log("RecentChatsFragmentLollipop", log);
    }
}
