package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class FileBrowserListFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	MegaBrowserListAdapter adapter;
	
	MegaApiAndroid megaApi;
		
	List<ItemFileBrowser> rowItems;
	
	ArrayList<Long> historyNodes = null;
	
	//Esto hay que quitarlo cuando haga el visor completo
	ArrayList<String> namesArray = new ArrayList<String>();
	ArrayList<Integer> imageIds = new ArrayList<Integer>();
	NodeList nodes;
	//HASTA AQUI 
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		rowItems = new ArrayList<ItemFileBrowser>();
		
		nodes = megaApi.getChildren(megaApi.getRootNode());
		for(int i=0; i<nodes.size(); i++){
			
			//Esto hay que quitarlo cuando haga el visor completo
			namesArray.add("NombrePrueba");
			imageIds.add(R.drawable.sal01);
			//HASTA AQUI
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		aB.setTitle(getString(R.string.section_cloud_drive));
		
		View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
		        
        listView = (ListView) v.findViewById(R.id.file_list_view_browser);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
		adapter = new MegaBrowserListAdapter(context, nodes);
		adapter.setPositionClicked(-1);
		if (historyNodes != null){
			adapter.setHistoryNodes(historyNodes);
			nodes = megaApi.getChildren(megaApi.getNodeByHandle(historyNodes.get(historyNodes.size()-1)));
			adapter.setNodes(nodes);
		}
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
		
		if (nodes.get(position).isFolder()){
			ArrayList<Long> historyNodes = adapter.getHistoryNodes();
			historyNodes.add(nodes.get(position).getHandle());
			adapter.setHistoryNodes(historyNodes);
			log("handle a meter: "+ nodes.get(position).getHandle());
			nodes = megaApi.getChildren(nodes.get(position));
			adapter.setNodes(nodes);
		}
		else{
			Toast.makeText(context, "[IS FILE]Node handle clicked: " + nodes.get(position).getHandle(), Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(context, FullScreenImageViewer.class);
			intent.putExtra("position", position);
			intent.putExtra("imageIds", imageIds);
			intent.putExtra("names", namesArray);
			if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
				intent.putExtra("parentNodeHandle", -1L);
			}
			else{
				intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
			}
			startActivity(intent);
		}
    }
	
	public int onBackPressed(){
		
		ArrayList<Long> historyNodes = adapter.getHistoryNodes();
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else if (historyNodes.size() > 1){
			long handle = historyNodes.get(historyNodes.size()-2);
			log("handle a retirar: " + handle);
			historyNodes.remove(historyNodes.size()-1);
			adapter.setHistoryNodes(historyNodes);
			nodes = megaApi.getChildren(megaApi.getNodeByHandle(handle));
			adapter.setNodes(nodes);
			return 2;
		}
		else{
			return 0;
		}
	}
	
	public ArrayList<Long> getHistoryNodes(){
		return adapter.getHistoryNodes();
	}
	
	public void setHistoryNodes(ArrayList<Long> historyNodes){
		this.historyNodes = historyNodes;
		if (adapter != null){
			adapter.setHistoryNodes(historyNodes);
		}	}
	
	private static void log(String log) {
		Util.log("FileBrowserListFragment", log);
	}

}
