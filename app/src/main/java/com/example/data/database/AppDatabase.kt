package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CustomTrigger
import com.example.data.model.TextTransformHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CustomTrigger::class, TextTransformHistory::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun triggerDao(): TriggerDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teclado_inteligente_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate with useful triggers
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).triggerDao()
                                dao.insertTrigger(
                                    CustomTrigger(
                                        name = "Tom Profissional",
                                        prompt = "Reescreva este texto em tom executivo, polido e corporativo, mantendo a mensagem direta e cordial.",
                                        iconSymbol = "💼",
                                        iconKey = "work",
                                        sortOrder = 0
                                    )
                                )
                                dao.insertTrigger(
                                    CustomTrigger(
                                        name = "Mensagem Concisa",
                                        prompt = "Elimine redundâncias e torne a mensagem extremamente direta e rápida de ler sem perder nenhuma informação essencial.",
                                        iconSymbol = "⚡",
                                        iconKey = "bolt",
                                        sortOrder = 1
                                    )
                                )
                                dao.insertTrigger(
                                    CustomTrigger(
                                        name = "Empático & Caloroso",
                                        prompt = "Reescreva com simpatia, empatia e calor humano, ideal para conversas amigáveis ou suporte acolhedor.",
                                        iconSymbol = "❤️",
                                        iconKey = "favorite",
                                        sortOrder = 2
                                    )
                                )
                                dao.insertTrigger(
                                    CustomTrigger(
                                        name = "Argumento Convincente",
                                        prompt = "Torne o argumento mais persuasive, bem estruturado e com impacto convincente.",
                                        iconSymbol = "🎯",
                                        iconKey = "campaign",
                                        sortOrder = 3
                                    )
                                )
                            }
                        }
                    }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
