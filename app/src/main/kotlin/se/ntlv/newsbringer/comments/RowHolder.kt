package se.ntlv.newsbringer.comments

import android.view.View
import android.widget.TextView
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.find
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.BindingViewHolder
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.network.RowItem


class RowHolder(root: View, private val nestingIncrement: Int, private val navigator : Navigator) : BindingViewHolder<RowItem.CommentUiData>(root) {
    val self = root
    val text = root.find<TextView>(R.id.comment_text)
    val by = root.find<TextView>(R.id.by)
    val time = root.find<DateView>(R.id.comment_submission_time)
    var id: Long? = null
    val kids = root.find<TextView>(R.id.comment_child_count)
    val colorView = root.find<View>(R.id.color_band)

    val color = arrayOf(R.color.cyan, R.color.green, R.color.orange, R.color.pink, R.color.purple, R.color.red, R.color.teal, R.color.yellow)

    override fun bind(item: RowItem.CommentUiData) {
        val ancestorCount = item.ancestorCount
        val padding = nestingIncrement * ancestorCount
        self.setPadding(padding, self.paddingTop, self.paddingRight, self.paddingBottom)

        colorView.backgroundResource = color[ancestorCount % color.size]

        by.text = "${item.ordinal} - ${item.by}"
        time.text = item.time.toString()
        val content = if (!item.text.isNullOrEmpty()) item.text else "[Removed]"
        text.setHtml(content, navigator)
        id = item.id

        val kidsString = item.kids
        val count = if (kidsString.isNullOrBlank()) 0 else kidsString.split(',').size
        kids.text = count.toString()
    }
}
