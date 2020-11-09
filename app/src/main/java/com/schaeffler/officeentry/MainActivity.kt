package com.schaeffler.officeentry

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.schaeffler.officeentry.utils.isNightMode

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isNightMode) {

        }

        setContentView(R.layout.activity_main)
    }
}