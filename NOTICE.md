# BXbox Cloud TV — créditos e licença

Projeto Android próprio, iniciado em uma árvore nova, sem histórico Git ou interface React Native do XStreaming. Ele contém adaptações de protocolo; não é uma implementação inteiramente independente dos projetos abaixo.

## XStreaming
- Autores: Geocld e contribuidores do XStreaming.
- Fonte: https://github.com/Geocld/XStreaming
- Referência consultada: commit 9e6a21251c379c861cc48f105530895dab9180c3.
- Licença: GNU Affero General Public License, versão 3; texto completo em LICENSE.
- Adaptações: fluxo OAuth por dispositivo, troca Xbox/XSTS/GSSV, endpoints de catálogo e sessão, parâmetros SDP, nomes/protocolos dos canais WebRTC, handshake e formato binário de controle.
- Arquivos de referência: src/xal/msal.ts, src/xCloud/index.ts, src/utils/xcloud.ts, src/webrtc/index.ts, src/webrtc/Channel/{Control,Message,Input}.ts e src/webrtc/Packet/index.ts.
- Client ID público padrão: o mesmo presente na implementação de referência. Não é segredo, autorização própria nem garantia de aceitação futura. A tela de consentimento é da Microsoft; nunca forneça sua senha ao projeto.

## WebRTC e ferramentas
- io.github.webrtc-sdk:android:125.6422.07 — https://github.com/webrtc-sdk/android (empacotamento MIT).
- WebRTC — https://webrtc.googlesource.com/src (BSD de três cláusulas, com dependências sob suas próprias licenças).
- LowLatencyDecoderFactory é um adaptador próprio acoplado à API interna da versão fixada; não atualizar o SDK sem revisar este adaptador e testar.
- Gradle Wrapper gerado com Gradle 8.9, Apache-2.0: https://github.com/gradle/gradle/blob/v8.9.0/LICENSE.
- Cópias das licenças principais estão em third_party/ e nos assets do aplicativo.

Microsoft, Xbox e nomes de jogos são marcas de seus titulares. BXbox Cloud TV é um cliente experimental independente, sem afiliação.

Distribua o código correspondente junto ao APK e preserve créditos e licença AGPL-3.0. O workflow inclui automaticamente o código da mesma revisão. Não coloque senhas, tokens pessoais ou chaves de assinatura no repositório.
