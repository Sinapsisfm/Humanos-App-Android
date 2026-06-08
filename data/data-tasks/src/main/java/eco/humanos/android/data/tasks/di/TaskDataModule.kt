package eco.humanos.android.data.tasks.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.data.tasks.repository.TaskRepository
import eco.humanos.android.data.tasks.repository.TaskRepositoryImpl
import javax.inject.Singleton

/**
 * Hilt bindings for the tasks data layer.
 *
 * Binds [TaskRepositoryImpl] (which depends on the DAO from core-database,
 * the HumanOS gateway from integration-humanos, and the trace repository
 * from core-observability) to the [TaskRepository] contract consumed by the
 * feature layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TaskDataModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
