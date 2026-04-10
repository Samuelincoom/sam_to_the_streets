package com.samtothestreets.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.samtothestreets.data.dao.CaseDao
import com.samtothestreets.data.dao.GraphQADao
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.data.entity.GraphQA

@Database(entities = [ProjectCase::class, GraphQA::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun caseDao(): CaseDao
    abstract fun graphQADao(): GraphQADao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sam_streets_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
