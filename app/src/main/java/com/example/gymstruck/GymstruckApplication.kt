package com.example.gymstruck

import android.app.Application
import com.example.gymstruck.di.AppContainer

class GymstruckApplication : Application() {
    val container by lazy { AppContainer.getInstance(this) }
}
