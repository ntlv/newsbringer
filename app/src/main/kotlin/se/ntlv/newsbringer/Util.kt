package se.ntlv.newsbringer

import android.os.Bundle


@Suppress("UNCHECKED_CAST")
inline fun <reified T> Bundle?.getParceledArray(tag: String): Array<T> =
        this?.getParcelableArray(tag) as? Array<T> ?: emptyArray()