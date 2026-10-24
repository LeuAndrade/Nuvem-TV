# Verificação da entrega — 24/10/2026

Versão: 0.1.0-experimental. Android 7.0/API 24 ou superior. Nenhum APK desta entrega foi executado numa TV real.

| Verificação | Resultado |
| --- | --- |
| Compilação Android Debug inicial | Passou |
| Compilação Lite/R8 inicial e após correções | Passou |
| Compilação final `testLiteUnitTest lintLite assembleLite` | BUILD SUCCESSFUL |
| Testes Java de formato binário e restrição de hosts | 5 testes, 0 falhas, 0 erros |
| Teste JavaScript simulado | Boot, SDP, ICE, handshake, envio de controle e encerramento passaram |
| Sintaxe do JavaScript | Válida |
| YAML do GitHub Actions | Estrutura válida; workflow ainda não executado em um repositório GitHub |
| Android Lint | 0 erros/fatais, 8 avisos |
| Catálogo público Microsoft | Consultas SIGL e hidratação de dois produtos, HTTP 200; nomes e XCloudTitleId presentes |
| Assinatura dos APKs | Verificada por apksigner; chave de desenvolvimento |
| Login Xbox completo e consentimento real | Não testados |
| Sessão Xbox / SDP e ICE com servidor autenticado | Não testados |
| Hardware decoder / áudio / gamepad em aparelho | Não testados |
| Desempenho, temperatura e RAM em TV de 1 GB | Não medidos |

Os testes de protocolo usam respostas simuladas. Eles verificam a estrutura do cliente; não certificam compatibilidade com o serviço ao vivo. Os testes Java não simulam MediaCodec nem EGL.

Avisos Lint: gravação síncrona das credenciais (executada fora da interface), banner vetorial, orientação fixa, regra de extração de backup, textos fixos em português e gravidade de sobreposição à esquerda. Não foram escondidos por um baseline. Não são uma confirmação de falha de streaming, mas devem ser revistos antes de uma distribuição de produção.

As estatísticas exibem média de decodificação por diferenças entre amostras de totalDecodeTime/framesDecoded, sem descontos artificiais. Quando não há contador disponível, exibem um traço. Elas não medem a latência total entre controle e imagem.

Esta entrega implementa as opções solicitadas, mas não é uma promessa de ausência de erros ou de melhora garantida em qualquer TV. O maior teste restante é uma partida real na TV alvo.

## Tamanho e integridade dos APKs

- NuvemTV-arm64.apk: 11599896 bytes (11.06 MiB); SHA-256 `461d5d8bd2c6c04531e456a73456bdaf44a8418f3943e068cc29ec686167ed49`.
- NuvemTV-arm32.apk: 6759242 bytes (6.45 MiB); SHA-256 `8db0e0fcd9a8b103d28714034e87681cedaab3fcccfa559639122b862a58fecc`.
