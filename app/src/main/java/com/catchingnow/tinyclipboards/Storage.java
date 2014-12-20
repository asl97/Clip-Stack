package com.catchingnow.tinyclipboards;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by heruoxin on 14/12/9.
 */
public class Storage {
    private static final String TABLE_NAME = "cliphistory";
    private static final String CLIP_STRING = "history";
    private static final String CLIP_DATE = "date";
    private StorageHelper dbHelper;
    private SQLiteDatabase db;
    private List<String> clipsInMemory;
    private boolean isClipsInMemoryChanged = true;

    public Storage(Context context) {
        dbHelper = new StorageHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public List<String> getAllClipHistory() {
        if (isClipsInMemoryChanged) {
            String sortOrder = CLIP_DATE + " DESC";
            String[] COLUMNS = {CLIP_STRING};
            Cursor c = db.query(TABLE_NAME, COLUMNS, null, null, null, null, sortOrder);
            clipsInMemory = new ArrayList<String>();
            while(c.moveToNext()) {
                clipsInMemory.add(c.getString(0));
            }
            c.close();
            isClipsInMemoryChanged = false;
        }
        return clipsInMemory;
    }
    public List<String> getClipHistory(int n) {
        //get the `n`th String of ClipHistory
        List<String> ClipHistory = getAllClipHistory();
        List<String> thisClips = new ArrayList<String>();
        n = (n > ClipHistory.size() ? ClipHistory.size() : n);
        for (int i=0; i < n; i++) {
            thisClips.add(ClipHistory.get(i));
        }
        return thisClips;
    }
    public boolean addClipHistory(String currentString) {
        List<String> tmpClips = getAllClipHistory();
        for (String str: tmpClips) {
            if (str.contains(currentString)) {
                return false;
            }
        }
        Date date = new Date();
        long timestamp = date.getTime();
        ContentValues values = new ContentValues();
        values.put(CLIP_DATE, timestamp);
        values.put(CLIP_STRING, currentString);
        long rowid = db.insert(TABLE_NAME, null, values);
        if (rowid == -1) {
            Log.e("Storage", "write db error: " + currentString);
            return false;
        }
        isClipsInMemoryChanged = true;
        return true;
    }

//    public void printClips(int n) {
//        for (int i=0; i<n; i++){
//            String s = getClipHistory(n);
//            Log.v("printClips", s);
//        }
//    }
}