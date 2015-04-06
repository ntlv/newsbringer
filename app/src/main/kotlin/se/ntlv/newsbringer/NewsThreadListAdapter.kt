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
        tag.time.setText(cursor.getString(PostTable.COLUMN_TIMESTAMP))
        tag.score.setText(cursor.getString(PostTable.COLUMN_SCORE))
        tag.link = cursor.getString(PostTable.COLUMN_URL)
        tag.id = cursor.getLong(PostTable.COLUMN_ID)
        tag.text = cursor.getString(PostTable.COLUMN_TEXT)
        val children = cursor.getString(PostTable.COLUMN_CHILDREN)
        if (children.length() > 0) {
            val strings = children.split(',')
            tag.commentCount.setText(strings.size().toString())
            tag.commentQuantity = strings.size().toLong()
        } else {
            tag.commentCount.setText("0")
            tag.commentQuantity = 0L
        }
    }

    fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
    fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))


    class ViewHolder(root: View) {
        val title = root.findViewById(R.id.title) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as DateView
        val score = root.findViewById(R.id.score) as TextView
        var link: String? = null
        var id: Long? = null
        var text: String? = null
        val commentCount = root.findViewById(R.id.comment_count) as TextView
        var commentQuantity: Long? = null

        fun TextView.getContent(): String = this.getText().toString()

        val metadata: Metadata
            get() = Metadata(id as Long, text ?: "", title.getContent(), by.getContent(), time.getContent(), score.getContent(), link, commentQuantity ?: 0)
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


