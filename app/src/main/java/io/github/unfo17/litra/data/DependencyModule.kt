package io.github.unfo17.litra.data
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object DependencyModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): LitraDatabase = Room.databaseBuilder(context, LitraDatabase::class.java, "litra.db").build()
}
