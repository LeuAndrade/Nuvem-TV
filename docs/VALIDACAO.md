# Validação — BXbox Cloud TV 0.7.0

## Testes locais

- Gradle :app:testLiteUnitTest :app:lintLite :app:assembleLite: BUILD SUCCESSFUL. Java 17, Android SDK/build-tools 35, Gradle 8.9.
- 88 testes Java/Robolectric: zero falhas, zero erros, zero ignorados. Regressões anteriores de autenticação, foco, biblioteca, catálogo, compras, idiomas, imagens e protocolo preservadas.
- Novos testes cobrem relacionamento na raiz/detail, direção de seguir, confirmação apenas após resposta aceita, rótulos no perfil, limpeza/reconciliação, texto em contentPayload.content.parts, remetentes e ordem, exclusão, envio aceito mantido durante consistência eventual, deduplicação, cursor e limite de histórico. Integração Android usa transporte simulado para enviar pedido/seguir/mensagem e conferir atualização da interface; não contata pessoas reais.
- Navegação social: ausência de atalho flutuante, um só botão de perfil oficial, Voltar focando barra e segundo Voltar saindo, página sem captura destruída ao recolher, retenção da mesma página com reserva do microfone e isolamento dos botões do jogo quando expandida.
- Ping: seleção do par ICE pelo transporte do vídeo, prioridade sobre pares antigos, RTT convertido em ms, NaN/ausência apresentados como —, zero real preservado.
- Qualidade: dispositivo de 1280×720/1920×1080 conforme a seleção, seleção inválida recusada. Teste WebView de controle parado/mudando comprova evitar novas serializações/injeções sem mudança; não é benchmark de FPS.
- node tools/test-player.cjs: passou. SDP/ICE/bitrate, mensagens de dimensões 720p/1080p, pacotes do controle, áudio remoto não mudo separado do vídeo, nenhuma repetição de play() em mídia já ativa, faixas simultâneas, microfone/renegociação/negação/liberação, coleta única opcional, RTT e eventos de dimensões recebidas. PeerConnection/servidor e elementos de mídia simulados.
- Android Lint: zero erros/fatais; 18 avisos remanescentes de estilo/localização/compatibilidade/ícone. Isso não significa ausência absoluta de bugs.
- Wire.java, Pad.java e LowLatencyDecoderFactory.java comparados à 0.6 sem alteração. Decoder Native continua por hardware, sem nova dependência de runtime.
- APKs ARM32/ARM64 verificados com apksigner; br.com.nuvemtv, 0.7.0, versionCode 7, BXbox Cloud TV. Certificado local SHA-256 a1461b49275b49e7e55637318e52442f8ed7aad1b105d91cf55480cd11f584f0 igual aos APKs locais 0.6/0.5/0.4/0.3.
- Instalador testado do zero e em atualização preservando workflow, fontes/arquivos/APKs comparados e checksums conferidos. Nenhuma chave privada no pacote.

## Limites de verificação

Não houve TV, conta Xbox autenticada, microfone físico ou interlocutor. Não foi medido FPS, latência, uso de RAM ou nitidez no aparelho real. As otimizações eliminam trabalho identificável no código, mas não comprovam que toda regressão de streaming foi resolvida. O serviço social original com voz continua custando memória/CPU quando necessário.

As respostas e notificações autenticadas, histórico real, amizade/mensagem recebida e grupo precisam de teste com a conta. APIs podem negar operações por privacidade/autorização e mudar formato. A interface não apresenta uma falha como envio feito. Grupos continuam no serviço oficial incorporado ao app, sem novo protocolo nativo de Party. A continuidade de página recolhida depende do WebView. Fechar social/segundo plano encerra a página/captura.

720p/1080p são pedidos sem redução automática local. A dimensão recebida é observada no decoder/elemento de vídeo, não inferida do tamanho da tela. O servidor/rede pode adaptar bitrate, compressão e resolução. A verificação não garante Full HD constante, detalhe perceptual equivalente, HDR nem ausência de artefatos. Ping ICE mede ida/volta da rede, não toda a latência de controle/decoder/tela.

GUIA_GITHUB.md explica atualização no mesmo repositório, uso do controle e comparação de qualidade; ALTERACOES_0.7.md documenta implementação e referências.
