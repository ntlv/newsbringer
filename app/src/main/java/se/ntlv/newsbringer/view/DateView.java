package se.ntlv.newsbringer.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

public class DateView extends TextView {

	public DateView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	static CharSequence reformat(CharSequence text) {
		try {
			long asLong = Long.parseLong(String.valueOf(text));
			long diff = System.currentTimeMillis() / 1000 - asLong;
			long dividend = (3600 > diff) ? 60 : 3600;
			String suffix = (3600 > diff) ? "min" : "hours";
			return (diff / dividend) + suffix;

		} catch (NumberFormatException e) {
			return text;
		}
	}

	public void setText(CharSequence text, TextView.BufferType type) {
		if (TextUtils.isEmpty(text)) {
			return;
		}
		super.setText(reformat(text), type);
	}
}

