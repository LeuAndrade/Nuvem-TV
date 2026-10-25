<div align="center">

# BXbox Cloud TV

### Seus jogos na nuvem, em uma interface feita para a sua TV.

**Catálogo organizado · Dois motores de streaming · Recursos sociais · Foco em leveza**

Android TV • Android 7.0 ou superior • ARM32 e ARM64 • Versão 0.7.0

</div>

---

O **BXbox Cloud TV** é um cliente independente de Xbox Cloud Gaming para Android TV, com navegação por controle, uma biblioteca visual e opções para ajustar a transmissão ao seu aparelho e à sua conexão.

O projeto prioriza uma interface simples e o uso cuidadoso de memória, com atenção especial a TVs e TV Boxes de poucos recursos, incluindo aparelhos com 1 GB de RAM.

## 🎮 Uma biblioteca para descobrir sua próxima partida

Explore os jogos em fileiras horizontais com capas, nomes e navegação pelas setas do controle:

- **Destaques promovidos:** campanhas e destaques obtidos do site oficial do Xbox Cloud.
- **Volte a jogar:** seus jogos recentes, logo na página inicial.
- **Mais populares em nuvem:** descubra títulos em evidência no catálogo.
- **Jogar com o Game Pass:** explore a seleção disponível por assinatura.
- **Transmita seu próprio jogo:** encontre títulos do catálogo de compra e transmissão.
- **Adicionados recentemente:** acompanhe as novidades.
- **Saindo de cena em breve:** veja a seleção disponibilizada pela Microsoft.
- **Gêneros:** escolha o tipo de jogo que quer encontrar.
- **Todos os jogos:** abra o catálogo completo com busca, ordenação A–Z/Z–A e filtros por gênero.

As categorias usam metadados da Microsoft e contam com atualização automática e manual. O conteúdo pode variar conforme o catálogo, a região e a disponibilidade do serviço.

## 📚 Seus jogos, do seu jeito

- **Meus jogos** consulta compras e resgates da conta, separando a posse individual do acesso por assinatura.
- Detalhes do jogo com imagem, nome e opções para jogar ou voltar à biblioteca.
- Favoritos para reunir sua seleção pessoal.
- Consulta de licença e indicação quando não for possível confirmar a compra ou o acesso.
- Acesso à loja oficial para os títulos que precisem ser adquiridos.

A compra de um jogo não garante compatibilidade com a nuvem: o título também precisa estar habilitado para streaming pelo Xbox.

## ⚡ Leveza desde a biblioteca até a partida

- Interface Android nativa, sem frameworks de interface pesados.
- Capas carregadas conforme ficam visíveis.
- Fileiras e grade com reciclagem de células.
- Cache de imagens limitado para controlar o consumo de memória.
- Liberação das imagens e suspensão das atualizações da biblioteca ao iniciar o streaming.
- Página social criada somente quando aberta e liberada ao sair quando não precisa manter a captura de voz.
- Sem vídeos de fundo ou carrosséis automáticos.
- **Perfil leve** para aplicar rapidamente Native, H264, 720p e 5 Mb/s.

No modo Native, a decodificação de vídeo utiliza **hardware compatível**, evitando o fallback para decodificação por software. A opção de baixa latência aproveita o suporte oferecido pelo decoder e pelo Android.

## 🖥️ Escolha como jogar

| Opção | O que oferece |
| --- | --- |
| **Native** | Reprodução com WebRTC Android e decodificação de vídeo por hardware. |
| **WebView** | Player WebRTC local do aplicativo, executado pelo Android System WebView. |
| **Resolução** | Solicitação de 720p ou 1080p. |
| **Bitrate** | Limites selecionáveis de 3, 5, 8, 10 ou 15 Mb/s. |
| **Codec** | H264, VP8, VP9 e opções de H265/AV1 quando disponíveis e compatíveis. |
| **Servidor** | Seleção automática ou escolha entre as regiões disponíveis para a conta. |
| **Baixa latência** | Solicitação no Native quando suportada pelo hardware e pelo sistema. |

O aplicativo preserva suas escolhas de resolução e bitrate. A transmissão final continua sujeita à adaptação do servidor, às condições da rede e à compatibilidade do aparelho.

## 📊 Saiba como a transmissão está chegando

Ative as estatísticas para acompanhar os dados disponíveis da sessão:

- Resolução recebida e resolução solicitada.
- Taxa de quadros por segundo — FPS.
- Bitrate recebido e limite escolhido.
- **Ping da conexão WebRTC**, em milissegundos.
- Tempo de decodificação, perda de pacotes e identificação do decoder, quando informados pelo motor.

Em **Configurações → Streaming → Qualidade da última sessão**, compare a resolução solicitada com a realmente recebida, mesmo sem deixar as estatísticas sobre o jogo.

O ping representa o tempo de ida e volta da conexão WebRTC; não corresponde à latência completa do controle até a imagem na TV. Dados indisponíveis são indicados como tal.

## 👥 Jogar com amigos

Encontre pessoas e acompanhe suas conversas sem sair do aplicativo:

- Pesquisa por gamertag.
- Lista de amigos e sugestões de amizade.
- Foto do usuário na lista e no perfil.
- Solicitação de amizade com indicação **Amizade solicitada** após o pedido ser aceito pelo serviço.
- Opção de seguir com indicação **Seguindo**.
- Histórico de mensagens, envio de texto e atualização da conversa.
- Carregamento de mensagens anteriores quando disponibilizadas pelo serviço, dentro do limite de memória do aplicativo.
- **Ver perfil no Xbox** abre a página oficial daquela pessoa dentro do app.

Grupos, convites, bloqueio e demais controles oficiais ficam acessíveis pela área Xbox incorporada ao aplicativo. Essas operações são realizadas na interface original do serviço, que pode solicitar um primeiro login separado. A disponibilidade depende das permissões e dos recursos da conta.

A interface permanece limpa, **sem atalho social flutuante sobre a biblioteca ou o streaming**. Na página oficial, pressione **Voltar** para acessar os botões superiores; **Menu** também leva o foco aos controles.

## 🎙️ Microfone e voz sob seu controle

- Ativação do microfone nas configurações, com solicitação de permissão ao Android.
- Suporte à captura de voz no jogo quando a sessão e o título permitirem.
- Teste local do microfone e consulta ao último estado de voz.
- Informações de voz opcionais, **desligadas por padrão**.
- Separação entre a captura do jogo e a captura reservada à área social, evitando disputar o mesmo microfone.

É necessário um microfone ou dispositivo de entrada de áudio reconhecido pela TV. A comunicação em grupo utiliza a área oficial incorporada; sua continuidade ao recolher a página depende do WebView. Fechar o social ou colocar o aplicativo em segundo plano encerra essa página e sua captura.

## 🌍 Idioma e conta

- Idioma automático conforme o sistema.
- Português, inglês e espanhol selecionáveis nas configurações.
- Login lembrado no aparelho, com restauração da sessão salva enquanto a credencial permanecer válida.
- Opção **Sair da conta** para desconectar e limpar os dados locais associados.
- Autenticação sem campo de Client ID exposto ao usuário.

## 🚀 Comece a jogar

1. Instale o APK correspondente à arquitetura do aparelho: **ARM32** ou **ARM64**.
2. Abra o BXbox Cloud TV e autorize sua conta Microsoft/Xbox.
3. Aguarde a atualização do catálogo.
4. Escolha um jogo e selecione **Jogar**.
5. Ajuste motor, resolução, bitrate, codec e servidor conforme o aparelho e a conexão.

Para aparelhos com poucos recursos, o **Perfil leve** é um ponto de partida. O acesso aos jogos exige uma conta elegível e as assinaturas, compras ou permissões necessárias para cada título.

## 🛠️ Código aberto e compilação pelo GitHub

O projeto inclui código-fonte, Gradle Wrapper e workflow do GitHub Actions para gerar APKs ARM32 e ARM64.

- [Passo a passo para gerar e atualizar o APK](docs/GUIA_GITHUB.md)
- [Notas da versão 0.7](docs/ALTERACOES_0.7.md)
- [Testes executados e limites de validação](docs/VALIDACAO.md)

Para compilar localmente: Java 17, Android SDK 35, Build Tools 35.0.0 e Gradle 8.9.

```bash
node tools/test-player.cjs
bash gradlew :app:testLiteUnitTest :app:lintLite :app:assembleLite
```

Os APKs são gerados em `app/build/outputs/apk/lite/`. Para atualizar uma instalação mantendo seus dados, utilize um APK assinado com a mesma chave.

## ℹ️ Sobre o projeto

**BXbox Cloud TV é um projeto independente, sem afiliação com Microsoft ou Xbox.** Não fornece jogos, assinaturas ou licenças e não substitui os serviços oficiais.

O foco em aparelhos de 1 GB não representa garantia de desempenho em todos os modelos. Qualidade, fluidez, codecs, áudio e recursos sociais dependem do hardware, da conexão e dos serviços do Xbox. As integrações autenticadas e a voz ainda exigem validação em aparelhos e contas reais; os testes locais estão documentados acima.

Código distribuído sob **AGPL-3.0**, com créditos às adaptações de protocolo do [XStreaming](https://github.com/Geocld/XStreaming). Consulte [LICENSE](LICENSE) e [NOTICE.md](NOTICE.md). Preserve a licença, os créditos e o código correspondente ao redistribuir o projeto.
