package se.ntlv.newsbringer.newsthreads

import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.CursorRecyclerViewAdapter
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.database.getInt
import se.ntlv.newsbringer.database.getLong
import se.ntlv.newsbringer.database.getString

open class NewsThreadAdapter(layout: Int) : CursorRecyclerViewAdapter<NewsThreadAdapter.ViewHolder>() {
    override val actualItemCount: Int
        get() = mCursor?.count ?: 0

    var clickListener: (ViewHolder?) -> Unit? = {}
    var longClickListener: (ViewHolder?) -> Unit? = {}

    val mLayout = layout

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
        val v = LayoutInflater.from(parent?.context).inflate(mLayout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, cursor: Cursor) {
        val isStarred = cursor.getInt(PostTable.COLUMN_STARRED) == 1
        val titlePrefix = if (isStarred) "\u2605" else ""
        val title = cursor.getString(PostTable.COLUMN_TITLE)
        viewHolder.title.text = titlePrefix + title
        viewHolder.by.text = cursor.getString(PostTable.COLUMN_BY)
        viewHolder.time.text = cursor.getString(PostTable.COLUMN_TIMESTAMP)
        viewHolder.score.text = cursor.getString(PostTable.COLUMN_SCORE)
        viewHolder.link = cursor.getString(PostTable.COLUMN_URL)
        viewHolder.id = cursor.getLong(PostTable.COLUMN_ID)
        val children = cursor.getString(PostTable.COLUMN_CHILDREN)
        if (children.length > 0) {
            val strings = children.split(',')
            viewHolder.commentCount.text = strings.size.toString()
            viewHolder.commentQuantity = strings.size.toLong()
        } else {
            viewHolder.commentCount.text = "0"
            viewHolder.commentQuantity = 0L
        }
        viewHolder.self.setOnClickListener { clickListener(viewHolder) }
        viewHolder.self.setOnLongClickListener { longClickListener(viewHolder); true }
    }

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val self = root
        val title = root.findViewById(R.id.title) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as DateView
        val score = root.findViewById(R.id.score) as TextView
        var link: String? = null
        var id: Long? = null
        val commentCount = root.findViewById(R.id.comment_count) as TextView
        var commentQuantity: Long? = null
    }
}


