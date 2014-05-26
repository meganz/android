package com.mega.android;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaTransferListenerInterface;
import com.mega.sdk.TransferList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class TransfersFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	MegaTransfersAdapter adapter;
	
	MegaApiAndroid megaApi;
	
//	SparseArray<TransfersHolder> transfersListArray = null;
	
	TransferList tL = null;
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
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
		Toast.makeText(context, "tL.size()=" + tL.size(), Toast.LENGTH_LONG).show();
		
		listView = (ListView) v.findViewById(R.id.transfers_list_view);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
//		adapter = new MegaTransfersAdapter(context, transfersListArray, aB);
		adapter = new MegaTransfersAdapter(context, tL, aB);
		adapter.setPositionClicked(-1);
		listView.setAdapter(adapter);
		
		return v;
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
		
//		Intent i = new Intent(context, FullScreenImageViewer.class);
//		i.putExtra("position", position);
//		i.putExtra("imageIds", imageIds);
//		i.putExtra("names", namesArray);
//		startActivity(i);
    }
	
	public int onBackPressed(){
		
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
	
	public void setTransfers(TransferList _transfers){
		this.tL = _transfers;
		if (adapter != null){
			adapter.setTransfers(tL);
		}
	}

	private static void log(String log) {
		Util.log("TransfersFragment", log);
	}
}
