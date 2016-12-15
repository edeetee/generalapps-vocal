package generalapps.vocal;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by edeetee on 12/09/2016.
 */
public abstract class FilteredFirebaseRecyclerAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements ChildEventListener {
    public List<DataSnapshot> items;
    Class<VH> mViewHolderClass;
    int mLayout;
    Query mRef;

    public static abstract class Filter{
        public abstract boolean shouldAdd(DataSnapshot data);
    }
    Filter mFilter;

    public FilteredFirebaseRecyclerAdapter(Class<VH> viewHolderClass, int layout, Query ref){
        mViewHolderClass = viewHolderClass;
        mLayout = layout;

        items = new ArrayList<>();
        mRef = ref;
        mRef.addChildEventListener(this);
    }

    public void setFilter(Filter filter){
        mFilter = filter;
        mRef.removeEventListener(this);
        int count = items.size();
        items = new ArrayList<>();
        notifyItemRangeRemoved(0, count);
        mRef.addChildEventListener(this);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(mLayout, parent, false);
        try {
            Constructor<VH> constructor = mViewHolderClass.getConstructor(View.class);
            return constructor.newInstance(view);
        } catch (Exception e) {
            throw new RuntimeException(mViewHolderClass.getName() + " class constructor failed", e);
        }
    }

    public int findKeyPosition(String key){
        int position = 0;
        for(DataSnapshot curItem : items){
            if(curItem.getKey().equals(key)){
                return position;
            }
            position++;
        }
        return -1;
    }

//    public int findOrderPosition(DataSnapshot snapshot){
//        int position = 0;
//        for(DataSnapshot curItem : items){
//            try{
//                if(mComparator == null ?
//                        snapshot.getKey().compareTo(curItem.getKey()) <= 0 :
//                        mComparator.compare(snapshot, curItem) <= 0){
//                    return position;
//                }
//            } catch(Exception e){
//                Log.e("FFRecyclerAdapter", "PriorityPositionFailure", e);
//            }
//
//            position++;
//        }
//        return items.size();
//    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        populateViewHolder(holder, items.get(position));
    }

    DataSnapshot getItem(int pos){
        return items.get(pos);
    }

    public abstract void populateViewHolder(VH holder, DataSnapshot item);

    @Override
    public int getItemCount() {
        return items.size();
    }

    void cleanup(){
        mRef.removeEventListener(this);
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String previousChildKey) {
        //TODO check if exists from previous filter to enable move animations etc when changing filter

        int index = previousChildKey != null ? findKeyPosition(previousChildKey) + 1 : 0;
        //if prevChildKey == null or not in list, add to end
        if(index == 0)
            index = items.size();

        if(mFilter == null || mFilter.shouldAdd(dataSnapshot)){
            items.add(index, dataSnapshot);
            notifyItemInserted(index);
        }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String previousChildKey) {
        int position = findKeyPosition(dataSnapshot.getKey());

        if(position != -1){
            if(mFilter == null || mFilter.shouldAdd(dataSnapshot)){
                items.set(position, dataSnapshot);
                notifyItemChanged(position);
            } else
                onChildRemoved(dataSnapshot);
        } else
            onChildAdded(dataSnapshot, previousChildKey);
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        int position = findKeyPosition(dataSnapshot.getKey());
        //only trigger remove is was actually added
        if(position != -1){
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String previousChildKey) {
        int oldPos = findKeyPosition(dataSnapshot.getKey());

        //only move if both new and old positions exist in list
        if(oldPos != -1){
            items.remove(oldPos);
            int newPos = findKeyPosition(previousChildKey) + 1;
            if(newPos != 0){
                items.add(newPos, dataSnapshot);
                notifyItemMoved(oldPos, newPos);
            } else{
                notifyItemRemoved(oldPos);
            }
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
}
