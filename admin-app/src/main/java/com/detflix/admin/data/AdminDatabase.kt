package com.detflix.admin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AdminCodeEntity::class,
        AdminClientEntity::class,
        AdminPaymentEntity::class,
        AdminContentEntity::class,
        AdminLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AdminDatabase : RoomDatabase() {
    abstract fun adminDao(): AdminDao

    companion object {
        @Volatile
        private var INSTANCE: AdminDatabase? = null

        fun getInstance(context: Context): AdminDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AdminDatabase::class.java,
                    "detflix_admin.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
