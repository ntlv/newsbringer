package se.ntlv.newsbringer.comments

import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.BindingViewHolder
import se.ntlv.newsbringer.adapter.starify
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.network.RowItem

class HeaderHolder(root: View, private val clickListener: (View) -> Unit, private val navigator : Navigator) : BindingViewHolder<RowItem.NewsThreadUiData>(root) {
    val self = root
    val title = root.find<TextView>(R.id.comments_header_title)
    val by = root.find<TextView>(R.id.by)
    val time = root.find<DateView>(R.id.submission_time)
    val score = root.find<TextView>(R.id.score)
    val commentCount = root.find<TextView>(R.id.comment_count)
    val text = root.find<TextView>(R.id.submission_text)

    override fun bind(item: RowItem.NewsThreadUiData) {
        title.text = item.title.starify(item.isStarred)
        by.text = item.by
        time.text = item.time.toString()
        if (item.text.isNullOrEmpty()) {
            text.visibility = View.GONE
        } else {
            text.visibility = View.VISIBLE
            text.setHtml(item.text, navigator)
        }
        score.text = item.score.toString()
        commentCount.text = item.descendants.toString()
        self.setOnClickListener(clickListener)
    }
}
