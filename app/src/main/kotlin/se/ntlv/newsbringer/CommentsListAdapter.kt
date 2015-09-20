package se.ntlv.newsbringer

import android.content.Context
import android.database.Cursor
import android.text.Html
import android.util.TypedValue
import android.view.View
import android.widget.ResourceCursorAdapter
import android.widget.TextView
import se.ntlv.newsbringer.database.CommentsTable

open class CommentsListAdapter(ctx: Context, layout: Int, cursor: Cursor?, flags: Int) :
        ResourceCursorAdapter(ctx, layout, cursor, flags) {

    val pxPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, ctx.resources.displayMetrics).toInt()

    val color = arrayOf(
            R.color.cyan,
            R.color.green,
            R.color.orange,
            R.color.pink,
            R.color.purple,
            R.color.red,
            R.color.teal,
            R.color.yellow
    )

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val tag = view.tag
        val ancestorCount = cursor.getInt(CommentsTable.COLUMN_ANCESTOR_COUNT)
        val padding = (pxPadding * ancestorCount).toInt()
        view.setPadding(padding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom())

        tag.colorView.setBackgroundResource(color.get(ancestorCount.mod(color.size())))

        tag.by.text = cursor.getString(CommentsTable.COLUMN_BY)
        tag.time.text = cursor.getString(CommentsTable.COLUMN_TIME)
        tag.text.htmlText = cursor.getString(CommentsTable.COLUMN_TEXT)
        tag.id = cursor.getLong(CommentsTable.COLUMN_ID)

        val kidsString = cursor.getString(CommentsTable.COLUMN_KIDS)
        val count = if (kidsString.isNotEmpty()) {
            kidsString.split(',').size().toString()
        } else {
            ""
        }
        tag.kids.text = count
    }

    var TextView.htmlText : String
        get() {
            return this.text as String
        }
        set(source: String) {
            this.text = Html.fromHtml(source)
        }

    class ViewHolder(root: View) {
        val text = root.findViewById(R.id.text) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as TextView
        var id: Long? = null
        val kids = root.findViewById(R.id.kids) as TextView
        val colorView = root.findViewById(R.id.color_band)
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



