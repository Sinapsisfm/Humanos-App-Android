package eco.humanos.android.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.core.database.DatabaseConstants
import eco.humanos.android.core.database.HumanosDatabase
import eco.humanos.android.core.database.dao.CaptureDao
import eco.humanos.android.core.database.dao.TaskDao
import eco.humanos.android.core.database.dao.TraceEventDao
import javax.inject.Singleton

/**
 * Hilt bindings for the Room database and its DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HumanosDatabase =
        Room.databaseBuilder(context, HumanosDatabase::class.java, DatabaseConstants.DATABASE_NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideCaptureDao(db: HumanosDatabase): CaptureDao = db.captureDao()

    @Provides
    fun provideTaskDao(db: HumanosDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideTraceEventDao(db: HumanosDatabase): TraceEventDao = db.traceEventDao()
}
