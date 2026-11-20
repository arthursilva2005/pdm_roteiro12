package com.example.roteiro12.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roteiro12.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FormState(val livro: Livro = Livro(isbn="",titulo=""), val ocupado: Boolean=false, val mensagem: String?=null, val salvo: Boolean=false)
@HiltViewModel class AcervoViewModel @Inject constructor(repo: LivroRepository, catalogo: CatalogoRepository): ViewModel() {
 val erro = MutableStateFlow<String?>(null)
 val livros = ObservarLivros(repo)().catch { erro.value="Não foi possível ler o acervo local." }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
 val form = MutableStateFlow(FormState())
 private val consultar = ConsultarIsbn(catalogo)
 private val salvar = SalvarLivro(repo)
 private val excluir = ExcluirLivro(repo)
 private val restaurar = RestaurarLivro(repo)
 fun iniciar(livro: Livro? = null) { form.value=FormState(livro ?: Livro(isbn="",titulo="")) }
 fun alterar(l: Livro) { form.value=form.value.copy(livro=l,mensagem=null) }
 fun buscar() { val isbn=form.value.livro.isbn
  viewModelScope.launch { form.value=form.value.copy(ocupado=true,mensagem=null)
   try { val encontrado=consultar(isbn); val atual=form.value.livro
    form.value=form.value.copy(livro=encontrado.copy(id=atual.id,localizacao=atual.localizacao,observacoes=atual.observacoes,status=atual.status),mensagem="Dados recebidos da Open Library. Revise e complete os campos ausentes.")
   } catch(e: CancellationException) { throw e } catch(e: Exception) { form.value=form.value.copy(mensagem=e.message ?: "Resposta inválida da API. Tente novamente.") }
   finally { form.value=form.value.copy(ocupado=false) }
  }
 }
 fun gravar() { viewModelScope.launch { form.value=form.value.copy(ocupado=true,mensagem=null)
  try { salvar(form.value.livro); form.value=form.value.copy(salvo=true) }
  catch(e: CancellationException) { throw e } catch(e: Exception) { form.value=form.value.copy(mensagem=e.message ?: "Não foi possível salvar no aparelho.") }
  finally { form.value=form.value.copy(ocupado=false) }
 } }
 fun arquivar(id: Long, restauracao: Boolean=false) { viewModelScope.launch {
  try { if(restauracao) restaurar(id) else excluir(id) }
  catch(e: CancellationException) { throw e } catch(e: Exception) { erro.value="Não foi possível atualizar o livro. Tente novamente." }
 } }
}

