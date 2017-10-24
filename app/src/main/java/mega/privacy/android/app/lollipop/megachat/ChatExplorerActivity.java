package mega.privacy.android.app.lollipop.megachat;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatListItem;

public class ChatExplorerActivity extends PinActivityLollipop implements View.OnClickListener{

    Toolbar tB;
    ActionBar aB;
    FrameLayout fragmentContainer;
    ChatExplorerFragment chatExplorerFragment;
    FloatingActionButton fab;

    private long[] nodeHandles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        log("onCreate first");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat_explorer);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_chat_explorer);
        fab = (FloatingActionButton) findViewById(R.id.fab_chat_explorer);
        fab.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        //Set toolbar
        tB = (Toolbar) findViewById(R.id.toolbar_chat_explorer);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        if(aB!=null){
            aB.setTitle("Choose chat");
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        }
        else{
            log("aB is null");
        }

        showFabButton(false);

        Intent intent = getIntent();
        if(intent!=null){
            log("Intent received");
            nodeHandles = intent.getLongArrayExtra("NODE_HANDLES");
            if(nodeHandles!=null){
                log("Node handle is: "+nodeHandles[0]);
            }
        }

        if(chatExplorerFragment ==null){
            chatExplorerFragment = new ChatExplorerFragment().newInstance();
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container_chat_explorer, chatExplorerFragment, "chatExplorerFragment");
        ft.commitNow();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");

        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void chooseChats(ArrayList<MegaChatListItem> chatListItems){
        log("chooseChats");

        long[] longArray = new long[chatListItems.size()];
        for (int i=0; i<chatListItems.size(); i++){
            longArray[i] = chatListItems.get(i).getChatId();
        }

        Intent intent = new Intent();
        intent.putExtra("SELECTED_CHATS", longArray);
        intent.putExtra("NODE_HANDLES", nodeHandles);
        setResult(RESULT_OK, intent);
        log("finish!");
        finish();
    }

    public void showFabButton(boolean show){
        if(show){
            fab.setVisibility(View.VISIBLE);
        }
        else{
            fab.setVisibility(View.GONE);
        }
    }

    public static void log(String log) {
        Util.log("ChatExplorerActivity", log);
    }

    @Override
    public void onClick(View v) {
        log("onClick");

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        switch(v.getId()) {
            case R.id.fab_chat_explorer: {
                if(chatExplorerFragment!=null){
                    if(chatExplorerFragment.getSelectedChats()!=null){
                        chooseChats(chatExplorerFragment.getSelectedChats());
                    }
                }
                break;
            }
        }
    }
}
