package generalapps.vocal;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by edeetee on 25/10/2016.
 */

public class ArtistSearcher extends LinearLayout {

    @BindView(R.id.searchRecycler) RecyclerView searchRecycler;
    @BindView(R.id.search) EditText searchBox;
    FilteredFirebaseRecyclerAdapter searchAdapter;

    public ArtistSearcher(Context context) {
        super(context);
        init(context);
    }

    public ArtistSearcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.artist_searcher, this);

        ButterKnife.bind(this);

        searchRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        searchRecycler.addItemDecoration(new SimpleDividerDecoration(getContext(), 0.03f));
    }

    void setOnUserSelectedListener(final OnUserSelectedListener listener){
        DatabaseReference usersRef = MainActivity.database.getReference("users");
        searchAdapter = new FilteredFirebaseRecyclerAdapter<SearchHolder>(SearchHolder.class, R.layout.artist_search_item, usersRef.orderByChild("name")){
            @Override
            public void populateViewHolder(SearchHolder holder, DataSnapshot item) {
                holder.bind(item.getValue(User.class), listener);
            }
        };
        searchRecycler.setAdapter(searchAdapter);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                searchAdapter.setFilter(new FilteredFirebaseRecyclerAdapter.Filter() {
                    @Override
                    public boolean shouldAdd(DataSnapshot data) {
                        return data.getValue(User.class).name.toLowerCase().startsWith(s.toString().toLowerCase());
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    void cleanup(){
        searchAdapter.cleanup();
    }

    interface OnUserSelectedListener{
        void OnUserSelected(User user);
    }

    public static class SearchHolder extends RecyclerView.ViewHolder{
        TextView text;
        public SearchHolder(View itemView) {
            super(itemView);
            text = (TextView)itemView.findViewById(R.id.text);
        }

        public void bind(final User user, final OnUserSelectedListener listener){
            text.setText(user.name);
            text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.OnUserSelected(user);
                }
            });
        }
    }
}
