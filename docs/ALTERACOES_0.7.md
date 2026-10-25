# BXbox Cloud TV 0.7.0

## Correções sociais

SocialState lê isFriend, isFriendRequestSent e isFollowedByCaller da raiz/detail; isFollowingCaller não é confundido com seguir a pessoa. Escreve confirmação local apenas após sucesso HTTP/resposta sem erros. Flags explícitas frescas prevalecem, com janela de 60 s para consistência eventual após escrita. Login preservado; saída remove o estado social. Ver perfil no Xbox substitui os dois botões redundantes e abre /play/user/gamertag.

ChatHistory entende contentPayload.content.parts (e partes legadas), sender, messageId, timestamp e exclusões. Envios aceitos são mostrados com indicação de sincronização até o mesmo ID aparecer na leitura; falha não vira mensagem enviada. Leituras antigas não apagam o envio aceito, mensagens com ID não duplicam. Cursor opaco é codificado, histórico ordenado, até 100 mensagens por conversa e seis conversas em RAM. Nenhum texto é salvo em disco. Falha de leitura conserva o conteúdo conhecido. Poll de 15 s só na conversa visível/primeiro plano; parado na navegação/streaming. Não substitui o serviço remoto nem prova recebimento pelo destinatário.

## Navegação e trabalho dispensável

Removido reopen/atalho flutuante. Voltar/B da página foca a barra nativa; Menu também. ↓ na barra foca WebView/popup, segundo Voltar/B recolhe ou fecha. Sem reserva de captura o painel é destruído ao sair; com reserva fica recolhido conservando a sessão, sem desenhar um atalho. Ao iniciar jogo não se mantém WebView social sem reserva de microfone. Cookies permanecem. Ao sair da partida o social pode ser reaberto por Jogar com amigos. Sem alteração no cliente nativo de Party (as operações continuam oficiais incorporadas).

Native e player WebView usam uma única consulta de estatísticas a cada 2 s para os dois indicadores opcionais. WebView evita JSON e evaluateJavascript para controle sem mudança, play() repetido em mídia que já toca e solicitação de keyframe para faixa de áudio. Captura web deixa de sugerir taxa de hardware fixa. Pacotes do controle e decoder de hardware preservados. Não houve medição de FPS/RAM/latência em TV real; não se pode afirmar que toda queda de desempenho tenha sido eliminada. Uma página social com voz ainda tem custo.

## Qualidade e RTT

Cloud.deviceInfo valida 720/1080 e envia dimensões correspondentes sem redução. Pedidos de dimensões/SDP preservados. Native RendererEvents e eventos loadedmetadata/resize do player web informam tamanho realmente decodificado por evento, sem poll adicional. Configurações mostram a comparação da última sessão nesta execução. Estatísticas exibem tamanho/bitrate medidos disponíveis, alvo selecionado e RTT do par ICE escolhido pelo transporte do vídeo, com fallback a candidato selecionado/nomeado bem-sucedido. Conversão segundos→ms, ausência/NaN nunca viram zero. Nenhum teste ICMP, upscale ou filtro artificial. RTT não mede latência completa. Compressão/resolução adaptativa dependem do servidor/rede.

## Limites e referências

Não testado com conta Xbox autenticada, TV, microfone físico ou interlocutor. Integrações autenticadas, áudio e continuidade do grupo recolhido precisam de validação real. O servidor pode recusar consultas, limitar histórico/privacidade e alterar APIs. Não há garantia de Full HD constante ou desempenho idêntico com voz social em 1 GB.

- Estrutura de mensagens/pessoas: https://github.com/OpenXbox/xbox-webapi-python/tree/master/xbox/webapi/api/provider
- RTT/transport selectedCandidatePairId: https://www.w3.org/TR/webrtc-stats/
- Projeto de referência, créditos AGPL preservados: https://github.com/Geocld/XStreaming

Consulte GUIA_GITHUB.md e VALIDACAO.md. Atualização no mesmo repositório; nunca publique a chave privada de assinatura.
