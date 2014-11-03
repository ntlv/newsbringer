package se.ntlv.newsbringer.network

import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

import java.io.UnsupportedEncodingException

import com.android.volley.Response.*
import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import com.android.volley.toolbox.HttpHeaderParser.*
import com.android.volley.toolbox.HttpHeaderParser.parseCharset

public class GsonRequest<T>
/**
 * Make a GET request and return a parsed object from JSON.
 *
 * @param url     URL of the request to make
 * @param clazz   Relevant class object, for Gson's reflection
 * @param headers Map of request headers
 */
(url: String, private val clazz: Class<T>, private val headers: Map<String, String>?, private val listener: Listener<T>, errorListener: ErrorListener) : Request<T>(Request.Method.GET, url, errorListener) {

    private val gson = Gson()

    throws(javaClass<AuthFailureError>())
    override fun getHeaders(): Map<String, String> {
        return headers ?: super.getHeaders()
    }

    override fun deliverResponse(response: T) {
        listener.onResponse(response)
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<T> {
        try {
            val charSet = parseCharset(response.headers)
            val json = String(response.data, charSet)
            val parsedHeaders = parseCacheHeaders(response)
            return success(gson.fromJson<T>(json, clazz), parsedHeaders)
        } catch (e: UnsupportedEncodingException) {
            return error(ParseError(e))
        } catch (e: JsonSyntaxException) {
            return error(ParseError(e))
        }

    }
}
