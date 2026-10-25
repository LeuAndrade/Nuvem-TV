# Validação — BXbox Cloud TV 0.6.0

## Verificado no ambiente de desenvolvimento

- Gradle :app:testLiteUnitTest :app:lintLite :app:assembleLite: BUILD SUCCESSFUL, Java 17, SDK/build-tools 35, Gradle 8.9.
- 70 testes Java/Robolectric: zero falhas, zero erros, zero ignorados. Incluem regressões anteriores de login/layout/foco/catálogo/compras/idioma e novos casos de pesquisa com fallback 400, não mascarar 403, codificação de gamertag, URLs de avatares, fotos na lista/perfil e orçamento de imagem 96×96.
- SocialIntegrationTest verifica painel interno sem Intent externo, mesma instância entre telas e ao abrir o jogo, restrição de navegação/origem de captura, opção de informações de voz desligada por padrão, habilitação após permissão, atributos de áudio de mídia sem mudar volume e isolamento dos botões do controle enquanto o social está expandido. Não acessa uma conta real nem estabelece um grupo remoto.
- Testes de transporte social validam método, URL, corpo e tratamento de falhas usando respostas simuladas. Não comprovam mensagem recebida, convite enviado ou bloqueio de uma conta real.
- node tools/test-player.cjs: passou. SDP/ICE, controle, áudio remoto separado do vídeo e habilitado, faixas simultâneas preservadas, recvonly sem microfone, sendrecv/captura quando ativado, renegociação, voiceStats opcional sem coleta por padrão, negação de permissão e liberação das faixas inclusive em captura assíncrona pendente. PeerConnection/servidor simulados.
- Android Lint: zero erros/fatais, 18 avisos remanescentes de estilo, localização, compatibilidade e ícone. Não significa ausência absoluta de bugs.
- Interface nativa renderizada nos testes a 1920×1080 com fixture pública de catálogo. A fixture é somente de desenvolvimento e não entra no APK. Não houve teste de TV física.
- Wire.java, Pad.java e LowLatencyDecoderFactory.java comparados à versão 0.5, sem alteração. Dependências de runtime preservadas. Decodificação nativa continua restrita a hardware.
- APKs ARM32 e ARM64 compilados e verificados com apksigner. Manifesto br.com.nuvemtv, versionName 0.6.0, versionCode 6, nome BXbox Cloud TV.
- Certificado local SHA-256 a1461b49275b49e7e55637318e52442f8ed7aad1b105d91cf55480cd11f584f0, igual aos APKs locais 0.5/0.4/0.3. Um APK gerado no GitHub pode ter outra chave.
- Instalador verificado em diretório vazio e em atualização preservando workflow existente. Arquivos extraídos comparados com o projeto empacotado. Nenhuma chave privada incluída.

## O que ainda exige teste real

Não houve TV, microfone físico, conta Xbox autenticada nem interlocutor. As correções de áudio tratam diferenças concretas de configuração e reprodução, mas não comprovam som audível no seu aparelho ou voz recebida por outra pessoa. Os testes não certificam as operações sociais autenticadas, consulta de compras ou continuidade de grupo recolhido.

Grupos/convites/bloqueio usam a página original Xbox incorporada em WebView; não há novo cliente nativo de Party. As ações dependem do serviço, conta, privacidade, compatibilidade e versão do WebView. O login oficial pode ser pedido uma primeira vez separadamente. A concessão de microfone não prova conexão com um grupo. Fechar social ou colocar o app em segundo plano encerra a página/captura. WebViews podem restringir reprodução de páginas ocultas; confirme a continuidade após recolher.

Não houve medição de RAM, latência ou FPS em aparelho de 1 GB. O caminho de vídeo foi preservado e a área social é criada somente ao abrir, mas a página oficial e a voz ativa aumentam o consumo. Resolução, codec, bitrate e baixa latência continuam sujeitos ao servidor/hardware.

Consulte ALTERACOES_0.6.md e GUIA_GITHUB.md para uso e limitações.
