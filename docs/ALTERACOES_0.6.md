# BXbox Cloud TV 0.6.0

## Pesquisa e imagens

O endpoint de people search usa contrato 3 e decoração detail,preferredColor. Em HTTP 400/404 tenta detail e depois Profile por gamertag exato; erros 401/403 não são mascarados. Espaços e sufixos # são codificados. O perfil exato pode achar apenas correspondência exata, sem substituir a busca parcial do serviço.

Avatares de hosts Microsoft validados preservam a query necessária ao serviço de imagem. São decodificados em até 96×96 e compartilham o cache limitado existente. Só imagens visíveis são vinculadas; o cache é liberado na entrada do streaming.

## Social oficial incorporado

Uma instância lazy de WebView abre https://www.xbox.com/pt-BR/play/user (ou locale/perfil escolhido) dentro da Activity. Operações de grupo, convite, bloqueio e demais opções são realizadas nos controles oficiais. O wrapper não implementa um novo protocolo de Party, nem simula estados de envio/conexão. O login pode ser solicitado separadamente; CookieManager persiste a sessão. Sair da conta limpa cookies e armazenamento web.

Navegação principal restrita a HTTPS Xbox/autenticação Microsoft; câmera não é concedida. Captura de áudio só para a origem www.xbox.com, após permissão Android. Sem ponte JavaScript, leitura de cookies/tokens ou injeção na página remota. Popups de login acionados pelo usuário permanecem dentro do app. O jogo é aberto pela biblioteca nativa.

Recolher conserva a página; voltar ao app não destrói seu PeerConnection. Alternar telas reanexa o mesmo painel. Controle do jogo não recebe botões enquanto o social expandido está em foco. Fechar social, sair ou segundo plano encerra a página. Concessão de microfone é apenas uma reserva de captura, não prova de grupo conectado. A partida não disputa a captura reservada ao grupo. O estado real da chamada deve ser conferido no Xbox. A política de páginas ocultas do WebView pode afetar o áudio e precisa de teste físico.

## Áudio e informações opcionais

GameAudio pede foco de mídia com USAGE_GAME, sem mudar volume global ou forçar alto-falante/rota Bluetooth. Não disputa foco com o grupo que reservou o microfone. Native habilita as faixas recebidas, volume de faixa 1.0, recebe áudio sem microfone (recvonly); só liga sendrecv e capacidade chatStream quando captura foi solicitada. A captura não força 24 kHz no hardware: usa taxa suportada, mono, deixando resampling para Opus/WebRTC.

No player WebView, áudio remoto tem elemento/MediaStream separado do vídeo mudo, com reprodução iniciada/retomada em interação e no recebimento da faixa. Novas faixas de áudio não removem as faixas ativas anteriores. CSP aceita media blob/mediastream; encerramento libera ambas e o microfone.

voiceStats inicia falso; configurações habilitam sua escolha após ativar o microfone. Sem opt-in não há HUD nem timer de contadores de voz. O último estado pode ser consultado manualmente e o teste local de microfone preservado tenta taxas/fontes suportadas. Estatísticas de vídeo seguem independentes.

## Limites

Não houve conta Xbox autenticada, TV/microfone físico ou interlocutor no ambiente de desenvolvimento. Build/testes não comprovam som audível, mensagem recebida nem grupo estabelecido. Serviços não documentados podem recusar clientes e mudar. O painel oficial requer compatibilidade do WebView e recursos disponíveis à conta. Mais uma página remota/RTC consome RAM e CPU quando ativa; não há medição em TV de 1 GB. Não alterados Wire.java, Pad.java e LowLatencyDecoderFactory.java; nenhuma dependência de runtime nova.

## Referências

- Interface oficial: https://www.xbox.com/pt-BR/play/user
- WebChromeClient/PermissionRequest: https://developer.android.com/reference/android/webkit/WebChromeClient
- AudioAttributes: https://developer.android.com/reference/android/media/AudioAttributes
- Projeto de referência de protocolo (créditos/licença preservados): https://github.com/Geocld/XStreaming

O site oficial executa as operações reais apenas quando você as confirma; os testes sociais usam transporte simulado e não enviam ações para outras pessoas.
