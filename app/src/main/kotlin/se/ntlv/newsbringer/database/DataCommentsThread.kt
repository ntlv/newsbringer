package se.ntlv.newsbringer.database

import android.os.Parcel
import android.os.Parcelable
import se.ntlv.newsbringer.network.RowItem
import se.ntlv.newsbringer.newsthreads.DataDiffCallback

class DataCommentsThread(base: List<RowItem>, diff: DiffUtil.DiffResult) : Data<RowItem>(base, diff), Parcelable {

    companion object : AnkoLogger{

        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<DataCommentsThread> {
            override fun newArray(size: Int): Array<DataCommentsThread?> = arrayOfNulls(size)

            override fun createFromParcel(source: Parcel): DataCommentsThread {
                info("Creating from parcel")
                val header = source.readTypedObject(RowItem.NewsThreadUiData.CREATOR)

                val comments: MutableList<RowItem.CommentUiData> = mutableListOf()
                source.readTypedList(comments, RowItem.CommentUiData.CREATOR)

                val base : MutableList<RowItem> = mutableListOf(header)
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