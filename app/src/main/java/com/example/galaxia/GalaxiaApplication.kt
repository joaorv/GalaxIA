package com.example.galaxia

import android.app.Application

class GalaxiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GalaxiaApplication
            private set
    }
}
