# Validação BXbox Cloud TV 0.2.0 — 24/10/2026

| Verificação | Resultado |
| --- | --- |
| Compilação Android da variante Lite com R8 | BUILD SUCCESSFUL |
| Testes Java, autenticação, catálogo, interface e capas | 21 testes, 0 falhas, 0 erros |
| Android Lint | 0 erros, 0 fatais, 12 avisos |
| Player WebView, protocolo e pacotes simulados | Passou |
| Consulta pública ao catálogo e miniaturas Microsoft | HTTP 200, metadados e 12 capas obtidos para QA |
| Renderização de catálogo e detalhes em 1920×1080 | Inspecionada usando Robolectric com gráficos nativos |
| Nome, identificador e versão nos APKs | BXbox Cloud TV / br.com.nuvemtv / versionCode 2 |
| Assinatura dos APKs | Verificada, igual à assinatura da entrega anterior |
| Código dos seis componentes de streaming/controle/decoder | Idêntico à versão anterior, comparação byte a byte |
| Nova partida autenticada numa TV física | Não realizada neste ambiente |

## Testes de sessão

Os testes exercitam a classe Auth com armazenamento e respostas HTTP substitutos: reabertura reutiliza refresh token sem device code; tokens válidos não geram renovação repetida; falha temporária de rede preserva a credencial; invalid_grant confirmado desconecta; Sair da conta remove dados persistidos e em memória.

O cofre, o applicationId e as preferências mantêm os nomes da versão anterior para permitir migração. A chave de assinatura é a mesma desta entrega anterior, não necessariamente de APKs que o usuário tenha compilado em outra máquina ou no GitHub.

## Testes de biblioteca

Busca sem acentos, filtro de gênero, ordem A–Z/Z–A, favoritos, ordem recente, metadados Microsoft, restrição dos hosts de capa, navegação entre detalhes e início e ausência do atalho oficial. O teste de imagens lê uma imagem grande de um cache local, confirma redução para até 320×180, verifica o orçamento de RAM e a suspensão do carregador.

A renderização de QA usa somente metadados e capas públicas, sem conta Xbox autenticada. Não é uma captura de uma TV física e não mede FPS, uso total de RAM nem latência de streaming. As dependências Robolectric e org.json dos testes não entram no APK.

Os avisos Lint não foram ocultados com baseline. A navegação foi verificada separadamente; o relato de boa jogabilidade do streaming da versão anterior veio do usuário.

## Integridade dos motores preservados

- `java/br/com/nuvemtv/NativePlayer.java`: `64f686843c7b6bf9cc27d473b21fe227dd95c7318ea3f80c9174094a4cfb5743`
- `java/br/com/nuvemtv/WebPlayer.java`: `70e3dc54dd601b19df7dcd7b47dbafe97e66a3c5e65660f305cdaf6207ca9fef`
- `java/br/com/nuvemtv/Wire.java`: `7f65fec18e12b905864f6c68a7aeb5ef41c543891cded62b251145d53082ee07`
- `java/br/com/nuvemtv/Pad.java`: `2553b9b47d09baa0f4b013e201b0572ecb0cd733d0d004f3607d72be8942abb6`
- `java/org/webrtc/LowLatencyDecoderFactory.java`: `335cd959942926df00ac960c470dec6db9f3c14c8e96d9309ba75e4b81c51f77`
- `assets/player.js`: `9b3ef80ff430dd709a0bd1ca3bf4ca2b8982f6a05b64bdfc21140a6dd67a1b7f`

## APKs

- BXboxCloudTV-arm32.apk: 6763726 bytes (6.45 MiB); SHA-256 `4bf3199d4f1949fe847b0d05d707ac32bee1057c470280d253e315fbf34f870a`.
- BXboxCloudTV-arm64.apk: 11604380 bytes (11.07 MiB); SHA-256 `ab2b7670d247a1de7515ae2d8e64446e14125bde81379fc5cec608cdeb686aad`.
