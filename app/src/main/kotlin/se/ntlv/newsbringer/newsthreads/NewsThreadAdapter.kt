package se.ntlv.newsbringer.newsthreads

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onLongClick
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.DataLoadingFacilitator
import se.ntlv.newsbringer.adapter.GenericRecyclerViewAdapter
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.network.NewsThreadUiData

open class NewsThreadAdapter(val layout: Int, f: DataLoadingFacilitator) : GenericRecyclerViewAdapter<NewsThreadUiData, NewsThreadAdapter.ViewHolder>(f) {
    override fun viewToDataPosition(viewPosition: Int) = viewPosition

    override fun dataToViewPosition(dataPosition: Int) = dataPosition

    override val deltaUpdatingEnabled = true

    override fun actualItemCount(): Int {
        val count = data?.count ?: 0
        return count
    }

    var clickListener: (ViewHolder?) -> Unit? = {}
    var longClickListener: (ViewHolder?) -> Boolean = { false }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
        val v = LayoutInflater.from(parent?.context).inflate(layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: NewsThreadUiData) {
        viewHolder.bind(item, clickListener, longClickListener)
    }

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val view = root
        val title = root.find<TextView>(R.id.title)
        val by = root.find<TextView>(R.id.by)
        val time = root.find<DateView>(R.id.time)
        val score = root.find<TextView>(R.id.score)
        var link: String? = null
        var id: Long? = null
        val commentCount = root.find<TextView>(R.id.comment_count)
        val ordinal = root.find<TextView>(R.id.ordinal)

        fun bind(item: NewsThreadUiData, onClick: (ViewHolder?) -> Unit?, onLongClick: (ViewHolder?) -> Boolean) {
            val titlePrefix = if (item.isStarred == 1) "\u2605" else ""
            title.text = titlePrefix + item.title
            by.text = item.by
            time.text = item.time.toString()
            score.text = item.score.toString()
            link = item.url
            id = item.id
            commentCount.text = item.descendants.toString()
            ordinal.text = item.ordinal.toString()

            view.onClick { onClick(this) }
            view.onLongClick { onLongClick(this) }
        }

        override fun toString(): String {
            return "ViewHolder(view=$view, title=${title.text}, by=${by.text}, time=${time.text}, score=${score.text}, link=$link, id=$id, commentCount=${commentCount.text}, ordinal=${ordinal.text})"
        }


    }
}


