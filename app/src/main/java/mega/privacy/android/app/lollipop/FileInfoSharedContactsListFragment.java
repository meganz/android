package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.MegaFileInfoSharedContactLollipopAdapter;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

public class FileInfoSharedContactsListFragment extends MegaFragment implements RecyclerView.OnItemTouchListener, View.OnClickListener {
    
    public static final String NODE_HANDLE = "NODE_HANDLE";
    final int MAX_NUMBER_OF_CONTACTS_IN_LIST = 5;
    private MegaShare selectedShare;
    private ActionMode actionMode;
    protected RecyclerView listView;
    protected LinearLayoutManager mLayoutManager;
    protected long nodeHandle;
    protected MegaNode node;
    protected ArrayList<MegaShare> listContacts;
    protected ArrayList<MegaShare> fullListContacts;
    protected Button moreButton;
    
    MegaFileInfoSharedContactLollipopAdapter adapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("FileInfoSharedContactsListFragment onCreate");
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState) {
        log("FileInfoSharedContactsListFragment onCreate");
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_file_info_contact_list,container,false);
        
        listView = (RecyclerView)view.findViewById(R.id.file_info_contact_list_view);
        listView.addOnItemTouchListener(this);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.addItemDecoration(new SimpleDividerItemDecoration(mContext,outMetrics));
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        listView.setLayoutManager(mLayoutManager);
        
        //get shared contact list and max number can be displayed in the list is five
        setContactList();
        
        moreButton = (Button)view.findViewById(R.id.more_button);
        moreButton.setOnClickListener(this);
        setMoreButtonText();
        
        //setup adapter
        if (adapter == null) {
            adapter = new MegaFileInfoSharedContactLollipopAdapter(mContext,node,listContacts,listView);
            listView.setAdapter(adapter);
        }
        adapter.setShareList(listContacts);
        adapter.setPositionClicked(-1);
        adapter.setMultipleSelect(false);
        
        listView.setAdapter(adapter);
        
        ((MegaApplication)((Activity)mContext).getApplication()).sendSignalPresenceActivity();
        return view;
    }
    
    public void refresh(){
        setContactList();
        setMoreButtonText();
        adapter.setShareList(listContacts);
        adapter.notifyDataSetChanged();
    }
    
    private void setContactList() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            long handle = bundle.getLong(NODE_HANDLE,-1);
            node = megaApi.getNodeByHandle(handle);
        }
        
        fullListContacts = new ArrayList<>();
        listContacts = new ArrayList<>();
        if (node != null) {
            fullListContacts = megaApi.getOutShares(node);
            
            if (fullListContacts.size() > MAX_NUMBER_OF_CONTACTS_IN_LIST) {
                listContacts = new ArrayList<>(fullListContacts.subList(0,MAX_NUMBER_OF_CONTACTS_IN_LIST));
            } else {
                listContacts = fullListContacts;
            }
        }
    }
    
    private void setMoreButtonText() {
        int fullSize = fullListContacts.size();
        if (fullSize > MAX_NUMBER_OF_CONTACTS_IN_LIST) {
            moreButton.setVisibility(View.VISIBLE);
            moreButton.setText((fullSize - MAX_NUMBER_OF_CONTACTS_IN_LIST) + " " + getResources().getString(R.string.label_more).toUpperCase());
        } else {
            moreButton.setVisibility(View.GONE);
        }
    }
    
    public boolean isMultipleSelect(){
        return false;
    }
    
    public MegaFileInfoSharedContactLollipopAdapter getAdapter(){
        return this.adapter;
    }
    
    public String getUser(int position){
        return listContacts.get(position).getUser();
    }
    
    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv,MotionEvent e) {
        return false;
    }
    
    @Override
    public void onTouchEvent(RecyclerView rv,MotionEvent e) {
    
    }
    
    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    
    }
    
    @Override
    public void onClick(View v) {
        Intent i = new Intent(mContext, FileContactListActivityLollipop.class);
        i.putExtra("name", node.getHandle());
        startActivity(i);
    }
}
