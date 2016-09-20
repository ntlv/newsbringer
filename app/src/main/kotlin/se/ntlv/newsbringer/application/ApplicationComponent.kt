package se.ntlv.newsbringer.application

import android.app.Application
import dagger.Component
import okhttp3.OkHttpClient
import se.ntlv.newsbringer.comments.CommentsActivity
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.network.IoService
import se.ntlv.newsbringer.newsthreads.NewsThreadsActivity
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {

    companion object {
        fun init(module: ApplicationModule): ApplicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(module)
                .build()
    }

    fun database(): Database

    fun application(): Application

    fun httpClient(): OkHttpClient

    fun inject(service: IoService)

    fun inject(service: NewsThreadsActivity)

    fun inject(commentsActivity: CommentsActivity)

    @Named(IO_POOL)
    fun ioPool(): ThreadPoolExecutor
}
