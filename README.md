# Prego Posts Bot

O bot tem uma única responsabilidade: publicar uma rom aleatória no canal do Telegram 3 vezes
por dia (09:00, 15:00 e 21:00). Ele não recebe nem responde comandos.

O cadastro de roms acontece fora do bot, em duas etapas manuais:

1. `uploadRoms` — sobe as roms para o Dropbox, encurta o link e grava no `roms/{console}/__data.json`.
2. `importRoms` — lê os `__data.json` já preenchidos e popula o `jsons/roms.json` que o bot publica.

## 1. Upload das roms para o Dropbox

```bash
./gradlew uploadRoms \
  -Pconsole=gba \
  -PdropboxAppKey=... \
  -PdropboxAppSecret=... \
  -PdropboxRefreshToken=... \
  -PshrinkApiToken=seu_token_shrink
```

`-PdropboxAccessToken=...` pode substituir os três `dropboxApp*` acima (mais simples, mas expira
em ~4h). Sem `-Pconsole=`, processa todo console que tiver um `roms/{console}/__data.json`.
`-Plimit=N` processa só N roms novas (útil pra testar antes de rodar o catálogo inteiro).
Idempotente: pode rodar de novo a qualquer momento, roms já encurtadas são puladas.

## 2. Importar para o bot

```bash
./gradlew importRoms
```

Popula `jsons/roms.json` a partir das roms que já têm link, criando o console correspondente se
necessário. Também idempotente — roms já importadas são puladas.

## 3. Rodar o bot

```bash
# Buildar o JAR (requer JDK 21+)
./gradlew shadowJar

# Executar
java -jar build/libs/postsroms-bot-1.0.0.jar \
  --botToken=seu_token \
  --targetChatId=id_do_canal
```

Para manter rodando em segundo plano com `nohup`:

```bash
nohup java -jar build/libs/postsroms-bot-1.0.0.jar \
  --botToken=seu_token \
  --targetChatId=id_do_canal > bot.log 2>&1 &
```
# pack-utilities
