# BXbox Cloud TV 0.5.0

Esta entrega modifica o aplicativo real. Ela NÃO conclui um cliente nativo de grupos de voz Xbox: grupo/convite e bloqueio encaminham explicitamente ao Xbox oficial. Pesquisa, amigos, sugestões, perfil, seguir, pedido de amizade e mensagens têm chamadas de serviço implementadas, mas não foram testadas numa conta autenticada. Não há afirmação de equivalência completa com o cliente oficial.

## Correção de voz no jogo

Foi comparado o código com o fluxo público servido pelo próprio Xbox Play, incluindo `web-rtc-stream.d6f6b903.chunk.js`. O cliente oficial negocia a capacidade `chatStream` versão 1 na oferta inicial, começa com áudio `sendrecv`, captura voz mono/16 bits/24 kHz, usa `addTrack(track, stream)` e então renegocia com `isMediaStreamsChatRenegotiation`.

Na versão 0.4 faltava a capacidade `chatStream` na configuração inicial, o áudio começava somente para receber e o envio tentava substituir diretamente a faixa. Esses pontos foram corrigidos nos dois motores. A resposta inicial agora é verificada: sem anúncio de `chatStream`, o app informa falta de suporte, sem fingir que enviou voz. As trocas SDP têm requestId e aguardam uma resposta correspondente/nova, evitando aceitar imediatamente uma resposta anterior na renegociação.

O Native pede entrada mono 24 kHz ao módulo de áudio. O WebView usa as mesmas características como preferências de captura. O decodificador de vídeo, o pacote binário do controle e o perfil leve não foram alterados.

Quando o microfone está ligado, um pequeno indicador mostra os pacotes e bytes de áudio enviados pelo PeerConnection e o nível informado pela fonte. Isso comprova emissão pelo cliente quando os contadores crescem; não comprova que o jogo encaminhou áudio nem que outra pessoa ouviu. O indicador fica separado das estatísticas de vídeo e só consulta áudio enquanto a opção de microfone está ligada.

Configurações > Streaming > Testar microfone abre um teste local de cerca de dois segundos, após a permissão Android. Ele lê amostras PCM e informa entrada, amostras e nível. Não grava arquivos nem envia áudio. Serve para distinguir captura indisponível/silenciosa de problemas de negociação/transmissão. O chat de voz precisa estar liberado no jogo e na conta. Um microfone de controle remoto pode existir apenas para a busca da TV.

Não houve uma TV física, microfone físico, conta Xbox autenticada ou interlocutor para provar áudio ponta a ponta. A causa de protocolo foi corrigida; funcionamento na instalação do usuário ainda precisa ser confirmado. Se falhar, envie o resultado de Testar microfone e o texto do indicador Voz durante a partida, além de modelo da TV/Box e tipo de conexão do microfone.

## Início

- Destaques promocionais atuais do Xbox Play, extraídos do estado público `heroData` do site oficial no idioma selecionado, com ordem, título, subtítulo e imagem da campanha. Navegação manual entre campanhas, sem reprodução de vídeo e sem temporizador permanente.
- Uma promoção pode ser uma pré-venda ou um título ainda sem suporte à nuvem. Nesses casos o app abre detalhes/loja, sem inventar um botão de transmissão funcional.
- Volte a jogar virou fileira horizontal acima de populares e saiu do menu lateral. A ordem vem do histórico autenticado de xCloud.
- Adicionadas fileiras Jogar com o Game Pass e Transmita seu próprio jogo usando as listas oficiais, mantendo populares, adicionados e saindo.
- Os gêneros permanecem como cartões. Após eles existe o botão Todos os jogos, que abre a grade completa com busca, ordenação e gênero.
- Listas e promoções renovam na abertura, por Atualizar e no prazo de seis horas com catálogo aberto. Uma falha preserva o último resultado. Sem atualizações de catálogo durante streaming.
- Cada fileira mantém até quatro células reutilizáveis, com largura estável mesmo quando existe só um jogo. Só as capas das fileiras visíveis são vinculadas; imagens de fileiras fora da tela são removidas das views. Cache segue 3 MiB em RAM/20 MiB em disco.

## Meus jogos

O item Biblioteca virou Meus jogos. Ao abrir/atualizar, consulta as licenças válidas da conta em Collections, com paginação e assinatura XSTS, e hidrata os produtos no catálogo Microsoft. Compras/resgates Single válidos são separados de assinatura Recurring, testes, revogadas e expiradas. Jogos concedidos por um pacote entram quando o pacote também tem uma compra válida identificada.

Não usa o histórico, favoritos, presença no catálogo ou entitlement de Game Pass para inventar posse. Pode listar jogos adquiridos sem suporte a nuvem, mas mostra essa limitação e abre a loja em vez de iniciar uma sessão inválida. Compras em outras lojas, discos e compartilhamento podem não ser retornados por esse serviço. A Microsoft pode recusar o endpoint a um cliente independente; nesse caso o app preserva o cache e mostra a falha, sem substituir compras por todo o catálogo.

A consulta usa uma fila separada da abertura de streaming, cache de metadados privado do aplicativo (sem tokens no arquivo de catálogo) e remove o cache no logout. Não é uma certificação de acesso pela Microsoft.

## Jogar com amigos

- Pesquisa por gamertag via PeopleHub, lista de amigos e sugestões do Xbox, com resultados reais do serviço; não há perfis de demonstração no APK.
- Perfil com presença e biografia quando retornadas. Seguir e pedido de amizade são operações diferentes.
- Mensagens consultam a conversa real e enviam texto ao XUID escolhido. Antes de enviar, há confirmação do gamertag e conteúdo; erros HTTP não viram mensagens locais de sucesso. Uma confirmação de aceitação do serviço não significa confirmação de leitura/entrega.
- Controles de família, privacidade, autorização, versões de API e limites podem impedir operações. O app mostra a recusa em vez de exibir sucesso fictício.
- Convidar para novo grupo NÃO cria grupo nem ativa microfone local. Exibe essa limitação e oferece abrir o Xbox oficial no navegador para usar o serviço original. Não há protocolo de grupo/áudio entre pessoas implementado.
- Bloquear no Xbox oficial abre o perfil oficial; o bloqueio precisa ser concluído ali. Não foi implementado um bloqueio nativo da conta.
- O navegador pode pedir seu próprio login. Isso é separado da sessão persistente do aplicativo.

## Idiomas

Configurações > Streaming > Idioma: Idioma do sistema (padrão), Português, English e Español. Português e espanhol seguem o idioma correspondente do sistema; demais idiomas usam inglês quando a seleção é automática. A opção manual prevalece. As telas principais, botões e diálogos têm tradução local, sem biblioteca ou serviço de tradução. Texto dos jogos, biografias, mensagens de pessoas, retorno de API e diagnósticos técnicos podem conservar o idioma original. O idioma de sessão e a hidratação dos metadados usam a escolha; os metadados mudam na próxima atualização do catálogo.

## Referências de implementação

- [Xbox Play oficial](https://www.xbox.com/pt-BR/play), estado público de promoções e pacote WebRTC servido por assets.play.xbox.com.
- [SIGL de Game Pass](https://catalog.gamepass.com/sigls/v2?id=af206485-e87d-4624-9007-cb7f6d0cc42e&market=BR&language=pt-BR) e [transmita seu próprio jogo](https://catalog.gamepass.com/sigls/v2?id=e4c1d680-2c70-45e4-a38d-8a292c68c700&market=BR&language=pt-BR).
- [OpenXbox: PeopleHub](https://github.com/OpenXbox/xbox-webapi-python/blob/master/xbox/webapi/api/provider/people/__init__.py) e [mensagens](https://github.com/OpenXbox/xbox-webapi-python/blob/master/xbox/webapi/api/provider/message/__init__.py), consultados como especificação de interoperabilidade, sem incorporar dependência.
- [friend-connect](https://github.com/cjmustard/friend-connect), formato de operações de amizade.
- [XStreaming: voz](https://github.com/Geocld/XStreaming/blob/main/src/webrtc/Channel/Chat.ts) e [licenças](https://github.com/Geocld/XStreaming/blob/main/src/xCloud/ownedLibrary.ts), com os créditos/AGPL preservados.
