# Meu Acervo — PDM 12

Aplicativo Android em Kotlin/Jetpack Compose para organizar livros pessoais, com consulta real de ISBN à Open Library, localização, observações e estado de leitura. Não há dados simulados no aplicativo.

## Abrir e executar
Extraia o ZIP e abra a pasta roteiro12 no Android Studio. Aguarde o Gradle Sync. Execute o módulo app em dispositivo Android 7.0 (API 24) ou superior com internet. O Android Studio configura o caminho local do SDK; local.properties não integra a entrega.

Ambiente original: Android Studio 2026.1.3, AGP 9.3.3, Gradle 9.5.0, JBR/JDK 25.0.2, compile/target SDK 37, min SDK 24. Java source/target 11. Plugin Compose 2.2.10 e BOM 2026.02.01; Kotlin integrado ao AGP. Dependências adicionadas: Retrofit 3.0.0, OkHttp 4.12.0, Room 2.8.4, Hilt 2.59.2 e KSP 2.3.6. Os resultados efetivamente verificados estão em VALIDACAO.md.

## Requisitos e arquitetura
- Retrofit/OkHttp: data/AcervoData.kt e di/AcervoModule.kt, cliente HTTPS com timeout.
- Cadastro por API: Consultar ISBN preenche o formulário; só depois da resposta é possível salvar. Revise dados e complete campos ausentes. Exemplo: 9780140328721 (Fantastic Mr. Fox).
- Arquitetura Limpa: domain/Acervo.kt contém modelos, contratos e casos de uso sem imports Android/Room/Retrofit. data implementa os contratos com DTOs, mapeamentos e Room. presentation contém telas, navegação e ViewModel. di conecta os componentes usando Hilt.
- Navegação: acervo, cadastro, detalhes, edição e excluídos.
- Edição: título, autores, editora, publicação, localização, observações e leitura. ISBN é imutável após salvar.
- Soft delete: deletedAt recebe timestamp; os dados e o ID permanecem no Room. Lista ativa filtra deletedAt nulo. Excluídos permite restaurar, limpando o campo. Não existe exclusão física.
- Um registro por ISBN, inclusive nos excluídos. Para reutilizá-lo, restaure o registro anterior.

## API
https://openlibrary.org/isbn/{isbn}.json retorna a edição (pode redirecionar). Autores são resolvidos em https://openlibrary.org/authors/{id}.json. A data de publicação é texto livre. Metadados ausentes são editáveis. Limite local conservador: intervalo de 1,1 segundo por chamada; sem busca automática a cada tecla. Não requer chave. A cobertura de livros brasileiros varia.
Documentação: https://openlibrary.org/dev/docs/api/books e https://openlibrary.org/developers/api
ISBN inválido é barrado antes da rede; 404, 429, falha de conexão e persistência têm mensagens. Autores são opcionais, até cinco resolvidos por consulta. Não há cadastro manual sem consulta bem-sucedida. O gerenciamento local funciona offline após cadastro.

## Verificação
No terminal: gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
Testes: validação ISBN-10/13, normalização, bloqueio de consulta inválida, exigência de origem da API e regras de exclusão/restauração com repositório em memória. Estes testes de domínio não substituem teste de integração do Room.

Roteiro prático: cadastre pelo ISBN acima, complete localização, salve, abra detalhes, edite, reinicie, exclua e restaure. Teste ISBN incorreto, livro não encontrado e rede desligada. O formulário permanece no ViewModel durante rotação; não há recuperação de rascunho após encerramento do processo.

O projeto anterior pdm_roteiro11 não pôde ser consultado pelo acesso web desta sessão. Nenhum código dele foi copiado.
