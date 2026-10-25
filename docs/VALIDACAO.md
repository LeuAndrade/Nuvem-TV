# Validação — BXbox Cloud TV 0.5.0

## Resultado executado

- Gradle `:app:testLiteUnitTest :app:lintLite :app:assembleLite`: BUILD SUCCESSFUL, Java 17, SDK/build-tools 35, Gradle 8.9.
- 55 testes Java/Robolectric: zero falhas e zero erros. Regressão de login, filtros, favoritos, foco na TV, navegação das fileiras, retorno ao mesmo jogo, posse separada de assinatura/teste, pacotes comprados, paginação incompleta, promoções oficiais, idioma automático/manual e requisições sociais.
- Testes de serialização social usam um transporte simulado: validam método/URL/corpo, codificação de pesquisa, XUID, limites de mensagem e propagação de falha do servidor. Não comprovam que uma conta real recebeu uma solicitação/mensagem.
- `node tools/test-player.cjs`: passou. SDP, ICE, handshake, pacote de controle, áudio inicial sendrecv, addTrack, renegociação, chatStream ausente sem captura, permissão negada sem parar o jogo, contadores outbound e liberação inclusive quando a captura termina de abrir após encerramento. O PeerConnection/servidor desses testes é simulado.
- Teste de resposta SDP impede reutilizar uma resposta antiga na negociação de microfone e suporta respostas legadas sem requestId.
- Android Lint: zero erros/fatais e 16 avisos (estilo, localização de strings, compatibilidade de API e ícone). Não significa ausência absoluta de bugs.
- Interface renderizada com componentes Android nativos em 1920×1080 e fixture de 28 jogos/10 promoções obtidos do catálogo/site Microsoft em pt-BR. Destaques reais, fileiras, grade, detalhes e configurações foram inspecionados. A fixture é só de desenvolvimento e não entra no APK.
- `Wire.java`, `Pad.java` e `LowLatencyDecoderFactory.java` continuam iguais aos arquivos da versão 0.4; nenhuma troca de decoder por software.
- APKs ARM32 e ARM64 verificados com apksigner; manifesto br.com.nuvemtv, versão 0.5.0, versionCode 5, nome BXbox Cloud TV.
- Certificado local SHA-256 a1461b49275b49e7e55637318e52442f8ed7aad1b105d91cf55480cd11f584f0, o mesmo dos APKs locais 0.4/0.3 desta conversa. Uma instalação gerada no GitHub pode ter outra chave.

## O que não foi validado ou concluído

Não houve TV física, microfone físico, conta Xbox autenticada nem interlocutor. O ajuste de chatStream corrige uma diferença concreta do protocolo, mas não permite afirmar que o usuário já será ouvido no jogo. O teste local de microfone e os contadores enviados servem para o próximo teste real.

Não foi concluído um cliente de grupos de voz Xbox nem envio de convite de grupo, nem bloqueio nativo da conta. Essas ações encaminham explicitamente ao serviço oficial e não exibem um sucesso fictício. Não há equivalência completa com o Xbox Cloud oficial.

As chamadas sociais de texto/perfil e a consulta de compras foram implementadas, mas necessitam validação autenticada. Autorizações, conta, privacidade, controle familiar, restrição de cliente e alterações de API podem impedir operações. Uma consulta de licença recusada não vira ausência de compra; um envio recusado não vira mensagem local enviada.

Sem teste de desempenho/latência em aparelho físico de 1 GB. O caminho de vídeo foi mantido, mas captura, encode e diagnóstico de voz têm custo quando ligados. Estatísticas de vídeo continuam opcionais. A escolha de idioma cobre telas principais; conteúdo do servidor/pessoas e diagnósticos técnicos pode permanecer em seu idioma original.

Consulte docs/ALTERACOES_0.5.md para comportamento, limites e referências, e docs/GUIA_GITHUB.md para gerar o APK no mesmo repositório.
