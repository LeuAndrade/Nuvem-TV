# Guia completo: do zero ao APK, pelo navegador

Este projeto cria um repositório novo. Não use Fork, Import repository nem duplique o XStreaming. Os componentes adaptados estão creditados sob AGPL-3.0.

## 1. Entenda os arquivos recebidos

- `NuvemTV/`: projeto completo, com pastas e workflow.
- `APKs/`: APKs experimentais já compilados nesta entrega.
- `instalar_projeto.py`: instalador que contém todos os arquivos do projeto. Não baixa código de outro repositório. Use-o uma vez dentro do seu repositório novo.
- `CODIGO_COMPLETO.txt`: arquivos de texto do projeto reunidos; o binário do Gradle Wrapper vai no ZIP e no instalador.
- `workflow-android.txt`: conteúdo pronto para colar no GitHub Actions.

Extraia o ZIP no aplicativo de arquivos do seu computador ou celular. Enviar o ZIP fechado ao GitHub **não** transforma o ZIP num projeto Android.

## 2. Crie a conta e o repositório

1. Abra https://github.com e entre ou crie sua conta.
2. Abra https://github.com/new.
3. Em Repository name, escreva `nuvem-tv`.
4. Selecione Public se quiser facilitar o compartilhamento do código AGPL.
5. Marque Add a README file. Não escolha template e não importe outro repositório.
6. Clique Create repository.

## 3. Envie somente o instalador

1. Na aba Code do seu repositório, procure Add file > Upload files.
2. Escolha `instalar_projeto.py` entre os arquivos extraídos.
3. Clique Commit changes e confirme a gravação na branch `main`.
4. Você deve ver README.md e instalar_projeto.py na página.

No iPhone, o seletor abre Arquivos. Se os botões não aparecerem, use a opção do Safari de solicitar site para computador. A interface pelo computador é mais confortável, mas o processo usa somente navegador.

## 4. Abra o editor do GitHub

1. Na aba Code, clique no botão verde Code.
2. Selecione Codespaces > Create codespace on main.
3. Aguarde abrir o editor VS Code no navegador.
4. Se o terminal não estiver visível, abra o menu Terminal > New Terminal. No celular esse menu pode estar em `☰`.

Codespaces é um ambiente de desenvolvimento na nuvem do GitHub. Se sua conta não tiver acesso ou cota disponível, não há como eu liberá-lo. Não aceite cobrança sem entender seu plano. Alternativa: no computador, envie os arquivos extraídos e suas pastas com Add file > Upload files; confira os arquivos ocultos `.github` e `.gitignore`. Depois siga a etapa do workflow abaixo.

## 5. Gere os arquivos na raiz

No terminal do Codespaces, cole e execute:

```bash
python3 instalar_projeto.py --sem-workflow --substituir
```

`--sem-workflow` deixa o Actions para a etapa seguinte, evitando depender de permissões de gravação de workflows no token do Codespaces. `--substituir` permite trocar o README inicial pelo README do aplicativo. Use em um repositório novo, conforme este guia.

Você deve ver `app`, `gradle`, `gradlew`, `settings.gradle`, `build.gradle`, `LICENSE` e outros arquivos **na raiz**. Não deixe tudo dentro de uma pasta NuvemTV dentro do repositório.

Salve no GitHub com estes comandos, um por linha:

```bash
git add .
git commit -m "Cria projeto Nuvem TV"
git push
```

Espere cada comando terminar. Se o Git pedir nome ou e-mail, configure seu nome e o e-mail noreply exibido em Settings > Emails da sua conta; então repita o commit. Não cole senhas ou tokens em arquivos do projeto.

## 6. Crie o workflow de compilação

1. Volte à página do repositório no GitHub e atualize.
2. Clique Add file > Create new file.
3. No nome, escreva exatamente `.github/workflows/android.yml`. As barras criam as pastas.
4. Abra `workflow-android.txt` da entrega e copie **todo o conteúdo**, sem os marcadores de bloco de código.
5. Cole no editor do GitHub.
6. Clique Commit changes e grave na branch `main`.

Se você subiu as pastas completas e o workflow já aparece no caminho certo, não crie uma segunda cópia. Use Actions > Gerar APK Nuvem TV > Run workflow para iniciar manualmente.

## 7. Aguarde a compilação

1. Clique na aba Actions.
2. Se aparecer pedido para habilitar workflows, habilite os workflows deste seu repositório.
3. Abra `Gerar APK Nuvem TV`.
4. Abra a execução mais recente. Amarelo significa executando; verde significa sucesso; vermelho significa falha.
5. A primeira execução baixa dependências e pode demorar vários minutos. Não feche/recrie o repositório para tentar acelerar.
6. Se ficar vermelha, abra o job `android` e a etapa que falhou. O APK só é publicado se testes, Lint e compilação concluírem.

## 8. Baixe o APK

1. Abra a execução verde e role até Artifacts.
2. Baixe `NuvemTV-APKs-e-codigo` estando conectado à conta GitHub.
3. Extraia o ZIP baixado. Procure `app/build/outputs/apk/lite/`.
4. `app-armeabi-v7a-lite.apk` é para Android ARM de 32 bits.
5. `app-arm64-v8a-lite.apk` é para Android ARM de 64 bits.
6. Um processador anunciado como 64 bits pode rodar Android de 32 bits. Se houver erro de ABI, use o APK de 32 bits ou consulte as especificações do sistema. Não tente APK de outra origem para corrigir.

O pacote também contém `fonte-correspondente.zip`, com o código da revisão que gerou esses APKs. Mantenha-o junto do APK ao compartilhar. O relatório de testes e Lint fica no artifact `NuvemTV-verificacoes`.

Os artifacts expiram após o prazo configurado (30 dias aqui). Guarde seu download ou execute o workflow novamente.

## 9. Instale na Android TV

1. Transfira o APK correto à TV por pendrive ou por um meio de transferência em que você confie.
2. Abra o arquivo num gerenciador de arquivos da TV.
3. Se o Android solicitar, permita que **esse gerenciador** instale aplicativos de fontes externas.
4. Confirme a instalação e abra Nuvem TV.
5. Conecte um controle físico Bluetooth ou USB. O controle remoto navega a interface; não substitui um gamepad nos jogos.

As opções para fontes externas variam por fabricante e versão. A aplicação requer Android 7.0 ou mais recente. Não exige root.

Se o Android disser que uma atualização conflita com a assinatura, a compilação anterior usou outra chave de desenvolvimento. Desinstale a anterior e instale a nova, sabendo que o login e preferências locais serão apagados. Para distribuir atualizações sem desinstalar, configure uma assinatura permanente e proteja a chave; nunca a coloque no repositório.

## 10. Faça o primeiro teste

1. Abra Configurações e aplique o perfil leve.
2. Deixe Native, H264, 720p, 5 Mb/s e servidor Automático.
3. Clique Entrar na conta. No celular, abra o endereço **mostrado pelo aplicativo** e digite o código. Confira que o login acontece num domínio Microsoft.
4. Após autorizar, volte ao aplicativo e clique Atualizar catálogo. Aguarde as consultas terminarem.
5. Abra um jogo disponível para sua conta e aceite a fila se houver.
6. Selecione Configurações > Estatísticas antes da próxima partida para medir. Trocar ajustes exige iniciar nova sessão.
7. Se a imagem travar, compare Native e WebView no mesmo jogo e rede. Não conclua pelo tamanho do APK que o decode será rápido.
8. Para sair de uma partida, use Voltar no controle remoto e confirme Encerrar.

O atalho Xbox.com/play usa o site oficial e seu próprio login. Os ajustes do player Nuvem TV não são injetados nesse site.

## 11. Interprete os resultados

- Sem decoder de hardware: o modo Native recusa software. Tente H264; se não houver suporte, esse aparelho não atende àquele caminho de vídeo.
- Vídeo preto só no Native: tente desligar a solicitação de baixa latência; reinicie a sessão. Compare WebView e atualize o System WebView quando houver atualização compatível na TV.
- Falha de login/token: confira conta, região e consentimento. O protocolo é não público e pode mudar. Não se corrige isso publicando senha ou token.
- Catálogo vazio: confira login e conexão; o servidor pode ter mudado o formato. Envie apenas o texto do erro, sem códigos de login ou tokens.
- Decode alto: teste 720p/H264, depois 3 Mb/s; desligue pós-processamento da TV quando aplicável. O app já não tem filtros. Uma TV sem hardware/driver adequado pode continuar lenta.
- Imagem boa mas comando atrasado: rede, servidor distante, Bluetooth e processamento da TV também podem contribuir. A média de decode não mede o atraso total.

## 12. Depois de terminar

Volte à lista de Codespaces da sua conta e pare o ambiente para não deixá-lo consumindo cota. O código continua no repositório. Para gerar outro APK, use Actions > Run workflow; não precisa recriar tudo.
