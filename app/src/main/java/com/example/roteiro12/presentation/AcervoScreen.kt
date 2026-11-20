package com.example.roteiro12.presentation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.roteiro12.domain.Livro

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AcervoScreen(vm: AcervoViewModel) {
 val nav=rememberNavController()
 val livros by vm.livros.collectAsStateWithLifecycle()
 val erro by vm.erro.collectAsStateWithLifecycle()
 val form by vm.form.collectAsStateWithLifecycle()
 var confirmar by remember { mutableStateOf<Livro?>(null) }
 Scaffold(topBar={ TopAppBar(title={Text("Meu acervo")}, navigationIcon={ TextButton(onClick={nav.popBackStack()}){Text("Voltar")} ) }) { pad ->
 Column(Modifier.padding(pad).fillMaxSize()) {
 erro?.let { Text(it,Modifier.padding(16.dp), color=MaterialTheme.colorScheme.error); TextButton(onClick={vm.erro.value=null}){Text("Fechar aviso")} }
 NavHost(nav,startDestination="acervo",modifier=Modifier.weight(1f)) {
  composable("acervo") {
   Lista(livros.filter{it.deletedAt==null},false,{nav.navigate("detalhes/${it.id}")},{vm.iniciar();nav.navigate("form")},{nav.navigate("excluidos")})
  }
  composable("excluidos") {
   Lista(livros.filter{it.deletedAt!=null},true,{nav.navigate("detalhes/${it.id}")},{},{nav.popBackStack()})
  }
  composable("detalhes/{id}") { entry ->
   val l=livros.find{it.id==entry.arguments?.getString("id")?.toLongOrNull()}
   Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    if(l==null) Text("Livro indisponível.") else {
     Text(l.titulo,style=MaterialTheme.typography.headlineMedium)
     Text("ISBN: ${l.isbn}\nAutores: ${l.autores.ifBlank{"Não informado"}}\nEditora: ${l.editora.ifBlank{"Não informada"}}\nPublicação: ${l.publicacao.ifBlank{"Não informada"}}")
     HorizontalDivider(); Text("Localização: ${l.localizacao}"); Text("Leitura: ${l.status}"); Text("Observações: ${l.observacoes.ifBlank{"Nenhuma"}}")
     Text("Fonte: Open Library • ${l.origem}",style=MaterialTheme.typography.bodySmall)
     if(l.deletedAt==null) {
      Button(onClick={vm.iniciar(l);nav.navigate("form")}){Text("Editar livro")}
      OutlinedButton(onClick={confirmar=l}){Text("Excluir do acervo")}
     } else { Text("Este livro foi excluído do acervo ativo."); Button(onClick={vm.arquivar(l.id,true)}){Text("Restaurar livro")} }
    }
   }
  }
  composable("form") {
   LaunchedEffect(form.salvo) { if(form.salvo) nav.popBackStack() }
   Formulario(form,vm::alterar,vm::buscar,vm::gravar)
  }
 }
 }
 }
 confirmar?.let { l -> AlertDialog(onDismissRequest={confirmar=null},title={Text("Excluir livro?")},text={Text("${l.titulo} irá para Excluídos e poderá ser restaurado.")},confirmButton={TextButton(onClick={vm.arquivar(l.id);confirmar=null}){Text("Excluir")}},dismissButton={TextButton(onClick={confirmar=null}){Text("Cancelar")}}) }
}
@Composable private fun Lista(livros: List<Livro>, excluidos: Boolean, abrir:(Livro)->Unit, novo:()->Unit, alternar:()->Unit) {
 var busca by rememberSaveable { mutableStateOf("") }
 Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 Text(if(excluidos) "Livros excluídos" else "Sua biblioteca pessoal",style=MaterialTheme.typography.headlineSmall)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
 if(!excluidos) Button(onClick=novo){Text("Cadastrar livro")}
 TextButton(onClick=alternar){Text(if(excluidos) "Ver acervo" else "Excluídos")}
 }
 OutlinedTextField(busca,{busca=it},label={Text("Buscar título, autor ou localização")},modifier=Modifier.fillMaxWidth())
 val lista=livros.filter{"${it.titulo} ${it.autores} ${it.localizacao}".contains(busca,true)}
 if(lista.isEmpty()) Text(if(excluidos) "Nenhum livro excluído." else "Nenhum livro encontrado. Cadastre pelo ISBN.")
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) { items(lista,key={it.id}) { l ->
 Card(Modifier.fillMaxWidth().clickable{abrir(l)}) { Column(Modifier.padding(16.dp)) {Text(l.titulo,style=MaterialTheme.typography.titleMedium);Text(l.autores.ifBlank{"Autor não informado"});Text("${l.localizacao} • ${l.status}",style=MaterialTheme.typography.bodySmall)} }
 } }
 }
}
@Composable private fun Formulario(s: FormState, alterar:(Livro)->Unit, buscar:()->Unit, salvar:()->Unit) {
 val l=s.livro
 Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
 Text(if(l.id==0L) "Cadastrar por ISBN" else "Editar livro",style=MaterialTheme.typography.headlineSmall)
 Text("Consulte a Open Library, revise os dados e informe onde o livro está guardado.")
 OutlinedTextField(l.isbn,{alterar(l.copy(isbn=it,origem=""))},enabled=!s.ocupado && l.id==0L,label={Text("ISBN (10 ou 13 caracteres)")},modifier=Modifier.fillMaxWidth())
 Button(onClick=buscar,enabled=!s.ocupado && l.id==0L){Text("Consultar ISBN")}
 if(s.ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
 s.mensagem?.let{Text(it,color=MaterialTheme.colorScheme.primary)}
 Campo("Título *",l.titulo,!s.ocupado){alterar(l.copy(titulo=it))}
 Campo("Autores",l.autores,!s.ocupado){alterar(l.copy(autores=it))}
 Campo("Editora",l.editora,!s.ocupado){alterar(l.copy(editora=it))}
 Campo("Publicação",l.publicacao,!s.ocupado){alterar(l.copy(publicacao=it))}
 Campo("Localização *",l.localizacao,!s.ocupado){alterar(l.copy(localizacao=it))}
 Campo("Observações",l.observacoes,!s.ocupado){alterar(l.copy(observacoes=it))}
 Text("Estado de leitura")
 listOf("Quero ler","Lendo","Concluído").forEach { status -> FilterChip(selected=l.status==status,onClick={alterar(l.copy(status=status))},enabled=!s.ocupado,label={Text(status)}) }
 Button(onClick=salvar,enabled=!s.ocupado && l.origem.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Salvar livro")}
 if(l.origem.isBlank()) Text("Para salvar, consulte um ISBN encontrado na API.")
 }
}
@Composable private fun Campo(rotulo:String,valor:String,habilitado:Boolean,mudar:(String)->Unit) { OutlinedTextField(valor,mudar,enabled=habilitado,label={Text(rotulo)},modifier=Modifier.fillMaxWidth()) }
