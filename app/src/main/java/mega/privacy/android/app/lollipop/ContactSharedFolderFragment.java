package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;


public class ContactSharedFolderFragment extends ContactFileBaseFragment {
    
    RecyclerView listView;
    Button moreButton;
    MegaBrowserLollipopAdapter adapter;
    Stack<Long> parentHandleStack = new Stack<Long>();
    final int MAX_SHARED_FOLDER_NUMBER_TO_BE_DISPLAYED = 5;
    private ActionMode actionMode;
    
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
        if (actionMode != null) {
            actionMode.finish();
        }
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
                updateActionModeTitle();
            }
        } else {
            Intent i = new Intent(getContext(),ContactFileListActivityLollipop.class);
            i.putExtra("name",userEmail);
            i.putExtra("node_position",position);
            getContext().startActivity(i);
        }
    }
    
    public void selectAll(){
        if (adapter != null){
            if(adapter.isMultipleSelect()){
                adapter.selectAll();
            }
            else{
                adapter.setMultipleSelect(true);
                adapter.selectAll();
                
                actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
            }
            
            updateActionModeTitle();
        }
    }
    
    private void updateActionModeTitle() {
        if (actionMode == null) {
            return;
        }
        List<MegaNode> documents = adapter.getSelectedNodes();
        int files = 0;
        int folders = 0;
        for (MegaNode document : documents) {
            if (document.isFile()) {
                files++;
            } else if (document.isFolder()) {
                folders++;
            }
        }
        Resources res = getResources();
        String title;
        int sum=files+folders;
        
        if (files == 0 && folders == 0) {
            title = Integer.toString(sum);
        } else if (files == 0) {
            title = Integer.toString(folders);
        } else if (folders == 0) {
            title = Integer.toString(files);
        } else {
            title = Integer.toString(sum);
        }
        actionMode.setTitle(title);
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            e.printStackTrace();
            log("oninvalidate error");
        }
        // actionMode.
    }
    
    private class ActionBarCallBack implements ActionMode.Callback {
        
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            List<MegaNode> documents = adapter.getSelectedNodes();
            
            switch (item.getItemId()) {
                case R.id.cab_menu_download: {
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i = 0; i < documents.size(); i++) {
                        handleList.add(documents.get(i).getHandle());
                    }
                    
                    ((ContactInfoActivityLollipop)context).onFileClick(handleList);
                    break;
                }
                case R.id.cab_menu_copy: {
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i = 0; i < documents.size(); i++) {
                        handleList.add(documents.get(i).getHandle());
                    }
                    
                    ((ContactInfoActivityLollipop)context).showCopyLollipop(handleList);
                    break;
                }
                case R.id.cab_menu_select_all:{
                    selectAll();
                    break;
                }
                case R.id.cab_menu_unselect_all:{
                    clearSelections();
                    break;
                }
                case R.id.cab_menu_leave_multiple_share: {
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i=0;i<documents.size();i++){
                        handleList.add(documents.get(i).getHandle());
                    }
                    
                    ((ContactInfoActivityLollipop) context).showConfirmationLeaveIncomingShare(handleList);
                    break;
                }
                case R.id.cab_menu_trash: {
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i=0;i<documents.size();i++){
                        handleList.add(documents.get(i).getHandle());
                    }
                    ((ContactInfoActivityLollipop)(context)).askConfirmationMoveToRubbish(handleList);
                    break;
                }
                case R.id.cab_menu_rename: {
                    MegaNode aux = documents.get(0);
                    ((ContactInfoActivityLollipop) context).showRenameDialog(aux, aux.getName());
                    break;
                }
            }
            actionMode.finish();
            return true;
        }
        
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_browser_action, menu);
            return true;
        }
        
        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            log("onDestroyActionMode");
            clearSelections();
            adapter.setMultipleSelect(false);
        }
        
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<MegaNode> selected = adapter.getSelectedNodes();
            boolean showRename = false;
            boolean showMove = false;
            boolean showTrash = false;
            
            // Rename
            if(selected.size() == 1){
                if ((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK) || (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK)) {
                    showRename = true;
                }
            }
            
            if (selected.size() > 0) {
                if ((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK) || (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK)) {
                    showMove = true;
                }
            }
            
            if (selected.size() != 0) {
                showMove = false;
                // Rename
                if(selected.size() == 1) {
                    
                    if((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
                        showMove = true;
                        showRename = true;
                    }
                    else if(megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK){
                        showMove = false;
                        showRename = false;
                    }
                }
                else{
                    showRename = false;
                    showMove = false;
                }
                
                for(int i=0; i<selected.size();i++)	{
                    if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
                        showMove = false;
                        break;
                    }
                }
                
                if(!((ContactInfoActivityLollipop)context).isEmptyParentHandleStack()){
                    showTrash = true;
                }
                for(int i=0; i<selected.size(); i++){
                    if((megaApi.checkAccess(selected.get(i), MegaShare.ACCESS_FULL).getErrorCode() != MegaError.API_OK)){
                        showTrash = false;
                        break;
                    }
                }
                
                if(selected.size()==adapter.getItemCount()){
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                    menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);
                }
                else{
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);
                }
            }
            else{
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }
            
            menu.findItem(R.id.cab_menu_download).setVisible(true);
            menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            
            menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(true);
            menu.findItem(R.id.cab_menu_leave_multiple_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            
            menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
            menu.findItem(R.id.cab_menu_copy).setVisible(true);
            
            menu.findItem(R.id.cab_menu_move).setVisible(showMove);
            menu.findItem(R.id.cab_menu_share_link).setVisible(false);
            menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
            
            return false;
        }
    }
    
    public void activateActionMode(){
        log("activateActionMode");
        if (!adapter.isMultipleSelect()){
            adapter.setMultipleSelect(true);
            actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
        }
    }
    
    public boolean isEmptyParentHandleStack() {
        return parentHandleStack.isEmpty();
    }
}

