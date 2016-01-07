package com.example.david.sharing;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by david on 03-01-2016.
 */
public class UserListAdapter extends BaseAdapter {
    private ArrayList<User> users;
    private LayoutInflater mInflator;
    Activity mother;

    public UserListAdapter(Context context, Activity mother, ArrayList<User> userList){
        users = userList;
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mother = mother;
        refreshUsers();
    }

    public void refreshUsers(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    mother.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                    SystemClock.sleep(1000);
                }
            }
        }).start();
    }

    public int getCount() {
        return users.size();
    }

    public Object getItem(int i) {
        return users.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView (int i, View view, ViewGroup viewGroup){

        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.user_item, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView) view.findViewById(R.id.user_session_item);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        User user = users.get(i);

        viewHolder.name.setText(user.mail);
        return view;
    }

    static class ViewHolder{
        TextView name;
    }
}
