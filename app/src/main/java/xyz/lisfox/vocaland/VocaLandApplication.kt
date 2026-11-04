package xyz.lisfox.vocaland

import android.app.Application
import xyz.lisfox.vocaland.di.AppContainer

class VocaLandApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
