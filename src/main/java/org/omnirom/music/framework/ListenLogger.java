/*
 * Copyright (C) 2014 Fastboot Mobile, LLC.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see <http://www.gnu.org/licenses>.
 */

package org.omnirom.music.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.omnirom.music.model.Song;
import org.omnirom.music.providers.ProviderIdentifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class handling logging of played and liked songs
 */
public class ListenLogger {
    private static final String TAG = "ListenLogger";
    private static final boolean DEBUG = true;

    private static final String PREFS = "ListenLogger";
    private static final String PREF_HISTORY_ENTRIES = "history_entries";
    private static final String PREF_LIKED_ENTRIES = "liked_entries";

    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_SONG_REF = "song_ref";
    private static final String KEY_PROVIDER = "provider";

    private SharedPreferences mPrefs;

    public ListenLogger(Context ctx) {
        mPrefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Adds an entry to the song history. The time used will be the current time.
     * @param song The song to add
     */
    public void addEntry(Song song) {
        SharedPreferences.Editor editor = mPrefs.edit();
        Set<String> entries = new TreeSet<>(mPrefs.getStringSet(PREF_HISTORY_ENTRIES,
                new TreeSet<String>()));

        JSONObject jsonRoot = new JSONObject();
        try {
            jsonRoot.put(KEY_TIMESTAMP, new Date().getTime());
            jsonRoot.put(KEY_SONG_REF, song.getRef());
            jsonRoot.put(KEY_PROVIDER, song.getProvider().serialize());
        } catch (JSONException ignore) {}

        entries.add(jsonRoot.toString());
        editor.putStringSet(PREF_HISTORY_ENTRIES, entries);
        editor.apply();
    }

    /**
     * Fetches and builds a list of all the history entries
     * @return A list of entries
     */
    public List<LogEntry> getEntries() {
        Set<String> entries = mPrefs.getStringSet(PREF_HISTORY_ENTRIES, null);
        List<LogEntry> output = new ArrayList<>();
        if (entries != null) {
            if (DEBUG) Log.d(TAG, "Log entries: " + entries.size());
            for (String entry : entries) {
                try {
                    JSONObject jsonObj = new JSONObject(entry);
                    String songRef = jsonObj.getString(KEY_SONG_REF);
                    String providerSerialized = jsonObj.getString(KEY_PROVIDER);
                    long timestamp = jsonObj.getLong(KEY_TIMESTAMP);

                    output.add(new LogEntry(songRef, providerSerialized, timestamp));
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Exception while trying to get log entry", e);
                }
            }
        }

        return output;
    }

    /**
     * Adds, if not already, a song to the list of liked songs.
     * @param song The song to add
     */
    public void addLike(Song song) {
        SharedPreferences.Editor editor = mPrefs.edit();
        Set<String> entries = new TreeSet<>(mPrefs.getStringSet(PREF_LIKED_ENTRIES, new TreeSet<String>()));

        JSONObject jsonRoot = new JSONObject();
        try {
            jsonRoot.put(KEY_SONG_REF, song.getRef());
            jsonRoot.put(KEY_PROVIDER, song.getProvider().serialize());
        } catch (JSONException ignore) {}

        entries.add(jsonRoot.toString());

        editor.putStringSet(PREF_LIKED_ENTRIES, entries);
        editor.apply();
    }

    /**
     * Removes a song from the list of liked songs
     * @param song The song to remove
     */
    public void removeLike(Song song) {
        SharedPreferences.Editor editor = mPrefs.edit();
        Set<String> entries = new TreeSet<>(mPrefs.getStringSet(PREF_LIKED_ENTRIES, new TreeSet<String>()));

        JSONObject jsonRoot = new JSONObject();
        try {
            jsonRoot.put(KEY_SONG_REF, song.getRef());
            jsonRoot.put(KEY_PROVIDER, song.getProvider().serialize());
        } catch (JSONException ignore) {}

        entries.remove(jsonRoot.toString());
        editor.putStringSet(PREF_LIKED_ENTRIES, entries);
        editor.apply();
    }

    /**
     * @return a list of all the liked entries
     */
    public List<LogEntry> getLikedEntries() {
        Set<String> entries = mPrefs.getStringSet(PREF_LIKED_ENTRIES, null);
        List<LogEntry> output = new ArrayList<>();
        if (entries != null) {
            for (String entry : entries) {
                try {
                    JSONObject jsonObj = new JSONObject(entry);
                    String songRef = jsonObj.getString(KEY_SONG_REF);
                    String providerSerialized = jsonObj.getString(KEY_PROVIDER);

                    output.add(new LogEntry(songRef, providerSerialized, 0));
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Exception while trying to get liked entries", e);
                }
            }
        }

        return output;
    }

    /**
     * Returns whether or not the song reference provided is in the list of liked songs or not
     * @param ref The reference of the song
     * @return true if the song is liked
     */
    public boolean isLiked(String ref) {
        Set<String> entries = mPrefs.getStringSet(PREF_LIKED_ENTRIES, null);
        if (entries != null) {
            for (String entry : entries) {
                try {
                    JSONObject jsonObj = new JSONObject(entry);
                    String songRef = jsonObj.getString(KEY_SONG_REF);
                    if (songRef.equals(ref)) {
                        return true;
                    }
                } catch (JSONException ignore) {
                }
            }

            return false;
        } else {
            return false;
        }
    }

    /**
     * Class representing an entry in either the log or the list of liked songs
     */
    public static class LogEntry {
        private Date mTimestamp;
        private String mSongRef;
        private ProviderIdentifier mIdentifier;

        private LogEntry(String songRef, String serializedProviderIdentifier, long timestamp) {
            mSongRef = songRef;
            mIdentifier = ProviderIdentifier.fromSerialized(serializedProviderIdentifier);
            mTimestamp = new Date(timestamp);
        }

        /**
         * @return The reference of the song
         */
        public String getReference() {
            return mSongRef;
        }

        /**
         * @return The provider identifier of the song
         */
        public ProviderIdentifier getIdentifier() {
            return mIdentifier;
        }

        /**
         * @return The timestamp at which this song was added (valid only for history, not likes)
         */
        public Date getTimestamp() {
            return mTimestamp;
        }
    }
}
