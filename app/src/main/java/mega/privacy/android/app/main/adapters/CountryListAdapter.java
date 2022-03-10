package mega.privacy.android.app.main.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.CountryCodePickerActivity;

public class CountryListAdapter extends RecyclerView.Adapter<CountryListAdapter.CountryHolder> {
    
    public interface CountrySelectedCallback{
        void onCountrySelected(CountryCodePickerActivity.Country country);
    }

    private List<CountryCodePickerActivity.Country> countries;
    private CountrySelectedCallback callback;

    public CountryListAdapter(List<CountryCodePickerActivity.Country> countries) {
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
                CountryCodePickerActivity.Country country = countries.get(holder.getAdapterPosition());
                if(callback != null){
                    callback.onCountrySelected(country);
                }
            }
        });
        v.setTag(holder);
        return holder;
    }

    public CountryCodePickerActivity.Country getItem(int position) {
        return countries.get(position);
    }

    @Override
    public void onBindViewHolder(@NonNull CountryListAdapter.CountryHolder holder,int position) {
        CountryCodePickerActivity.Country country = getItem(position);
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

    public void refresh(List<CountryCodePickerActivity.Country> countries){
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
