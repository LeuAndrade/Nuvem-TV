# Validação BXbox Cloud TV 0.3.0 — 24/10/2026

| Verificação | Resultado |
| --- | --- |
| Compilação Lite / R8 | BUILD SUCCESSFUL |
| Testes automatizados | 29 testes, 0 falhas, 0 erros |
| Android Lint | 0 erros/fatais, 8 avisos |
| Simulação do protocolo WebView | Passou |
| Client ID interno e identificação salva | Preservados; editor ausente das três seções de configuração |
| ↓ dos filtros para os cartões | Foco e pixels da borda verde verificados |
| Troca A–Z/Z–A ou gênero, depois ↓ | Foco e contorno continuam visíveis |
| Movimento entre cartões e retorno ao filtro/menu | Verificados com eventos de D-pad |
| Configurações de streaming, rede e conta | Preservadas e verificadas |
| Limpeza da biblioteca antes do streaming | Cache suspenso e referências a vistas/diálogos liberadas |
| Arquivos de autenticação, streaming, controle e cofre | 9 arquivos inalterados byte a byte |
| Nome / identificador / versão do APK | BXbox Cloud TV / br.com.nuvemtv / versionCode 3 |
| Assinatura ARM32 e ARM64 | Verificadas; mesma chave entre os dois APKs desta geração |
| Performance numa TV física | Não medida nesta atualização |

A interface foi renderizada em 1920×1080 com Robolectric e gráficos nativos, usando capas e metadados públicos Microsoft. Essas imagens de QA não são uma captura de uma sessão Xbox autenticada. A conferência de pixels verifica o contorno verde do item realmente selecionado, não só uma variável de foco.

Os testes adicionais cobrem preservação de identificador de autenticação já salvo e remoção apenas do editor público. Auth.java, Cloud.java e Vault.java continuam com os mesmos bytes. Foram mantidos os seis arquivos de streaming/controle/decodificação.

A biblioteca continua com cache de bitmaps de 3 MiB, arquivos de 20 MiB e duas transferências simultâneas. As capas dos cartões são limitadas a 320×180; destaque e detalhes podem usar 640×360 no mesmo orçamento de RAM. Não há animação de escala, carrossel automático nem reprodução de vídeos na interface.

Não há garantia de desempenho em todos os aparelhos. Os testes de sessão e WebRTC usam respostas simuladas; o relato de streaming jogável nas versões anteriores veio do usuário.

## Assinatura

Certificado SHA-256 desta geração: `a1461b49275b49e7e55637318e52442f8ed7aad1b105d91cf55480cd11f584f0`.

O applicationId e o formato da sessão foram preservados. Isso não basta para atualizar por cima de um APK assinado com outra chave. Estes APKs podem ter assinatura diferente de versões anteriores ou compiladas no GitHub. Para preservar os dados numa atualização, use a mesma chave do APK instalado. Desinstalar remove login e preferências locais. A chave privada de desenvolvimento não está incluída no projeto ou no ZIP público.

## Arquivos preservados

- `java/br/com/nuvemtv/NativePlayer.java`: `64f686843c7b6bf9cc27d473b21fe227dd95c7318ea3f80c9174094a4cfb5743`
- `java/br/com/nuvemtv/WebPlayer.java`: `70e3dc54dd601b19df7dcd7b47dbafe97e66a3c5e65660f305cdaf6207ca9fef`
- `java/br/com/nuvemtv/Wire.java`: `7f65fec18e12b905864f6c68a7aeb5ef41c543891cded62b251145d53082ee07`
- `java/br/com/nuvemtv/Pad.java`: `2553b9b47d09baa0f4b013e201b0572ecb0cd733d0d004f3607d72be8942abb6`
- `java/org/webrtc/LowLatencyDecoderFactory.java`: `335cd959942926df00ac960c470dec6db9f3c14c8e96d9309ba75e4b81c51f77`
- `assets/player.js`: `9b3ef80ff430dd709a0bd1ca3bf4ca2b8982f6a05b64bdfc21140a6dd67a1b7f`
- `java/br/com/nuvemtv/Auth.java`: `e626d1e1a33ef34f64dc912fabf42bc43c0e0fbcb843f528e825e2456cf0ea11`
- `java/br/com/nuvemtv/Cloud.java`: `857bf45bb7626461213dda7b7d615d33d2c4acb66b1814b58cb9e72a1fd0309b`
- `java/br/com/nuvemtv/Vault.java`: `ddc3868c9925950373643d73bb9c2a89773bc389784e1206a3e9443dcdf064e2`

## APKs

- BXboxCloudTV-v0.3-arm32.apk: 6781746 bytes (6.47 MiB); SHA-256 `cf35f0fcde6831386e493e0cb972a62549fbd59a950fd426341919bb92514a5c`.
- BXboxCloudTV-v0.3-arm64.apk: 11622400 bytes (11.08 MiB); SHA-256 `d0e44049af3193e55500da16505ad3b9e3dadb89a526ea0dceb03a1954390e3a`.
