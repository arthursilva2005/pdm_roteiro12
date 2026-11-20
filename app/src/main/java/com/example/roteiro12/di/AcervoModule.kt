package com.example.roteiro12.di
import android.content.Context
import androidx.room.Room
import com.example.roteiro12.data.*
import com.example.roteiro12.domain.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
@Module @InstallIn(SingletonComponent::class)
object AcervoModule {
 @Provides @Singleton fun database(@ApplicationContext context: Context) = Room.databaseBuilder(context, AcervoDatabase::class.java,"acervo.db").build()
 @Provides @Singleton fun livros(db: AcervoDatabase): LivroRepository = RoomLivroRepository(db.livros())
 @Provides @Singleton fun api(): OpenLibraryApi = Retrofit.Builder().baseUrl("https://openlibrary.org/").client(OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent","AcervoPessoalPDM12/1.0 (educational Android application)").build()) }.build()).addConverterFactory(GsonConverterFactory.create()).build().create(OpenLibraryApi::class.java)
 @Provides @Singleton fun catalogo(api: OpenLibraryApi): CatalogoRepository = OpenLibraryRepository(api)
}
