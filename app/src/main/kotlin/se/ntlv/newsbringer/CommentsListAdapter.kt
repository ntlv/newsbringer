package se.ntlv.newsbringer

import android.database.Cursor
import android.content.Context
import android.widget.ResourceCursorAdapter
import android.view.View
import android.widget.TextView
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.NewsThreadListAdapter.ViewHolder
import se.ntlv.newsbringer.network.Metadata
import se.ntlv.newsbringer.database.CommentsTable

open class CommentsListAdapter(ctx: Context, layout: Int, cursor: Cursor?, flags: Int) :
        ResourceCursorAdapter(ctx, layout, cursor, flags) {

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val tag = view.tag
        tag.by.setText(cursor.getString(CommentsTable.COLUMN_BY))
        tag.time.setText(cursor.getString(CommentsTable.COLUMN_TIME))
        tag.text.setText(cursor.getString(CommentsTable.COLUMN_TEXT))
    }

    fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
    fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))


    class ViewHolder(root: View) {
        val text = root.findViewById(R.id.text) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as TextView
    }

    val View.tag: ViewHolder
        get() {
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


