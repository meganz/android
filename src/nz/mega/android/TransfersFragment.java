package nz.mega.android;

import java.util.ArrayList;
import java.util.List;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaTransfer;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class TransfersFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	MegaTransfersAdapter adapter;
	
	MegaApiAndroid megaApi;
	
	ImageView emptyImage;
	TextView emptyText;
	ImageView pauseImage;
	TextView pauseText;
	
	boolean pause = false;
	
	TransfersFragment transfersFragment = this;
	
//	SparseArray<TransfersHolder> transfersListArray = null;
	
	ArrayList<MegaTransfer> tL = null;
	
	private ActionMode actionMode;
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaTransfer> transfers = getSelectedTransfers();
			
			switch(item.getItemId()){
				case R.id.cab_menu_trash:{
					for (int i=0;i<transfers.size();i++){
						MegaTransfer t = transfers.get(i);
						if (t.getType() == MegaTransfer.TYPE_DOWNLOAD){
							megaApi.cancelTransfer(t, (ManagerActivity)context);
						}
						else if (t.getType() == MegaTransfer.TYPE_UPLOAD){
							megaApi.cancelTransfer(t, adapter);
						}
					}
					clearSelections();
					hideMultipleSelect();
					break;
				}
				
				
				
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.transfers_action, menu);
			megaApi.pauseTransfers(true);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			adapter.setMultipleSelect(false);
			listView.setOnItemLongClickListener(transfersFragment);
			clearSelections();
			megaApi.pauseTransfers(pause);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaTransfer> selected = getSelectedTransfers();

			boolean showRemove = false;
			
			if (selected.size() > 0) {
				showRemove = true;
			}
			
			menu.findItem(R.id.cab_menu_trash).setVisible(showRemove);
			
			return false;
		}
	}
	
	private Handler handler;
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		handler = new Handler();
		
		super.onCreate(savedInstanceState);
		log("onCreate");		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {  
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		aB.setTitle(getString(R.string.section_transfers));

		((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
		((ManagerActivity)context).supportInvalidateOptionsMenu();
		
		View v = inflater.inflate(R.layout.fragment_transfers, container, false);
		
		
//		if (transfersListArray == null){
//			tL = megaApi.getTransfers();
//			transfersListArray = new SparseArray<TransfersHolder>();
//			
//			for (int i = 0; i< tL.size(); i++){
//				MegaTransfer t = tL.get(i);
//				TransfersHolder th = new TransfersHolder();
//				
//				th.setName(new String(t.getFileName()));
//				
//				transfersListArray.put(t.getTag(), th);
//			}
//		}
		tL = megaApi.getTransfers();
		
		listView = (ListView) v.findViewById(R.id.transfers_list_view);
		listView.setOnItemClickListener(null);
		listView.setOnItemLongClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
//		adapter = new MegaTransfersAdapter(context, transfersListArray, aB);
		adapter = new MegaTransfersAdapter(context, tL, aB);
		adapter.setPositionClicked(-1);
		
		adapter.setMultipleSelect(false);
		
		listView.setAdapter(adapter);
		
		emptyImage = (ImageView) v.findViewById(R.id.transfers_empty_image);
		pauseImage = (ImageView) v.findViewById(R.id.transfers_pause_image);
		emptyText = (TextView) v.findViewById(R.id.transfers_empty_text);
		pauseText = (TextView) v.findViewById(R.id.transfers_pause_text);
		
		emptyImage.setImageResource(R.drawable.ic_no_active_transfers);
		pauseImage.setImageResource(R.drawable.ic_pause_tranfers);
		emptyText.setText(getString(R.string.transfers_empty));
		pauseText.setText(getString(R.string.transfers_pause));
		
		pauseImage.setVisibility(View.GONE);
		pauseText.setVisibility(View.GONE);
		
		if (pause){
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.GONE);
			
			pauseImage.setVisibility(View.VISIBLE);
			pauseText.setVisibility(View.VISIBLE);
		}
		else{
			if (tL.size() == 0){
				emptyImage.setVisibility(View.VISIBLE);
				emptyText.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
				((ManagerActivity)context).setPauseIconVisible(false);
			}
			else{
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
				((ManagerActivity)context).setPauseIconVisible(true);
			}
			
			pauseImage.setVisibility(View.GONE);
			pauseText.setVisibility(View.GONE);
		}    
		 
//		refreshTransfers();
		
		return v;
	}
	
	private void refreshTransfers(){
		log("refreshTransfers()");
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				tL = megaApi.getTransfers();
				setTransfers(tL);
				if (tL.size() > 0){
					refreshTransfers();
				}
			}
		}, 1 * 1000);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){

		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
		
		if (adapter.isMultipleSelect()){
			SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true){
				listView.setItemChecked(position, true);
			}
			else{
				listView.setItemChecked(position, false);
			}				
			updateActionModeTitle();
			adapter.notifyDataSetChanged();
		}
		else{
			if (adapter != null){
				adapter.threeDotsClick(position);
			}	
		}		
    }
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapter.getPositionClicked() == -1){
			clearSelections();
			actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
			listView.setItemChecked(position, true);
			adapter.setMultipleSelect(true);
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
		}
		return true;
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				listView.setItemChecked(checkedPosition, false);
			}
		}
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaTransfer> transfers = getSelectedTransfers();
		int downloads = 0;
		int uploads = 0;
		for (MegaTransfer transfer : transfers) {
			if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD){
				downloads++;
			} else if (transfer.getType() == MegaTransfer.TYPE_UPLOAD) {
				uploads++;
			}
		}
		Resources res = getActivity().getResources();
		String format = "%d %s";
		String filesStr = String.format(format, downloads,
				res.getQuantityString(R.plurals.general_num_downloads, downloads));
		String foldersStr = String.format(format, uploads,
				res.getQuantityString(R.plurals.general_num_uploads, uploads));
		String title;
		if (downloads == 0 && uploads == 0) {
			title = "";
		} else if (downloads == 0) {
			title = foldersStr;
		} else if (uploads == 0) {
			title = filesStr;
		} else {
			title = foldersStr + ", " + filesStr;
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
	
	/*
	 * Get list of all selected documents
	 */
	private List<MegaTransfer> getSelectedTransfers() {
		ArrayList<MegaTransfer> transfers = new ArrayList<MegaTransfer>();
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				MegaTransfer transfer = adapter.getTransferAt(checkedItems.keyAt(i));
				if (transfer != null){
					transfers.add(transfer);
				}
			}
		}
		return transfers;
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		
		if (adapter == null){
			return 0;
		}
		
		if (adapter.isMultipleSelect()){
			hideMultipleSelect();
			return 2;
		}
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else{
			return 0;
		}
	}
	
//	public void setTransfers(SparseArray<TransfersHolder> tl){
//		transfersListArray = tl;
//		if (adapter != null){
//			adapter.setTransfers(transfersListArray);
//		}
//	}
	
	public void setTransfers(ArrayList<MegaTransfer> _transfers){
		this.tL = _transfers;
		
		if (adapter != null){
			if (tL == null){
				adapter.setPositionClicked(-1);
			}
			else{
				if (tL.size() == 0){
					adapter.setPositionClicked(-1);
				}
			}
			adapter.setTransfers(tL);
		}
		
		if (emptyImage != null){
			if (pause){
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
				
				pauseImage.setVisibility(View.VISIBLE);
				pauseText.setVisibility(View.VISIBLE);
			}
			else{
				if (tL.size() == 0){
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
					((ManagerActivity)context).setPauseIconVisible(false);
				}
				else{
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
					((ManagerActivity)context).setPauseIconVisible(true);
				}	
				
				pauseImage.setVisibility(View.GONE);
				pauseText.setVisibility(View.GONE);
			}
		}
	}
	
	public void setCurrentTransfer(MegaTransfer mT){
		if (adapter != null){
			adapter.setCurrentTransfer(mT);
		}
	}
	
	public void setPause(boolean pause){
		this.pause = pause;
		
		if (adapter != null){
			tL = megaApi.getTransfers();
			adapter.setTransfers(tL);
		}
		
		if (emptyImage != null){ //This means that the view has been already created
			if (pause){
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
				
				pauseImage.setVisibility(View.VISIBLE);
				pauseText.setVisibility(View.VISIBLE);
			}
			else{
				if (tL.size() == 0){
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
					((ManagerActivity)context).setPauseIconVisible(false);
				}
				else{
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
					((ManagerActivity)context).setPauseIconVisible(true);
				}
				
				pauseImage.setVisibility(View.GONE);
				pauseText.setVisibility(View.GONE);
			}
		}
	} 
	
	
	public void setNoActiveTransfers(){
		this.pause = false;
		if (emptyImage != null){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
			pauseImage.setVisibility(View.GONE);
			pauseText.setVisibility(View.GONE);
			((ManagerActivity)context).setPauseIconVisible(false);
		}
	}

	private static void log(String log) {
		Util.log("TransfersFragment", log);
	}
}
