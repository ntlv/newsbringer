package se.ntlv.newsbringer

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.jetbrains.anko.bundleOf
import se.ntlv.newsbringer.comments.CommentsActivity

class Navigator(val context: Context) {

    fun navigateToItemComments(id: Long?) {
        if (id != null) {
            context.startActivity(CommentsActivity.getIntent(context, id))
        }
    }

    fun goToLink(link: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))

    fun goToShareLink(title: String, link: String) {
        val i = Intent(Intent.ACTION_SEND);
        i.type = "text/plain";
        val args = bundleOf(Pair(Intent.EXTRA_SUBJECT, title), Pair(Intent.EXTRA_TEXT, link))
        i.putExtras(args)
        context.startActivity(Intent.createChooser(i, "Share URL"))


    }
}