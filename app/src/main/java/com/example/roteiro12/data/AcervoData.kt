package com.example.roteiro12.data

import androidx.room.*
import com.example.roteiro12.domain.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.HttpException
import java.io.IOException

@Entity(tableName="livros", indices=[Index(value=["isbn"], unique=true)])
data class LivroEntity(@PrimaryKey(autoGenerate=true) val id: Long=0, val isbn: String, val titulo: String, val autores: String, val editora: String, val publicacao: String, val localizacao: String, val observacoes: String, val status: String, val origem: String, val deletedAt: Long?)
fun LivroEntity.domain() = Livro(id,isbn,titulo,autores,editora,publicacao,localizacao,observacoes,status,origem,deletedAt)
fun Livro.entity() = LivroEntity(id,isbn,titulo,autores,editora,publicacao,localizacao,observacoes,status,origem,deletedAt)
@Dao interface LivroDao {
 @Query("SELECT * FROM livros ORDER BY titulo COLLATE NOCASE") fun observar(): kotlinx.coroutines.flow.Flow<List<LivroEntity>>
 @Insert suspend fun inserir(l: LivroEntity): Long
 @Update suspend fun atualizar(l: LivroEntity): Int
 @Query("UPDATE livros SET deletedAt = :data WHERE id = :id") suspend fun arquivar(id: Long, data: Long?)
}
@Database(entities=[LivroEntity::class], version=1, exportSchema=false)
abstract class AcervoDatabase: RoomDatabase() { abstract fun livros(): LivroDao }
class RoomLivroRepository(private val dao: LivroDao): LivroRepository {
 override fun observar() = dao.observar().map { lista -> lista.map { it.domain() } }
 override suspend fun salvar(livro: Livro): Long = try {
  if(livro.id == 0L) dao.inserir(livro.entity()) else { check(dao.atualizar(livro.entity()) == 1); livro.id }
 } catch(e: android.database.sqlite.SQLiteConstraintException) { throw IllegalStateException("Este ISBN já está no acervo ou nos excluídos. Abra ou restaure o registro.", e) }
 override suspend fun excluir(id: Long) = dao.arquivar(id, System.currentTimeMillis())
 override suspend fun restaurar(id: Long) = dao.arquivar(id, null)
}
data class ReferenciaDto(val key: String?)
data class EdicaoDto(val key: String?, val title: String?, val authors: List<ReferenciaDto>?, val publishers: List<String>?, val publish_date: String?)
data class AutorDto(val name: String?)
interface OpenLibraryApi {
 @GET("isbn/{isbn}.json") suspend fun edicao(@Path("isbn") isbn: String): EdicaoDto
 @GET("authors/{id}.json") suspend fun autor(@Path("id") id: String): AutorDto
}
class OpenLibraryRepository(private val api: OpenLibraryApi): CatalogoRepository {
 private val mutex = Mutex()
 private var ultima = 0L
 private suspend fun <T> limitada(block: suspend () -> T): T = mutex.withLock {
  delay((1100 - (System.currentTimeMillis()-ultima)).coerceAtLeast(0))
  ultima = System.currentTimeMillis(); block()
 }
 override suspend fun consultar(isbn: String): Livro = try {
  val d = limitada { api.edicao(isbn) }
  val nomes = mutableListOf<String>()
  for(ref in d.authors.orEmpty().take(5)) {
   val id = ref.key?.substringAfterLast('/') ?: continue
   try { limitada { api.autor(id) }.name?.let { nomes.add(it) } }
   catch(e: IOException) { /* Metadado opcional; usuário pode completar. */ }
   catch(e: HttpException) { /* Metadado opcional. */ }
  }
  Livro(isbn=isbn, titulo=d.title.orEmpty(), autores=nomes.joinToString(", "), editora=d.publishers.orEmpty().joinToString(", "), publicacao=d.publish_date.orEmpty(), origem=d.key ?: "Open Library ISBN:$isbn")
 } catch(e: HttpException) {
  throw IllegalStateException(when(e.code()) { 404 -> "Livro não encontrado na Open Library. Confira o ISBN."; 429 -> "Limite da API atingido. Aguarde e tente novamente."; else -> "A Open Library está indisponível (${e.code()}). Tente novamente." }, e)
 } catch(e: IOException) { throw IllegalStateException("Não foi possível conectar. Confira a internet e tente novamente.", e) }
}
