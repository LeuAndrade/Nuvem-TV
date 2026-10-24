# Nuvem TV

Cliente experimental de Xbox Cloud Gaming para Android TV, Android 7.0/API 24 ou superior. Interface Java Android enxuta, com navegação pelo controle remoto, sem React Native, imagens de catálogo, anúncios próprios ou serviços em segundo plano.

**Não foi validado com uma sessão autenticada Xbox nem em TV de 1 GB. Compilar não prova que o login, o protocolo ou o desempenho funcionarão no seu aparelho.** Consulte `docs/VALIDACAO.md` para o que foi testado.

## Funções implementadas

- Todos os jogos: união dos títulos retornados pela conta e de listas públicas Microsoft, com pesquisa e páginas de 40 itens; disponibilidade real depende da região, plano e propriedade de cada jogo.
- Volte a jogar: histórico retornado por `/v2/titles/mru`. Abrir inicia uma sessão; o progresso depende dos saves do serviço, não de uma captura local.
- Login Microsoft por código de dispositivo e armazenamento do refresh token cifrado no Android Keystore; logout remove a credencial e os cookies locais.
- Native: WebRTC nativo, SurfaceView/EGL, decodificador MediaCodec de hardware, sem fallback para software.
- WebView: player WebRTC local próprio usando a pilha WebRTC do Android System WebView; o sistema decide a implementação de decoder.
- Atalho adicional para `https://www.xbox.com/pt-BR/play`. Nesse atalho, os ajustes do nosso player não controlam o site oficial.
- Ajustes: motor, estatísticas, 720p/1080p, pedido de limite de 3–15 Mb/s, região retornada pela conta, codec e solicitação de baixa latência.
- Codecs oferecidos na tela: H264, VP8, VP9, H265 e AV1. O motor Native falha explicitamente se não houver hardware WebRTC compatível; o servidor pode rejeitar codecs mesmo quando há hardware.
- Controle físico: botões, dois analógicos, gatilhos e D-pad. Sem controle virtual, microfone, chat, vibração ou interfaces sociais do Xbox nesta versão.

## Perfil inicial

Native / H264 / 720p / 5 Mb/s / estatísticas desligadas. Para medir, ative estatísticas por alguns minutos. Baixe para 3 Mb/s se necessário, sabendo que a imagem perde qualidade e isso não garante menor custo de decode. Evite 1080p no primeiro teste.

A solicitação de baixa latência usa `MediaFormat.KEY_LOW_LATENCY` somente no Android 11+ e apenas quando o decoder anuncia suporte. Nenhuma propriedade privada de fabricante é forçada. Há opção de desligar para diagnosticar drivers defeituosos.

O bitrate é solicitado por SDP (`b=AS` e `b=TIAS`); não é um limitador rígido. A resolução é solicitada no perfil de sessão e na mensagem de dimensões; não se reduz a imagem recebida apenas redimensionando a tela. O servidor pode entregar outro fluxo. O aplicativo não consegue criar suporte de hardware que o aparelho não possui.

## Gerar o APK

Passo a passo para iniciantes: `docs/GUIA_GITHUB.md`.

Em um ambiente com JDK 17 e Android SDK 35:

```bash
node tools/test-player.cjs
bash gradlew :app:testLiteUnitTest :app:lintLite :app:assembleLite
```

Saídas em `app/build/outputs/apk/lite/`. Há um APK para `armeabi-v7a` e outro para `arm64-v8a`. A ABI do sistema Android importa mais que o anúncio do processador.

A variante `lite` usa R8 e remove recursos não usados; não é depurável. Está assinada com uma chave de desenvolvimento para instalação de teste. Uma nova máquina de CI pode gerar outra chave: nesse caso é preciso desinstalar a versão antiga ou configurar uma assinatura permanente antes de distribuir. Desinstalar apaga login e preferências locais. Não publique essa variante como produto final em uma loja.

## Estrutura

- MainActivity: interface, configurações, ciclo de vida e seleção de motor.
- Auth / Vault / Net: autenticação, armazenamento cifrado e HTTPS.
- Cloud: catálogo, sessão, SDP, ICE, keepalive e encerramento.
- NativePlayer / org.webrtc.LowLatencyDecoderFactory: WebRTC e decoder de hardware.
- WebPlayer / assets/player.js: player WebView e ponte restrita a operações de sessão.
- Wire / Pad: pacote binário e controles físicos.
- .github/workflows/android.yml: testes, análise estática e compilação no GitHub.

## Segurança e limitações

A implementação usa interfaces não públicas do Xbox Cloud Gaming e pode precisar de manutenção se a Microsoft mudá-las. Login por código usa o Client ID público presente na referência XStreaming; ele pode deixar de ser aceito ou exigir permissões que um novo registro não possui. Para distribuição própria, obtenha um registro e autorização apropriados. Trocar apenas o Client ID não garante acesso Xbox.

Sem senha no APK, sem backend de terceiros e sem credenciais no código. Tokens ficam fora do Git. Fluxos de conta infantil, MFA, restrições regionais, planos e jogos comprados dependem da Microsoft. Não há garantia de catálogo universal, de sessão bem-sucedida ou de ausência de bugs.

Licença AGPL-3.0; leia `LICENSE` e `NOTICE.md`.
