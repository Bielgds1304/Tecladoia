package com.example

import android.app.Application
import com.example.ai.offline.TFLiteModelManager
import com.example.ai.offline.TFLiteTextProcessor
import com.example.api.GeminiRepository
import com.example.data.PreferencesManager
import com.example.data.database.AppDatabase

class SmartKeyboardApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var tfliteModelManager: TFLiteModelManager
        private set

    lateinit var tfliteTextProcessor: TFLiteTextProcessor
        private set

    lateinit var geminiRepository: GeminiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        tfliteModelManager = TFLiteModelManager(this)
        tfliteTextProcessor = TFLiteTextProcessor(this, tfliteModelManager)
        geminiRepository = GeminiRepository(preferencesManager, tfliteTextProcessor)
    }

    companion object {
        lateinit var instance: SmartKeyboardApp
            private set
    }
}
