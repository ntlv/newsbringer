package se.ntlv.newsbringer

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.ColorDrawable
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.ResourceCursorAdapter
import android.widget.TextView
import se.ntlv.newsbringer.database.CommentsTable

open class CommentsListAdapter(ctx: Context, layout: Int, cursor: Cursor?, flags: Int) :
        ResourceCursorAdapter(ctx, layout, cursor, flags) {

    val pxPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, ctx.getResources().getDisplayMetrics()).toInt()

    val color = array(
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

        val colorDrawable = ColorDrawable(color.get(ancestorCount.mod(color.size())))
        Log.d("Comments adapter", "Color = $colorDrawable")
        tag.colorView.setImageDrawable(colorDrawable)

        tag.by.setText(cursor.getString(CommentsTable.COLUMN_BY))
        tag.time.setText(cursor.getString(CommentsTable.COLUMN_TIME))
        tag.text.setHtmlText(cursor.getString(CommentsTable.COLUMN_TEXT))
        tag.id = cursor.getLong(CommentsTable.COLUMN_ID)

        val kidsString = cursor.getString(CommentsTable.COLUMN_KIDS)
        val count = if (kidsString.isNotEmpty()) {
            kidsString.split(',').size().toString()
        } else {
            ""
        }
        tag.kids.setText(count)


    }

    fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
    fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))
    fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

    fun TextView.setHtmlText(source: String) = this.setText(Html.fromHtml(source))


    class ViewHolder(root: View) {
        val text = root.findViewById(R.id.text) as TextView
        val by = root.findViewById(R.id.by) as TextView
        val time = root.findViewById(R.id.time) as TextView
        var id: Long? = null
        val kids = root.findViewById(R.id.kids) as TextView
        val colorView = root.findViewById(R.id.color_band) as ImageView
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



