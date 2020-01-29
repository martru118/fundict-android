package com.martru118.fundict.ui.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.martru118.fundict.Helper.DatabaseOpenHelper;
import com.martru118.fundict.MainActivity;
import com.martru118.fundict.R;

public class FavoritesFragment extends Fragment {
    private DatabaseOpenHelper db;
    private FavAdapter adapter;

    public FavoritesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frame_favorites, container, false);
        db = new DatabaseOpenHelper(getContext());

        //set up recycler view and its adapter
        final RecyclerView favoritesList = v.findViewById(R.id.fav_list);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);

        favoritesList.setLayoutManager(llm);
        adapter = new FavAdapter(getContext(), db.getAllFavorites());
        favoritesList.setAdapter(adapter);

        ((MainActivity)getActivity()).setRefreshListener(new MainActivity.FragmentRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.swapCursor(db.getAllFavorites());
            }
        });

        //scroll to recyclerview
        if (adapter.getItemCount()>0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    favoritesList.smoothScrollToPosition(adapter.getItemCount()-1);
                }
            }, 500);
        } else {
            Snackbar.make(v.findViewById(R.id.fragment), "You have no bookmarks", Snackbar.LENGTH_LONG).show();
        }

        //swipe to remove
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                removeFromFavorites((long)viewHolder.itemView.getTag());
            }
        }).attachToRecyclerView(favoritesList);

        return v;
    }

    private void removeFromFavorites(long id) {
        db.swipetoRemove(id);
        adapter.swapCursor(db.getAllFavorites());
        Toast.makeText(getContext(), "Removed from bookmarks", Toast.LENGTH_SHORT).show();
    }
}
