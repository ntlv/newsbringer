package se.ntlv.newsbringer

import android.database.Cursor
import android.content.Context
import android.widget.ResourceCursorAdapter
import android.view.View
import android.widget.TextView
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.NewsThreadListAdapter.ViewHolder
import se.ntlv.newsbringer.network.NewsThread.Metadata

open class NewsThreadListAdapter(ctx: Context, layout: Int, cursor: Cursor?, flags: Int) :
        ResourceCursorAdapter(ctx, layout, cursor, flags) {

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val tag = view.tag
        tag.title.setText(cursor.getString(PostTable.COLUMN_TITLE))
        tag.by.setText(cursor.getString(PostTable.COLUMN_BY))
        tag.ordinal.setText(cursor.getString(PostTable.COLUMN_ORDINAL))
        tag.time.setText(cursor.getString(PostTable.COLUMN_TIMESTAMP))
        tag.score.setText(cursor.getString(PostTable.COLUMN_SCORE))
        tag.link = cursor.getString(PostTable.COLUMN_URL)
        tag.id = cursor.getLong(PostTable.COLUMN_ID)
        tag.text = cursor.getString(PostTable.COLUMN_TEXT)
    }

    fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
    fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))


    class ViewHolder(root: View) {
        val title = root.findViewById(R.id.title) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val ordinal = root.findViewById(R.id.ordinal) as TextView
        val time = root.findViewById(R.id.time) as DateView
        val score = root.findViewById(R.id.score) as TextView
        var link: String? = null
        var id: Long? = null
        var text: String? = null

        fun TextView.getContent(): String = this.getText().toString()

        val metadata: Metadata
            get() = Metadata(id, text ?: "", title.getContent(), by.getContent(), time.getContent(), score.getContent(), link)
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


