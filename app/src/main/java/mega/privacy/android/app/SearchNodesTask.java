package mega.privacy.android.app;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

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
    private Context context;
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

    public SearchNodesTask(Context mContext, Fragment mFragment, String mQuery, long mParentHandleSearch, ArrayList<MegaNode> mNodes){

        context = mContext;

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

        getOrder();

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
            cDExplorerF.setProgressView(false);
            cDExplorerF.setSearchNodes(nodes);
        } else if (isISharesExplorerF()) {
            iSharesExplorerF.setProgressView(false);
            iSharesExplorerF.setSearchNodes(nodes);
        }
    }

    public void cancelSearch() {
        if (megaCancelToken != null && !megaCancelToken.isCancelled()) {
            megaCancelToken.cancel();
        }
    }

    private void getOrder() {
        MegaPreferences prefs = DatabaseHandler.getDbHandler(context).getPreferences();

        if (prefs != null && prefs.getPreferredSortCloud() !=  null) {
            orderCloud = Integer.parseInt(prefs.getPreferredSortCloud());
        } else {
            orderCloud = MegaApiJava.ORDER_DEFAULT_ASC;
        }

        if (prefs != null && prefs.getPreferredSortOthers() != null) {
            orderOthers = Integer.parseInt(prefs.getPreferredSortOthers());
        } else {
            orderOthers = MegaApiAndroid.ORDER_DEFAULT_ASC;
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
                        parent = megaApi.getNodeByHandle(managerA.getParentHandleBrowser());
                        break;
                    }
                    case SHARED_ITEMS: {
                        if (managerA.getTabItemShares() == 0) {
                            if (managerA.getParentHandleIncoming() == -1) {
                                nodes = filterInShares(query);
                                return;
                            }

                            parent = megaApi.getNodeByHandle(managerA.getParentHandleIncoming());
                        } else if (managerA.getTabItemShares() == 1) {
                            if (managerA.getParentHandleOutgoing() == -1) {
                                nodes = filterOutShares(query);
                                return;
                            }

                            parent = megaApi.getNodeByHandle(managerA.getParentHandleOutgoing());
                        }
                        break;
                    }
                    case SAVED_FOR_OFFLINE: {
                        break;
                    }
                    case RUBBISH_BIN: {
                        parentHandle = managerA.getParentHandleRubbish();
                        if (parentHandle == -1) {
                            parent = megaApi.getRubbishNode();
                        } else {
                            parent = megaApi.getNodeByHandle(managerA.getParentHandleRubbish());
                        }
                        break;
                    }
                    case INBOX: {
                        parentHandle = managerA.getParentHandleInbox();
                        if (parentHandle == -1) {
                            parent = megaApi.getInboxNode();
                        } else {
                            parent = megaApi.getNodeByHandle(parentHandle);
                        }
                        break;
                    }
                }
            } else if (isCDExplorerF()) {
                parent = megaApi.getNodeByHandle(fileExplorerA.getParentHandleCloud());
            } else if (isISharesExplorerF()) {
                if (fileExplorerA.getParentHandleIncoming() == -1) {
                    nodes = filterInShares(query);
                    return;
                }

                parent = megaApi.getNodeByHandle(fileExplorerA.getParentHandleIncoming());
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
        return isFileExplorerA() && cDExplorerF != null;
    }

    public static void setSearchProgressView(RelativeLayout contentLayout, ProgressBar searchProgressBar, RecyclerView recyclerView, boolean inProgress) {
        contentLayout.setEnabled(!inProgress);
        if (inProgress) {
            contentLayout.setAlpha(0.4f);
            searchProgressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            contentLayout.setAlpha(1);
            searchProgressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public static ArrayList<MegaNode> getSearchedNodes(ArrayList<String> handles) {
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        ArrayList<MegaNode> nodes = new ArrayList<>();

        for (String handle : handles) {
            MegaNode node = megaApi.getNodeByHandle(Long.parseLong(handle));
            if (node != null) {
                nodes.add(node);
            }
        }

        return nodes;
    }
}