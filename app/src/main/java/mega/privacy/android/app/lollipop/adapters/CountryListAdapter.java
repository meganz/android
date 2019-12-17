package mega.privacy.android.app.lollipop.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop;

public class CountryListAdapter extends RecyclerView.Adapter<CountryListAdapter.CountryHolder> {
    
    public interface CountrySelectedCallback{
        void onCountrySelected(CountryCodePickerActivityLollipop.Country country);
    }

    private List<CountryCodePickerActivityLollipop.Country> countries;
    private CountrySelectedCallback callback;

    public CountryListAdapter(List<CountryCodePickerActivityLollipop.Country> countries) {
        this.countries = countries;
    }
    
    public void setCallback(CountrySelectedCallback callback){
        this.callback = callback;
    }

    @NonNull
    @Override
    public CountryListAdapter.CountryHolder onCreateViewHolder(@NonNull ViewGroup parent,int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_country_list,parent,false);
        final CountryHolder holder = new CountryHolder(v);
        holder.nameAndCode = v.findViewById(R.id.country_list_item_name_and_code);
        holder.nameAndCode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CountryCodePickerActivityLollipop.Country country = countries.get(holder.getAdapterPosition());
                if(callback != null){
                    callback.onCountrySelected(country);
                }
            }
        });
        v.setTag(holder);
        return holder;
    }

    public CountryCodePickerActivityLollipop.Country getItem(int position) {
        return countries.get(position);
    }

    @Override
    public void onBindViewHolder(@NonNull CountryListAdapter.CountryHolder holder,int position) {
        CountryCodePickerActivityLollipop.Country country = getItem(position);
        String name = country.getName();
        String code =country.getCode();
        String nameAndCode = name + " (" + code +  ")";
        holder.nameAndCode.setTag(code);
        holder.nameAndCode.setText(nameAndCode);
    }

    @Override
    public int getItemCount() {
        return countries.size();
    }

    public void refresh(List<CountryCodePickerActivityLollipop.Country> countries){
        this.countries = countries;
        notifyDataSetChanged();
    }

    public static class CountryHolder extends RecyclerView.ViewHolder {

        public TextView nameAndCode;

        public CountryHolder(View itemView) {
            super(itemView);
        }
    }
}
