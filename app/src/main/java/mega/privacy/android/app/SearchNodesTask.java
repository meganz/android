package mega.privacy.android.app;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.IncomingSharesExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaCancelToken;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.SortUtil.*;

public class SearchNodesTask extends AsyncTask<Void, Void, Void> {

    private ManagerActivityLollipop managerA;
    private FileExplorerActivityLollipop fileExplorerA;
    private IncomingSharesExplorerFragmentLollipop iSharesExplorerF;
    private CloudDriveExplorerFragmentLollipop cDExplorerF;
    private SearchFragmentLollipop searchF;

    private MegaApiAndroid megaApi;
    private MegaCancelToken megaCancelToken;

    private String query;
    private long parentHandleSearch = -1;
    private ArrayList<MegaNode> nodes;

    private int orderCloud;
    private int orderOthers;

    public SearchNodesTask(Context context, Fragment mFragment, String mQuery, long mParentHandleSearch, ArrayList<MegaNode> mNodes, int mOrderCloud, int mOrderOthers){

        if (context instanceof ManagerActivityLollipop) {
            managerA = (ManagerActivityLollipop) context;
        } else if (context instanceof FileExplorerActivityLollipop) {
            fileExplorerA = (FileExplorerActivityLollipop) context;
        }

        if (mFragment instanceof SearchFragmentLollipop) {
            searchF = (SearchFragmentLollipop) mFragment;
        } else if (mFragment instanceof IncomingSharesExplorerFragmentLollipop) {
            iSharesExplorerF = (IncomingSharesExplorerFragmentLollipop) mFragment;
        } else if (mFragment instanceof CloudDriveExplorerFragmentLollipop) {
            cDExplorerF = (CloudDriveExplorerFragmentLollipop) mFragment;
        }

        query = mQuery;
        parentHandleSearch = mParentHandleSearch;
        nodes = mNodes;

        orderCloud = mOrderCloud;
        orderOthers = mOrderOthers;

        megaApi = MegaApplication.getInstance().getMegaApi();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        getSearchNodes();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (isSearchF()) {
            searchF.setProgressView(false);
            searchF.setNodes(nodes);
        } else if (isCDExplorerF()) {

        } else if (isISharesExplorerF()) {

        }
    }

    public void cancelSearch() {
        if (megaCancelToken != null && !megaCancelToken.isCancelled()) {
            megaCancelToken.cancel();
        }
    }

    private void getSearchNodes() {
        if (query == null) {
            nodes.clear();
            return;
        }

        MegaNode parent = null;
        long parentHandle;

        if (parentHandleSearch == -1) {
            if (isSearchF()) {
                final ManagerActivityLollipop.DrawerItem drawerItem = managerA.getSearchDrawerItem();
                if (drawerItem == null) return;

                switch (drawerItem) {
                    case CLOUD_DRIVE: {
                        parent = megaApi.getNodeByHandle(managerA.parentHandleBrowser);
                        break;
                    }
                    case SHARED_ITEMS: {
                        if (managerA.getTabItemShares() == 0) {
                            if (managerA.parentHandleIncoming == -1) {
                                nodes = filterInShares(query);
                                return;
                            } else {
                                parent = megaApi.getNodeByHandle(managerA.parentHandleIncoming);
                            }
                        } else if (managerA.getTabItemShares() == 1) {
                            if (managerA.parentHandleOutgoing == -1) {
                                nodes = filterOutShares(query);
                                return;
                            } else {
                                parent = megaApi.getNodeByHandle(managerA.parentHandleOutgoing);
                            }
                        }
                        break;
                    }
                    case SAVED_FOR_OFFLINE: {
                        break;
                    }
                    case RUBBISH_BIN: {
                        parentHandle = managerA.parentHandleRubbish;
                        if (parentHandle == -1) {
                            parent = megaApi.getRubbishNode();
                        } else {
                            parent = megaApi.getNodeByHandle(managerA.parentHandleRubbish);
                        }
                        break;
                    }
                    case INBOX: {
                        parentHandle = managerA.parentHandleInbox;
                        if (parentHandle == -1) {
                            parent = megaApi.getInboxNode();
                        } else {
                            parent = megaApi.getNodeByHandle(parentHandle);
                        }
                        break;
                    }
                }
            } else if (isCDExplorerF()) {

            } else if (isISharesExplorerF()) {

            }
        } else {
            parent = megaApi.getNodeByHandle(parentHandleSearch);
        }

        if (parent != null) {
            if (query.isEmpty() || parentHandleSearch != -1) {
                nodes = megaApi.getChildren(parent);
            } else {
                megaCancelToken = MegaCancelToken.createInstance();
                nodes = megaApi.search(parent, query, megaCancelToken, true, orderCloud);
            }
        }
    }

    private ArrayList<MegaNode> filterInShares(String query) {
        ArrayList<MegaNode> inShares = megaApi.getInShares();
        ArrayList<MegaNode> filteredInShares = new ArrayList<>();

        for (MegaNode inShare : inShares) {
            if (shouldNodeBeFilter(inShare, query)) {
                filteredInShares.add(inShare);
            }
        }

        if(orderOthers == MegaApiJava.ORDER_DEFAULT_DESC){
            sortByMailDescending(filteredInShares);
        }

        return filteredInShares;
    }

    private ArrayList<MegaNode> filterOutShares(String query) {
        ArrayList<MegaShare> outShares = megaApi.getOutShares();
        ArrayList<MegaNode> filteredOutShares = new ArrayList<>();

        for (MegaShare outShare : outShares) {
            MegaNode node = megaApi.getNodeByHandle(outShare.getNodeHandle());
            if (node == null) continue;

            if (shouldNodeBeFilter(node, query)) {
                filteredOutShares.add(node);
            }
        }

        if(orderOthers == MegaApiJava.ORDER_DEFAULT_DESC){
            sortByNameDescending(filteredOutShares);
        }
        else{
            sortByNameAscending(filteredOutShares);
        }

        return filteredOutShares;
    }

    private boolean shouldNodeBeFilter(MegaNode node, String query) {
        if (node.getName().toLowerCase().contains(query.toLowerCase())) {
            return true;
        }

        return false;
    }

    private boolean isSearchF() {
        return managerA != null && searchF != null;
    }

    private boolean isFileExplorerA() {
        return fileExplorerA != null;
    }

    private boolean isISharesExplorerF() {
        return isFileExplorerA() && iSharesExplorerF != null;
    }

    private boolean isCDExplorerF() {
        return isISharesExplorerF() && cDExplorerF != null;
    }
}