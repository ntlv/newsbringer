package se.ntlv.newsbringer.database

import android.os.Parcel
import android.os.Parcelable
import android.support.v7.util.DiffUtil
import android.support.v7.util.DiffUtil.DiffResult
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import se.ntlv.newsbringer.network.RowItem
import se.ntlv.newsbringer.customviews.DataDiffCallback

class DataCommentsThread(internal val base: List<RowItem>, override val diff: DiffResult) : AdapterModelCollection<RowItem> {
    override fun get(position: Int) = base[position]

    override val size = base.size

    companion object : AnkoLogger {

        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<DataCommentsThread> {
            override fun newArray(size: Int): Array<DataCommentsThread?> = arrayOfNulls(size)

            override fun createFromParcel(source: Parcel): DataCommentsThread {
                info("Creating from parcel")
                val header = source.readTypedObject(RowItem.NewsThreadUiData.CREATOR)

                val comments: MutableList<RowItem.CommentUiData> = mutableListOf()
                source.readTypedList(comments, RowItem.CommentUiData.CREATOR)

                val base: MutableList<RowItem> = mutableListOf(header)
                base.addAll(comments)

                val diff = DiffUtil.calculateDiff(DataDiffCallback(null, base))

                return DataCommentsThread(base, diff)
            }

        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        if (base.isEmpty()) {
            dest.writeTypedObject(null, 0)
            dest.writeTypedList<RowItem>(null)
            return
        }
        dest.writeTypedObject(base[0], 0)
        dest.writeTypedList(base.subList(1, base.size))
    }

    override fun describeContents(): Int = 0
}
