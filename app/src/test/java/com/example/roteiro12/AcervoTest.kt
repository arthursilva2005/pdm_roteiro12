package com.example.roteiro12
import com.example.roteiro12.domain.*
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
class AcervoTest {
 @Test fun isbnValidaDigitos() {
  assertTrue(Isbn.valido("978-0-14-032872-1")); assertTrue(Isbn.valido("0140328726")); assertTrue(Isbn.valido("080442957X"))
  assertFalse(Isbn.valido("9780140328722")); assertFalse(Isbn.valido("123")); assertFalse(Isbn.valido("ABCDEFGHIJ"))
 }
 @Test fun consultaExigeIsbnValido() = runTest {
  var chamadas=0
  val caso=ConsultarIsbn(object: CatalogoRepository { override suspend fun consultar(isbn:String): Livro { chamadas++; return Livro(isbn=isbn,titulo="Real") } })
  try { caso("123"); fail() } catch(e: IllegalArgumentException) { assertEquals(0,chamadas) }
  assertEquals("9780140328721",caso("978-0140328721").isbn); assertEquals(1,chamadas)
 }
 @Test fun cadastroExigeConsultaETituloELocal() = runTest {
  val repo=FakeRepo(); val salvar=SalvarLivro(repo)
  val livro=Livro(isbn="9780140328721",titulo="Livro",localizacao="Estante")
  try { salvar(livro); fail() } catch(e:IllegalArgumentException) { assertTrue(repo.dados.value.isEmpty()) }
  salvar(livro.copy(origem="/books/teste")); assertEquals(1,repo.dados.value.size)
 }
 @Test fun exclusaoPreservaDadosERestauracao() = runTest {
  val repo=FakeRepo(); SalvarLivro(repo)(Livro(isbn="9780140328721",titulo="Livro",localizacao="Sala",origem="api"))
  ExcluirLivro(repo)(1); assertNotNull(repo.dados.value.single().deletedAt)
  RestaurarLivro(repo)(1); assertNull(repo.dados.value.single().deletedAt); assertEquals("Sala",repo.dados.value.single().localizacao)
 }
 private class FakeRepo: LivroRepository {
  val dados=MutableStateFlow<List<Livro>>(emptyList())
  override fun observar()=dados
  override suspend fun salvar(livro:Livro):Long { dados.value=listOf(livro.copy(id=1));return 1 }
  override suspend fun excluir(id:Long){dados.value=dados.value.map{it.copy(deletedAt=123)}}
  override suspend fun restaurar(id:Long){dados.value=dados.value.map{it.copy(deletedAt=null)}}
 }
}
