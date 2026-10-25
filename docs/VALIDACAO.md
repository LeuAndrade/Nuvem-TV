# Validação — BXbox Cloud TV 0.4.0

## Executado

- Build `:app:testLiteUnitTest :app:lintLite :app:assembleLite` com Java 17/Gradle 8.9/SDK 35: BUILD SUCCESSFUL.
- 43 testes Java/Robolectric: zero falhas, zero erros. Login persistente, filtros, favoritos, foco desenhado na grade, seleção por controle, retorno de detalhes, paginação, categorias oficiais, falha e esvaziamento de listas, intervalo de atualização, permissões, licenças de compra/assinatura/teste, validade e respostas incompletas.
- `node tools/test-player.cjs`: passou. SDP, ICE, handshake, pacotes de controle, voz desligada sem captura, voz ligada com renegociação, permissão negada sem interromper o player, encerramento e encerramento com captura ainda pendente.
- Android Lint: zero erros/fatais, 20 avisos de estilo/compatibilidade (incluindo texto concatenado/localização, preferências de teste, API de navegação legada e ícone). Não são declaração de ausência de bugs.
- Renderização de interface Android nativa em 1920×1080, com dados/capas públicos reais da Microsoft: Início, gêneros, grade, detalhes, configurações e ajuda. Carrosséis reaproveitam no máximo quatro células por fileira.
- Listas SIGL populares, adicionados e saindo consultadas com market=BR/language=pt-BR. Produto hidratado pelo catálogo oficial; jogos sem metadados de nuvem ficam fora das fileiras.
- `Wire.java`, `Pad.java` e `LowLatencyDecoderFactory.java` comparados byte a byte com a 0.3, sem diferenças.
- APKs ARM32/ARM64 verificados por apksigner e manifesto identificado como br.com.nuvemtv, versão 0.4.0, versionCode 4, label BXbox Cloud TV.
- Certificado dos APKs locais comparado com a entrega local 0.3: o mesmo SHA-256. Não equivale à chave de um APK gerado em outro ambiente.

## Limites

Não houve teste de sessão xCloud autenticada, de compra real da conta, nem de microfone ou desempenho em Android TV físico. Os testes de streaming/voz usam simulações de protocolo. Não se pode afirmar que voz, licença ou latência foram validadas ponta a ponta numa TV de 1 GB.

O recurso de licenças usa um endpoint que a Microsoft documenta para parceiros; ele pode recusar o aplicativo. Resposta desconhecida não é convertida em ausência de compra. O servidor xCloud continua responsável por decidir acesso, assinatura, edição e possibilidade de transmitir um jogo.

Microfones/headsets precisam ser reconhecidos como entrada de áudio pelo Android. Um controle remoto com botão de voz não garante essa entrada. Voz depende de suporte e permissões do jogo/conta. Captura/encode de voz tem custo adicional quando ativada; o perfil de vídeo e decoder não foram alterados.

Atualização do feed acontece no catálogo em primeiro plano, com cache e prazo de seis horas, além de abertura/atualização manual. Não há promessa de notificação instantânea ou atualização quando o app estiver fechado. Interfaces não públicas do Xbox podem mudar.

Código e APKs correspondentes acompanham o pacote sob AGPL-3.0. Consulte docs/GUIA_GITHUB.md para instalação e geração pelo mesmo repositório.
