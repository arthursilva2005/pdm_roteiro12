package com.example.roteiro12.domain

import kotlinx.coroutines.flow.Flow

data class Livro(val id: Long = 0, val isbn: String, val titulo: String, val autores: String = "", val editora: String = "", val publicacao: String = "", val localizacao: String = "", val observacoes: String = "", val status: String = "Quero ler", val origem: String = "", val deletedAt: Long? = null)
object Isbn {
 fun normalizar(s: String) = s.replace("-", "").replace(" ", "").uppercase()
 fun valido(raw: String): Boolean {
  val s = normalizar(raw)
  return when {
   s.matches(Regex("[0-9]{13}")) -> s.mapIndexed { i, c -> (c - '0') * if(i % 2 == 0) 1 else 3 }.sum() % 10 == 0
   s.matches(Regex("[0-9]{9}[0-9X]")) -> s.mapIndexed { i,c -> (if(c == 'X') 10 else c-'0') * (10-i) }.sum() % 11 == 0
   else -> false
  }
 }
}
interface LivroRepository {
 fun observar(): Flow<List<Livro>>
 suspend fun salvar(livro: Livro): Long
 suspend fun excluir(id: Long)
 suspend fun restaurar(id: Long)
}
interface CatalogoRepository { suspend fun consultar(isbn: String): Livro }
class ConsultarIsbn(private val repo: CatalogoRepository) {
 suspend operator fun invoke(isbn: String): Livro {
  require(Isbn.valido(isbn)) { "ISBN inválido. Informe 10 ou 13 caracteres e confira os dígitos." }
  return repo.consultar(Isbn.normalizar(isbn))
 }
}
class SalvarLivro(private val repo: LivroRepository) {
 suspend operator fun invoke(l: Livro): Long {
  require(l.titulo.isNotBlank()) { "Informe o título." }
  require(l.localizacao.isNotBlank()) { "Informe a localização do livro." }
  require(Isbn.valido(l.isbn)) { "ISBN inválido." }
  require(l.origem.isNotBlank()) { "Consulte o ISBN antes de cadastrar." }
  return repo.salvar(l.copy(titulo=l.titulo.trim(), localizacao=l.localizacao.trim()))
 }
}
class ObservarLivros(private val repo: LivroRepository) { operator fun invoke() = repo.observar() }
class ExcluirLivro(private val repo: LivroRepository) { suspend operator fun invoke(id: Long) = repo.excluir(id) }
class RestaurarLivro(private val repo: LivroRepository) { suspend operator fun invoke(id: Long) = repo.restaurar(id) }
