package se.ntlv.newsbringer.database

import android.os.Parcel
import android.os.Parcelable
import android.support.v7.util.DiffUtil
import se.ntlv.newsbringer.customviews.DataDiffCallback
import se.ntlv.newsbringer.network.NewsThreadUiData

class DataFrontPage(internal val base: List<NewsThreadUiData>,
                    override val diff: DiffUtil.DiffResult) : AdapterModelCollection<NewsThreadUiData>, List<NewsThreadUiData> by base {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<DataFrontPage> {
            override fun newArray(size: Int) = arrayOfNulls<DataFrontPage>(size)

            override fun createFromParcel(source: Parcel): DataFrontPage {
                val base = mutableListOf<NewsThreadUiData>()
                source.readTypedList(base, NewsThreadUiData.CREATOR)
                val diff = DiffUtil.calculateDiff(DataDiffCallback(null, base))
                return DataFrontPage(base, diff)
            }
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) = dest.writeTypedList(base)
}
