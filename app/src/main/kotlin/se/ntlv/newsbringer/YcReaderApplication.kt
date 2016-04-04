package se.ntlv.newsbringer

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose


class YcReaderApplication : Application(), AnkoLogger {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG.not()) {
            Fabric.with(this, Crashlytics());
        }
        verbose("CREATED")
    }
}