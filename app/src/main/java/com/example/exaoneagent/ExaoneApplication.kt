package com.example.exaoneagent

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class ExaoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 시스템 설정과 무관하게 항상 라이트 모드로 고정
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}