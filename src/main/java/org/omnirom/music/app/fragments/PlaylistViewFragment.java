package org.omnirom.music.app.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import org.omnirom.music.app.R;
import org.omnirom.music.app.adapters.PlaylistAdapter;
import org.omnirom.music.app.ui.PlaylistListView;
import org.omnirom.music.framework.PlaybackState;
import org.omnirom.music.framework.PluginsLookup;
import org.omnirom.music.model.Album;
import org.omnirom.music.model.Artist;
import org.omnirom.music.model.Playlist;
import org.omnirom.music.model.SearchResult;
import org.omnirom.music.model.Song;
import org.omnirom.music.providers.ILocalCallback;
import org.omnirom.music.providers.IMusicProvider;
import org.omnirom.music.providers.ProviderAggregator;

import java.util.Iterator;
import java.util.List;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Use the {@link org.omnirom.music.app.fragments.PlaylistViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class PlaylistViewFragment extends Fragment
        implements ILocalCallback, PlaybackState.Listener {

    private static final String TAG = "PlaylistViewFragment";
    public static final String KEY_PLAYLIST = "playlist";

    private PlaylistAdapter mAdapter;
    private Handler mHandler;
    private Playlist mPlaylist;
    private PlaybackState mPlaybackState;

    private BitmapDrawable mCell;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PlaylistViewFragment.
     */
    public static PlaylistViewFragment newInstance(Playlist p) {
        PlaylistViewFragment fragment = new PlaylistViewFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_PLAYLIST, p);
        fragment.setArguments(bundle);
        return fragment;
    }

    public PlaylistViewFragment() {
        mHandler = new Handler();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args == null) {

            throw new IllegalArgumentException("This fragment must have a valid playlist");
        }

        // Get the playlist from the arguments, from the instantiation
        mPlaylist = args.getParcelable(KEY_PLAYLIST);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_playlist_view, container, false);
        assert root != null;

        PlaylistListView lvPlaylistContents = (PlaylistListView) root.findViewById(R.id.lvPlaylistContents);
        mAdapter = new PlaylistAdapter(root.getContext());
        lvPlaylistContents.setAdapter(mAdapter);

        // Fill the playlist
        Iterator<String> songIt = mPlaylist.songs();
        while (songIt.hasNext()) {
            String songRef = songIt.next();
            Song song = ProviderAggregator.getDefault().getCache().getSong(songRef);
            if (song == null) {
                song = ProviderAggregator.getDefault().retrieveSong(songRef, mPlaylist.getProvider());
            }
            mAdapter.addItem(song);
        }
        mAdapter.notifyDataSetChanged();
        mAdapter.setPlaylist(mPlaylist);
        // Set the list listener
        lvPlaylistContents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                // Play the song
                Song song = mAdapter.getItem(i);

                if (song != null) {
                    try {
                        PluginsLookup.getDefault().getPlaybackService().playSong(song);
                    } catch (RemoteException e) {
                        Log.e("TEST", "Unable to play song", e);
                    }
                } else {
                    Log.e(TAG, "Trying to play null song!");
                }
            }
        });


        // Fill the playlist information
        TextView tvPlaylistName = (TextView) root.findViewById(R.id.tvPlaylistName);
        TextView tvNumTracks = (TextView) root.findViewById(R.id.tvNumTracks);

        tvPlaylistName.setText(mPlaylist.getName());
        tvNumTracks.setText(getString(R.string.nb_tracks, mPlaylist.getSongsCount()));

        return root;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
/*        MainActivity main = (MainActivity) activity;
        main.onSectionAttached(MainActivity.SECTION_PLAYLISTS);*/
        mPlaybackState = new PlaybackState();
        mPlaybackState.addListener(this);

        ProviderAggregator.getDefault().addUpdateCallback(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ProviderAggregator.getDefault().removeUpdateCallback(this);
        mPlaybackState.removeListener(this);
    }

    @Override
    public void onSongUpdate(List<Song> s) {
        // We check if the song belongs to this playlist
        boolean hasPlaylist = false;
        Iterator<String> songsRef = mPlaylist.songs();
        while (songsRef.hasNext()) {
            String ref = songsRef.next();
            for (Song song : s) {
                if (song.getRef().equals(ref)) {
                    hasPlaylist = true;
                    break;
                }
            }

            if (hasPlaylist) {
                break;
            }
        }

        // It does, update the list then
        if (hasPlaylist) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAlbumUpdate(List<Album> a) {

    }

    @Override
    public void onPlaylistUpdate(final List<Playlist> p) {
        // If the currently watched playlist is updated, update me
        if (p.equals(mPlaylist)) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onArtistUpdate(List<Artist> a) {

    }

    @Override
    public void onProviderConnected(IMusicProvider provider) {

    }

    @Override
    public void onSearchResult(SearchResult searchResult) {

    }

    @Override
    public void onCurrentSongChanged(PlaybackState state, final Song song) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.setCurrentSong(song);
                mAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void onPlaybackPositionChanged(PlaybackState state, int posMs) {

    }

    @Override
    public void onPlaybackStateChanged(PlaybackState state, int newState) {

    }
}
