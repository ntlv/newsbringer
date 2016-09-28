package se.ntlv.newsbringer

import android.app.Activity
import org.jetbrains.anko.browse
import org.jetbrains.anko.share

class Navigator(private val mContext : Activity) {

    fun navigateToItemComments(id: Long) {
        val res = goToLink("https://news.ycombinator.com/item?id=$id")
        if (res == false) throw IllegalArgumentException()
    }

    fun goToLink(link: String) = mContext.browse(link)

    fun goToShareLink(title: String, link: String) = mContext.share(link, title)
}
