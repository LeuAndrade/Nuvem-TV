# BXbox Cloud TV — 0.4.0

Cliente Android TV de Xbox Cloud Gaming, com interface Java Android própria. Requer Android 7.0/API 24 ou superior. Nome anterior: Nuvem TV. O applicationId, as preferências e a chave do cofre foram preservados para permitir atualizar o aplicativo e manter o login.

## Interface 0.4.0

Início com fileiras de populares em nuvem, adicionados recentemente, saindo em breve e gêneros; atualização automática das listas oficiais. Microfone opcional com permissão Android e envio de voz nos dois motores. Consulta de licença individual nos detalhes, distinguindo compra de acesso por assinatura. Veja `docs/ALTERACOES_0.4.md` para comportamento e limitações.

## Interface preservada da 0.3

A interface foi redesenhada a partir da referência visual fornecida pelo usuário: menu lateral fixo, fundo escuro, acento verde, jogo em destaque no Início e cartões com capas. Biblioteca, pesquisa, histórico, favoritos, detalhes, configurações, ajuda e diálogos usam a mesma linguagem.

- Client ID removido apenas da interface. O valor interno e o valor já salvo são preservados; o fluxo de login existente é mantido, com uma consulta adicional de autorização somente para verificar licenças.
- A grade desenha um contorno verde acima do cartão que tem foco, sem depender do estado de seleção visual da célula reciclada.
- Ordenação e gênero usam diálogos com opções focáveis. Ao fechar, o foco volta ao filtro; ↓ entra na grade, ↑ da primeira linha retorna ao filtro e ← da primeira coluna retorna ao menu lateral.
- Configurações organizadas em Streaming, Conexão e Conta e aplicativo. Todas as opções anteriores foram preservadas, exceto o editor público de Client ID solicitado para remoção.
- Capas de até 320×180; o destaque e os detalhes usam até 640×360 dentro do mesmo orçamento de cache. Não há carrossel automático, vídeo de fundo ou animações de escala.

## Recursos anteriores preservados

- Biblioteca em grade com capas e nomes; Início com fileiras por categoria.
- Busca por nome sem diferenciar acentos, ordenação A–Z/Z–A, filtro por gênero e aba Favoritos.
- Volte a jogar mantém a ordem recente retornada pelo serviço, com opção de ordenar por nome.
- Detalhes com capa, nome, categorias e editora, além de Jogar, Voltar ao início e Adicionar/remover favorito.
- Restauração automática da sessão salva na abertura, renovação silenciosa de tokens e atualização do catálogo.
- Não abre outro login se uma credencial já está salva. Falhas temporárias de rede preservam a conta; uma revogação confirmada de refresh token desconecta a sessão.
- Configurações com Sair da conta. A ação remove a credencial, a biblioteca e os favoritos locais dessa conta.
- Removido o botão e todo o caminho do site Xbox.com/play. O player WebView local continua disponível nas configurações.
- Ícone, banner e nome de exibição atualizados para BXbox Cloud TV.

## Otimização da biblioteca

A grade usa GridView nativo com reciclagem de células e páginas de até 60 títulos. Só as capas requisitadas por células visíveis entram no carregador. Miniaturas de até 320×180 pixels, no máximo duas transferências simultâneas, fila limitada a 20 tarefas, cache de bitmaps de 3 MiB e cache de arquivos de 20 MiB. Uma capa indisponível mantém o nome e o acesso ao jogo.

O catálogo salva somente metadados, limitados a 3 MiB, para reabrir a biblioteca rapidamente. A busca espera 160 ms após a digitação para evitar reconstruções excessivas.

Ao iniciar uma partida, o carregador de capas é suspenso, as conexões de imagem são desconectadas, a fila é descartada, o cache de bitmaps é esvaziado e as referências à grade são liberadas. Não há animação de fundo, vídeo de destaque, carrossel automático ou pré-carregamento do catálogo inteiro em bitmaps.

## Streaming preservado

Wire.java, Pad.java e LowLatencyDecoderFactory.java permanecem idênticos à versão 0.3. Os players receberam somente o caminho opcional de voz e seus controles de permissão, falha e liberação. Não houve alteração na seleção do decoder nem no perfil de vídeo.

Continuam disponíveis: Native/WebView, estatísticas opcionais, resolução de 720p/1080p, pedido de bitrate entre 3 e 15 Mb/s, regiões disponíveis na conta, seleção de codec e solicitação de baixa latência quando suportada.

O perfil inicial continua Native / H264 / 720p / 5 Mb/s. Native permite somente decoder por hardware. O servidor pode não aceitar o codec, a resolução ou o bitrate pedidos. Baixa latência depende do hardware, driver e Android 11+. Não há promessa de melhora em qualquer aparelho de 1 GB.

## Login

O refresh token fica cifrado no Android Keystore. No primeiro login, o app exibe um código para autorizar pelo celular. Nas aberturas seguintes, usa a credencial existente e renova os tokens silenciosamente. Se a Microsoft revogar a autorização, o app precisará de um novo login. Não é possível garantir uma sessão válida para sempre.

O Client ID continua no código de autenticação. Não é uma senha nem uma autorização própria do projeto; a aceitação depende da Microsoft. Ele não é exibido nem editável nas configurações. O identificador já usado pela sessão salva é mantido para não desconectar a conta por essa mudança de interface.

## Atualizar e compilar

Guia: `docs/GUIA_GITHUB.md`. Validação: `docs/VALIDACAO.md`. Alterações: `docs/ALTERACOES_0.4.md`.

```bash
node tools/test-player.cjs
bash gradlew :app:testLiteUnitTest :app:lintLite :app:assembleLite
```

JDK 17 e Android SDK 35. O teste de interface usa Robolectric e baixa dependências de teste na primeira execução; elas não entram no APK.

APKs em `app/build/outputs/apk/lite/`, um para armeabi-v7a e outro para arm64-v8a. O identificador Android e os dados de sessão foram preservados. Uma atualização por cima exige a mesma chave de assinatura do APK instalado. A assinatura de desenvolvimento desta geração não é necessariamente a mesma de APKs anteriores ou de APKs gerados no GitHub. Guarde sua chave de assinatura fora do repositório público. Se houver conflito de assinatura, desinstalar apaga login e preferências locais; gerar com a chave original é a forma de preservar esses dados.

## Limitações

Interfaces não públicas de Xbox podem mudar. Catálogo, histórico, entitlement e fila dependem da conta, plano, compras e região. O feedback de que o streaming anterior funciona veio do usuário; não é uma medição de laboratório deste projeto. A nova navegação foi verificada por compilação, testes e renderização de interface, sem um teste de desempenho numa TV física.

Microfone opcional para chat de voz compatível com o jogo; sem controle virtual, interface de grupos Xbox ou vibração. Não há site oficial embutido.

Licença AGPL-3.0. Preserve `LICENSE`, `NOTICE.md` e o código correspondente ao compartilhar APKs. Projeto independente, sem afiliação com Microsoft ou Xbox.
