package se.ntlv.newsbringer

import android.content.Context
import org.jetbrains.anko.browse
import org.jetbrains.anko.share

class Navigator(val context: Context) {

    fun navigateToItemComments(id: Long) {
        val res = goToLink("https://news.ycombinator.com/item?id=$id")
        if (res == false) throw IllegalArgumentException()
    }

    fun goToLink(link: String) = context.browse(link)

    fun goToShareLink(title: String, link: String) = context.share(link, title)
}
