# BXbox Cloud TV 0.4.0

## Início em categorias

O destaque da biblioteca continua primeiro. Abaixo ficam Mais populares em nuvem, Adicionados recentemente, Saindo de cena em breve e Explore por gênero. As três fileiras usam SIGLs oficiais da Microsoft, em pt-BR/BR, com a ordem publicada, cruzados com os metadados de títulos de nuvem. Não há popularidade inventada a partir de favoritos ou do histórico local.

Cada fileira reaproveita até quatro células: ←/→ percorrem todos os títulos, ↓/↑ mudam de fileira e ← no primeiro jogo retorna ao menu. Ao voltar dos detalhes, o mesmo jogo continua focado. As setas pequenas também permitem avançar com clique. Os gêneros abrem a Biblioteca já filtrada.

A grade Todos os jogos e a busca saíram apenas do Início. Biblioteca, Pesquisar, Favoritos e Volte a jogar continuam disponíveis no menu lateral com os filtros anteriores.

As listas são consultadas na abertura/restauração da conta e podem ser atualizadas pelo botão Atualizar. Com o catálogo em primeiro plano, o app verifica o prazo de seis horas; falhas têm intervalo mínimo de quinze minutos para nova tentativa automática. Não há serviço permanente nem atualização durante o streaming. Listas indisponíveis conservam o último resultado; uma lista oficialmente vazia substitui o cache anterior.

Os IDs SIGL foram confirmados no HTML oficial Xbox Play e as três respostas pt-BR foram consultadas no desenvolvimento. Essas interfaces podem mudar. O app não promete reproduzir recomendações personalizadas, testes A/B ou a ordenação que uma conta específica vê no navegador.

## Microfone

Configurações > Streaming > Ativar microfone. É desligado por padrão. Ao ligar, pede RECORD_AUDIO ao Android. A preferência só é ativada após autorização. Na partida, verifica novamente a permissão e se existe uma entrada de áudio reconhecida; se não houver, avisa e continua sem microfone.

O Native cria uma faixa de áudio, usa o transceiver de áudio existente e renegocia somente quando o recurso está ligado, com isMediaStreamsChatRenegotiation. O WebView usa getUserMedia e o mesmo fluxo de renegociação; o WebChromeClient autoriza somente AUDIO_CAPTURE, somente na origem local do player e com permissão Android já concedida. Não libera câmera ou origens externas. Uma captura que terminar de abrir depois do encerramento tem suas faixas imediatamente paradas.

O áudio do jogo permanece recebido com microfone desligado. O microfone é silenciado/liberado no encerramento. Falhas de captura não encerram a partida. O custo adicional de captura/codificação de voz existe apenas quando ativado; não é possível prometer custo zero em TVs fracas. Hardware, entrada de áudio, privacidade da conta e suporte de chat do jogo continuam necessários. O microfone do controle remoto de voz pode não ser disponibilizado a aplicativos pelo fabricante.

## Licenças e biblioteca

Ao abrir os detalhes, consulta as licenças do produto da conta, com autenticação XSTS para licensing.xboxlive.com e Collections b2bLicensePreview, conforme o fluxo usado pelo XStreaming. O resultado diferencia licença direta Single, acesso por assinatura/pacote/teste, ausência de licença válida e consulta indisponível. Trata paginação, validade, tipo de produto, status e testes. Não confunde presença no catálogo público com posse e não usa palavras do título para adivinhar compras.

A consulta tem uma fila própria, descarta pedidos pendentes de telas já fechadas e usa referências fracas para não reter capas da biblioteca durante o streaming. Uma resposta lenta da loja não ocupa a fila de abertura da sessão. Resultados ficam apenas na memória por quinze minutos, são apagados ao atualizar a biblioteca/sair e nunca armazenam tokens nesse cache. Falha HTTP, falta de autorização e resposta incompleta produzem estado desconhecido, não “não comprado”. A Microsoft documenta essa API para serviços parceiros e pode recusar clientes independentes; a consulta pode portanto ficar indisponível em contas/dispositivos. Isso é uma limitação real, não uma garantia de integração oficial.

Sem licença identificada e sem entitlement de nuvem, os detalhes orientam comprar jogos de compra na loja oficial Xbox ou verificar assinatura para Game Pass. A ausência no retorno não comprova que outra edição, licença compartilhada, disco ou compra em outra loja seja utilizável. Por isso, não bloqueia o botão de tentativa: a Microsoft valida a licença ao iniciar. Ter comprado um título não torna qualquer jogo compatível com nuvem nem substitui um plano exigido pelo serviço.

## Desempenho e compatibilidade

Sem novas bibliotecas no APK. Decoder de baixa latência, fábrica de hardware, empacotamento de controle e configuração de vídeo preservados. Cache de imagens 3 MiB em RAM/20 MiB em disco, dois carregadores e filas limitadas. Ao abrir o streaming, as fileiras e imagens são liberadas e a atualização automática é suspensa. Mantidos Client ID interno, cofre da conta e todas as configurações existentes.

applicationId br.com.nuvemtv, versionCode 4. Os APKs desta entrega usam a mesma assinatura dos APKs locais 0.3 desta conversa; APKs produzidos no GitHub podem usar outra chave. Atualizações mantendo dados sempre exigem a chave original da instalação.

## Referências de implementação

- [Xbox Cloud Gaming](https://www.xbox.com/pt-BR/play) e [catálogo oficial](https://catalog.gamepass.com/sigls/v2?id=06323672-b8c8-43cc-b0de-32d5a9834749&market=BR&language=pt-BR).
- [Permissões Android](https://developer.android.com/training/permissions/requesting) e [WebChromeClient](https://developer.android.com/reference/android/webkit/WebChromeClient).
- [Formato de Collections v8](https://learn.microsoft.com/en-us/gaming/gdk/docs/store/commerce/service-to-service/microsoft-store-apis/xstore-v8-query-for-products).
- [XStreaming: licenças](https://github.com/Geocld/XStreaming/blob/main/src/xCloud/ownedLibrary.ts) e [voz](https://github.com/Geocld/XStreaming/blob/main/src/webrtc/Channel/Chat.ts). As adaptações mantêm crédito e AGPL-3.0; o projeto não é um fork.
