package se.ntlv.newsbringer

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

class DateView: TextView {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0)
    constructor(context: Context) : super(context)

    override fun setText(text: CharSequence?, type: TextView.BufferType?) {
        if ( isInEditMode) {
            super.setText(text, type)
            return
        }
        val reformattedText = text?.reformat()
        super.setText(reformattedText ?: text, type)
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

    enum class TimeUnit(val dividend: Long, val singular: Int, val plural: Int) {
        SECOND(1L, R.string.second, R.string.seconds),
        MINUTE(60L, R.string.minute, R.string.minutes),
        HOUR(3600L, R.string.hour, R.string.hours),
        DAY(86400L, R.string.day, R.string.days)
    }

    fun getString(id : Int) : String = context.getString(id)

    fun formatTime(diff: Long, unit: TimeUnit): CharSequence {
        val time = diff.div(unit.dividend)
        return when (time) {
            1L -> "1 ${getString(unit.singular)}"
            else -> "$time ${getString(unit.plural)}"
        }
    }

}
