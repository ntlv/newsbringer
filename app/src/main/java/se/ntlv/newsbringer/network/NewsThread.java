package se.ntlv.newsbringer.network;

import android.content.ContentValues;

import java.util.Arrays;

import se.ntlv.newsbringer.database.PostTable;

class NewsThread {
    public int score;
    public long time;
    public long id;
    public String by;
    public String title;
    public long[] kids;
    public String text;
    public String type;
    public String url;

    public ContentValues getAsContentValues() {
        ContentValues cv = new ContentValues(10);
        cv.put(PostTable.COLUMN_ID, id);
        cv.put(PostTable.COLUMN_SCORE, score);
        cv.put(PostTable.COLUMN_TIMESTAMP, time);
        cv.put(PostTable.COLUMN_BY, by);
        cv.put(PostTable.COLUMN_TITLE, title);
        cv.put(PostTable.COLUMN_CHILDREN, kids == null ? "[]" : Arrays.toString(kids));
        cv.put(PostTable.COLUMN_TEXT, text == null ? "No text" : text);
        cv.put(PostTable.COLUMN_TYPE, type == null ? "No type" : type);
        cv.put(PostTable.COLUMN_URL, url == null ? "about:blank" : url);

        long unixTime = System.currentTimeMillis() / 1000;
        long hoursSinceSubmission = unixTime - time /3600;
        double adjustedScore = score - 1;
        double ordinal = adjustedScore / (Math.pow(hoursSinceSubmission + 2 , 1.8));

        cv.put(PostTable.COLUMN_ORDINAL, ordinal);

        return cv;
    }
}
