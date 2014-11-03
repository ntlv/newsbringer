package se.ntlv.newsbringer

import android.database.Cursor
import android.content.Context
import android.widget.ResourceCursorAdapter
import android.view.View
import android.widget.TextView
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.NewsThreadListAdapter.ViewHolder

open class NewsThreadListAdapter(ctx: Context, layout: Int, cursor: Cursor?, flags: Int) :
        ResourceCursorAdapter(ctx, layout, cursor, flags) {

    override fun bindView(view: View, context: Context?, cursor: Cursor) {

        val tag = view.getOrMakeTag()

        tag.title.setText(cursor.get(PostTable.COLUMN_TITLE))
        tag.by.setText(cursor.get(PostTable.COLUMN_BY))
        tag.ordinal.setText(cursor.get(PostTable.COLUMN_ORDINAL))
        tag.time.setText(cursor.get(PostTable.COLUMN_TIMESTAMP))
        tag.score.setText(cursor.get(PostTable.COLUMN_SCORE))
        tag.link = cursor.get(PostTable.COLUMN_URL)
    }

    fun Cursor.get(columnName: String): String {
        return this.getString(this.getColumnIndexOrThrow(columnName))
    }


    class ViewHolder(root: View) {
        val text = root.findViewById(R.id.text) as TextView
        val title = root.findViewById(R.id.title) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val ordinal = root.findViewById(R.id.ordinal) as TextView
        val time = root.findViewById(R.id.time) as DateView
        val score = root.findViewById(R.id.score) as TextView
        var link: String? = null
    }

    fun View.getOrMakeTag(): ViewHolder {
        val tag = getTag()
        if (tag == null) {
            val newTag = ViewHolder(this)
            this.setTag(newTag)
            return newTag
        } else {
            return tag as ViewHolder
        }
    }

}

