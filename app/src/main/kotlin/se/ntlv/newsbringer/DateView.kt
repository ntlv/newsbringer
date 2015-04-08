package se.ntlv.newsbringer

import android.widget.TextView
import android.content.Context
import android.util.AttributeSet
import kotlin.template.LocaleFormatter
import android.util.Log
import android.support.v4.util.TimeUtils
import android.view.View
import java.util.concurrent.TimeUnit

class DateView: TextView {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0)
    constructor(context: Context) : super(context)

    override fun setText(text: CharSequence?, type: TextView.BufferType?) {
        if ( isInEditMode()) {
            super.setText(text, type)
            return
        }
        val reformattedText = text?.reformat()
        super<TextView>.setText(reformattedText ?: text, type)
    }
    fun CharSequence.reformat(): CharSequence {
        try {
            val asLong = this.toString().toLong()
            val diff = System.currentTimeMillis().div(1000).minus(asLong)
            return when {
                diff < 60L -> formatTime(diff, TimeUnit.SECOND)
                diff < 3600L -> formatTime(diff, TimeUnit.MINUTE)
                diff < 86400L -> formatTime(diff, TimeUnit.HOUR)
                else -> formatTime(diff, TimeUnit.DAY)
            }
        } catch (formatException: NumberFormatException) {
            return this;
        }
    }

    enum class TimeUnit(dividend: Long, singular: Int, plural: Int) {
        val dividend = dividend
        val singular = singular
        val plural = plural

        SECOND : TimeUnit(1L, R.string.second,  R.string.seconds)
        MINUTE : TimeUnit(60L, R.string.minute, R.string.minutes)
        HOUR   : TimeUnit(3600L, R.string.hour, R.string.hours)
        DAY    : TimeUnit(86400L, R.string.day, R.string.days)
    }

    fun getString(id : Int) : String = getContext().getString(id)

    fun formatTime(diff: Long, unit: TimeUnit): CharSequence {
        val time = diff.div(unit.dividend)
        return when (time) {
            1L -> "1 ${getString(unit.singular)}"
            else -> "$time ${getString(unit.plural)}"
        }
    }

}
