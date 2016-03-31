package se.ntlv.newsbringer

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric


class YcReaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
//        Firebase.setAndroidContext(this)
        if (BuildConfig.DEBUG.not()) {
            Fabric.with(this, Crashlytics());
        }
    }
}

fun crashlyticsLogIfPossible(priority : Int, tag : String, message : String) {
    if (BuildConfig.DEBUG.not()) {
        Crashlytics.log(priority, tag, message)
    }
}