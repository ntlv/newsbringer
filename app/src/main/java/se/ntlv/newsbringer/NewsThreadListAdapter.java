package se.ntlv.newsbringer;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import se.ntlv.newsbringer.database.PostTable;


class NewsThreadListAdapter extends ResourceCursorAdapter {

	public NewsThreadListAdapter(Context context, int layout, Cursor c, int flags) {
		super(context, layout, c, flags);
	}

	static String get(Cursor cursor, String columnName) {
		return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
	}

	static ViewHolder getOrMakeTag(View view) {
		ViewHolder tag = (ViewHolder) view.getTag();
		if (tag == null) {
			ViewHolder newTag = new ViewHolder(view);
			view.setTag(newTag);
			return newTag;
		} else {
			return tag;
		}
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder tag = getOrMakeTag(view);

		tag.title.setText(get(cursor, PostTable.COLUMN_TITLE));
		tag.by.setText(get(cursor, PostTable.COLUMN_BY));
		tag.ordinal.setText(get(cursor, PostTable.COLUMN_ORDINAL));
		tag.time.setText(get(cursor, PostTable.COLUMN_TIMESTAMP));
		tag.score.setText(get(cursor, PostTable.COLUMN_SCORE));
		tag.link = get(cursor, PostTable.COLUMN_URL);
	}

	static class ViewHolder {

		public TextView title;
		public TextView by;
		public TextView ordinal;
		public TextView time;
		public TextView score;
		public String link;

		public ViewHolder(View root) {
			title = (TextView) root.findViewById(R.id.title);
			by = (TextView) root.findViewById(R.id.by);
			ordinal = (TextView) root.findViewById(R.id.ordinal);
			time = (TextView) root.findViewById(R.id.time);
			score = (TextView) root.findViewById(R.id.score);
		}
	}
}

