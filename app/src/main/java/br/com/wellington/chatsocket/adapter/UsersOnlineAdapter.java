package br.com.wellington.chatsocket.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.wellington.chatsocket.R;

public class UsersOnlineAdapter extends BaseAdapter implements Filterable {


    private List<String> usersOnline;
    private List<String> usersOnlineDisplayed;
    private Activity context;

    public UsersOnlineAdapter(Activity context, HashMap<Integer, String> usersOnline) {
        this.context = context;
        this.usersOnline = new ArrayList<>(usersOnline.values());
        this.usersOnlineDisplayed = new ArrayList<>(usersOnline.values());
    }

    @Override
    public int getCount() {
        if (usersOnlineDisplayed != null) {
            return usersOnlineDisplayed.size();
        } else if (usersOnline != null) {
             return usersOnline.size();
        } else {
            return 0;
        }
    }

    @Override
    public String getItem(int position) {
        if (usersOnlineDisplayed != null) {
            return usersOnlineDisplayed.get(position);
        } else if (usersOnline != null){
            return usersOnline.get(position);
        } else {
            return null;
        }
    }

    public void removeItem(String item) {
        usersOnlineDisplayed.remove(item);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = vi.inflate(R.layout.users_online_item, null);
        holder = createViewHolder(convertView);
        convertView.setTag(holder);

        holder.userName.setText(getItem(position));

        return convertView;
    }

    public void add(String userOnline) {
        usersOnlineDisplayed.add(userOnline);
        usersOnline.add(userOnline);
        notifyDataSetChanged();
    }

    public void add(List<String> newUsersOnline) {
        usersOnlineDisplayed.addAll(newUsersOnline);
        usersOnline.addAll(newUsersOnline);
        notifyDataSetChanged();
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.userName = (TextView) v;
        return holder;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                List<String> FilteredArrList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    results.count = usersOnline.size();
                    results.values = usersOnline;
                } else {
                    constraint = constraint.toString().toLowerCase();
                    for (String data : usersOnline) {
                        if (data.toLowerCase().contains(constraint.toString())) {
                            FilteredArrList.add(data);
                    }
                    }
                    results.count = FilteredArrList.size();
                    results.values = FilteredArrList;
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                usersOnlineDisplayed = (List<String>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public void clear() {
        usersOnlineDisplayed.clear();
        usersOnline.clear();
        notifyDataSetChanged();
    }


    private static class ViewHolder {
        public TextView userName;
    }
}
