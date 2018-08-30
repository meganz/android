package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaNode;


public class ContactSharedFolderFragment extends ContactFileBaseFragment {
    
    RecyclerView listView;
    Button moreButton;
    MegaBrowserLollipopAdapter adapter;
    Stack<Long> parentHandleStack = new Stack<Long>();
    final int MAX_SHARED_FOLDER_NUMBER_TO_BE_DISPLAYED = 5;
    
    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState) {
        log("ContactSharedFolderFragment: onCreateView");
        
        View v = null;
        if (userEmail != null) {
            v = inflater.inflate(R.layout.fragment_contact_shared_folder_list,container,false);
            contact = megaApi.getContact(userEmail);
            
            //only show up to 5 folders in this page
            ArrayList<MegaNode> fullList = megaApi.getInShares(contact);
            contactNodes = getNodeListToBeDisplayed(fullList);
            
            //set button text
            moreButton = (Button)v.findViewById(R.id.more_button);
            setupMoreButtonText(fullList.size());
            
            moreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getContext(),ContactFileListActivityLollipop.class);
                    i.putExtra("name",userEmail);
                    getContext().startActivity(i);
                }
            });
            
            //set up list view
            listView = (RecyclerView)v.findViewById(R.id.contact_shared_folder_list_view);
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
            listView.addItemDecoration(new SimpleDividerItemDecoration(context,outMetrics));
            listView.setLayoutManager(mLayoutManager);
            listView.setItemAnimator(new DefaultItemAnimator());
            
            if (adapter == null) {
                adapter = new MegaBrowserLollipopAdapter(context,this,contactNodes,-1,listView,aB,Constants.CONTACT_SHARED_FOLDER_ADAPTER,MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
                
            } else {
                adapter.setNodes(contactNodes);
                adapter.setParentHandle(-1);
            }
            
            adapter.setMultipleSelect(false);
            listView.setAdapter(adapter);
        }
        
        return v;
    }
    
    private ArrayList<MegaNode> getNodeListToBeDisplayed(ArrayList<MegaNode> fullList) {
        
        ArrayList newList = new ArrayList<>();
        if (fullList.size() > MAX_SHARED_FOLDER_NUMBER_TO_BE_DISPLAYED) {
            for (int i = 0;i < MAX_SHARED_FOLDER_NUMBER_TO_BE_DISPLAYED;i++) {
                newList.add(fullList.get(i));
            }
        } else {
            newList = fullList;
        }
        
        return newList;
    }
    
    public void showOptionsPanel(MegaNode sNode) {
        log("showOptionsPanel");
        ((ContactInfoActivityLollipop)context).showOptionsPanel(sNode);
    }
    
    public void clearSelections() {
        if (adapter != null && adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
    }
    
    public void setupMoreButtonText(int fullListLength) {
        
        int foldersInvisible = fullListLength - contactNodes.size();
        
        //hide button if no invisible folders
        if (foldersInvisible == 0) {
            moreButton.setVisibility(View.GONE);
            return;
        } else {
            moreButton.setVisibility(View.VISIBLE);
        }
        
        String label = foldersInvisible + " " + getResources().getString(R.string.contact_info_button_more).toUpperCase(Locale.getDefault());
        moreButton.setText(label);
        
    }
    
    public void hideMultipleSelect() {
        log("hideMultipleSelect");
        adapter.setMultipleSelect(false);
        //todo update action buttons
//        if (actionMode != null) {
//            actionMode.finish();
//        }
    }
    
    public void setNodes(long parentHandle) {
        if (megaApi.getNodeByHandle(parentHandle) == null) {
            parentHandle = -1;
            this.parentHandle = -1;
            ((ContactInfoActivityLollipop)context).setParentHandle(parentHandle);
            adapter.setParentHandle(parentHandle);
            
            ArrayList<MegaNode> fullList = megaApi.getInShares(contact);
            setNodes(getNodeListToBeDisplayed(fullList));
            setupMoreButtonText(fullList.size());
        }
    }
    
    public void setNodes(ArrayList<MegaNode> nodes) {
        this.contactNodes = nodes;
        if (adapter != null) {
            adapter.setNodes(contactNodes);
            //todo handle when no node available - collapse section and update button?
        }
    }
    
    public void setNodes() {
        contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
        adapter.setNodes(contactNodes);
        listView.invalidate();
    }
    
    public void itemClick(int position,int[] screenPosition,ImageView imageView) {
        ((MegaApplication)((Activity)context).getApplication()).sendSignalPresenceActivity();
        
        if (adapter.isMultipleSelect()) {
            log("multiselect ON");
            adapter.toggleSelection(position);
            
            List<MegaNode> selectedNodes = adapter.getSelectedNodes();
            if (selectedNodes.size() > 0) {
//                updateActionModeTitle();
            }
        } else {
            Intent i = new Intent(getContext(),ContactFileListActivityLollipop.class);
            i.putExtra("name",userEmail);
            i.putExtra("node_position",position);
            getContext().startActivity(i);
        }
    }
}
