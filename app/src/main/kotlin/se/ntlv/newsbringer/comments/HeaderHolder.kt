package se.ntlv.newsbringer.comments

import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.BindingViewHolder
import se.ntlv.newsbringer.adapter.starify
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.network.NewsThreadUiData
import se.ntlv.newsbringer.network.RowItem

class HeaderHolder(root: View, private val clickListener: (View) -> Unit, private val navigator : Navigator) : BindingViewHolder<RowItem>(root) {
    val self = root
    val title = root.find<TextView>(R.id.item_title)
    val by = root.find<TextView>(R.id.by)
    val time = root.find<DateView>(R.id.submission_time)
    val score = root.find<TextView>(R.id.score)
    val commentCount = root.find<TextView>(R.id.comment_count)
    val text = root.find<TextView>(R.id.submission_text)
    val ordinal = root.find<TextView>(R.id.ordinal)

    override fun bind(item: RowItem) {
        val castItem = item as NewsThreadUiData
        ordinal.text = castItem.ordinal.toString()
        title.text = castItem.title.starify(castItem.isStarred)
        by.text = castItem.by
        time.text = castItem.time.toString()
        if (castItem.text.isNullOrEmpty()) {
            text.visibility = View.GONE
        } else {
            text.visibility = View.VISIBLE
            text.setHtml(castItem.text, navigator)
        }
        score.text = castItem.score.toString()
        commentCount.text = castItem.descendants.toString()
        self.setOnClickListener(clickListener)
    }
}
