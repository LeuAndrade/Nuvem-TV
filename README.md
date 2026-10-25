# BXbox Cloud TV — 0.5.0

Cliente Android TV de Xbox Cloud Gaming com interface Java Android própria, API 24+. Sem frameworks de interface pesados. `applicationId br.com.nuvemtv`, `versionCode 5`, versão 0.5.0. Guia: [docs/GUIA_GITHUB.md](docs/GUIA_GITHUB.md).

## Estado desta entrega

- Início: destaques promocionais oficiais atualizáveis; Volte a jogar em fileira horizontal acima de populares; Game Pass, transmita seu próprio jogo, adicionados, saindo, gêneros e acesso à grade de todos os jogos.
- Meus jogos: consulta de compras/resgates da conta, separada do catálogo e de assinatura. Jogos não compatíveis com nuvem não são iniciados como stream.
- Idioma automático pelo sistema ou português, inglês e espanhol.
- Voz no jogo: corrigida negociação chatStream, áudio sendrecv, captura mono 24 kHz, addTrack e renegociação; diagnóstico local de microfone e indicador de bytes/pacotes enviados.
- Social: chamadas de pesquisa, amigos, sugestões, perfil, seguir, pedido de amizade e mensagens. Grupos de voz/convites e bloqueio nativo NÃO estão integrados: as ações correspondentes oferecem abrir o Xbox oficial. Não são apresentadas como operações executadas.

As integrações autenticadas e a voz precisam de teste real na conta/TV; esta entrega tem compilação e testes locais, não certificação ponta a ponta. Detalhes, limites e referências: [docs/ALTERACOES_0.5.md](docs/ALTERACOES_0.5.md). Validação: [docs/VALIDACAO.md](docs/VALIDACAO.md).

## Configurações preservadas

Native/WebView local, estatísticas, 720p/1080p, pedido de bitrate de 3 a 15 Mb/s, codecs, região, baixa latência suportada, perfil leve, conta/logout e licenças. Client ID permanece interno e sem editor público. O login existente é restaurado pelo refresh token cifrado no Android Keystore. Erros temporários de rede não apagam a conta.

Perfil inicial: Native / H264 / 720p / 5 Mb/s. Native admite somente decoder por hardware. Codec, resolução, bitrate e baixa latência dependem de servidor, hardware e driver. Não há garantia universal de desempenho em qualquer aparelho de 1 GB.

## Memória

Até quatro células por fileira, carregamento de capas visíveis, grade paginada com reciclagem, dois carregadores de imagens, fila de 20, cache de 3 MiB em RAM e 20 MiB em disco. Metadados até 3 MiB. Ao entrar no streaming, imagens, views e atualizações do catálogo são liberadas/suspensas. Sem vídeos de fundo ou carrossel automático. Captura/encode/estatísticas de voz existem apenas quando o microfone é ativado e têm custo adicional.

`Wire.java`, `Pad.java` e `LowLatencyDecoderFactory.java` foram preservados. A única dependência de runtime é o WebRTC Android fixado no Gradle; as bibliotecas de teste não entram no APK.

## Compilar

Java 17, Android SDK 35/build-tools 35.0.0, Gradle 8.9.

```bash
node tools/test-player.cjs
bash gradlew :app:testLiteUnitTest :app:lintLite :app:assembleLite
```

APKs ARM32/ARM64 em `app/build/outputs/apk/lite/`. Workflow do GitHub em `.github/workflows/android.yml`. A assinatura de uma atualização deve ser a mesma do APK instalado para conservar dados. APKs locais desta conversa usam a chave local preservada; builds em outro ambiente podem usar outra chave. Nunca publique a chave privada no repositório.

AGPL-3.0, com créditos de protocolo a XStreaming. Preserve LICENSE, NOTICE e o código correspondente ao distribuir os APKs. Projeto independente, sem afiliação com Microsoft ou Xbox.
