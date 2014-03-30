package org.omnirom.music.app.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;

import org.omnirom.music.app.MainActivity;
import org.omnirom.music.app.R;
import org.omnirom.music.app.adapters.PlaylistAdapter;
import org.omnirom.music.app.ui.ExpandableHeightGridView;
import org.omnirom.music.app.ui.FlowLayout;
import org.omnirom.music.model.Album;
import org.omnirom.music.model.Artist;
import org.omnirom.music.model.Playlist;
import org.omnirom.music.model.Song;
import org.omnirom.music.providers.ILocalCallback;
import org.omnirom.music.providers.IMusicProvider;
import org.omnirom.music.providers.ProviderAggregator;
import org.omnirom.music.providers.ProviderCache;
import org.omnirom.music.providers.ProviderConnection;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Use the {@link PlaylistFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class PlaylistFragment extends Fragment implements ILocalCallback {

    private PlaylistAdapter mAdapter;
    private Handler mHandler;
    private final ArrayList<Playlist> mPlaylistsUpdated = new ArrayList<Playlist>();

    private Runnable mUpdateListRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mPlaylistsUpdated) {
                for (Playlist p : mPlaylistsUpdated) {
                    if (mAdapter.contains(p)) {
                        mAdapter.notifyDataSetChanged();
                    } else {
                        mAdapter.addItem(p);
                    }
                }

                mPlaylistsUpdated.clear();
            }
        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PlaylistFragment.
     */
    public static PlaylistFragment newInstance() {
        PlaylistFragment fragment = new PlaylistFragment();
        return fragment;
    }
    public PlaylistFragment() {
        mAdapter = new PlaylistAdapter();
        mHandler = new Handler();

        ProviderAggregator.getDefault().addUpdateCallback(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_playlist, container, false);
        ExpandableHeightGridView playlistLayout =
                (ExpandableHeightGridView) root.findViewById(R.id.gvPlaylists);
        playlistLayout.setAdapter(mAdapter);
        playlistLayout.setExpanded(true);

        // Set the initial playlists
        List<Playlist> playlists = ProviderAggregator.getDefault().getAllPlaylists();
        mAdapter.addAll(playlists);

        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.SECTION_PLAYLISTS);
    }

    @Override
    public void onSongUpdate(Song s) {

    }

    @Override
    public void onAlbumUpdate(Album a) {

    }

    @Override
    public void onPlaylistUpdate(final Playlist p) {
        synchronized (mPlaylistsUpdated) {
            mPlaylistsUpdated.add(p);
        }

        mHandler.removeCallbacks(mUpdateListRunnable);
        mHandler.post(mUpdateListRunnable);
    }

    @Override
    public void onArtistUpdate(Artist a) {

    }

    @Override
    public void onProviderConnected(IMusicProvider provider) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.addAllUnique(ProviderAggregator.getDefault().getAllPlaylists());
            }
        });
    }
}