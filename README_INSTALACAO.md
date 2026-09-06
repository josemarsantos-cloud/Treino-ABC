# Treino ABC — Android + Health Connect

Versão 1.1.0 do aplicativo de musculação Treino ABC.

## O que está implementado
- Treinos A, B e C completos.
- Ajuste de carga por exercício, com persistência da última carga para o treino seguinte.
- Campo de repetições por série.
- Finalização individual de cada série.
- Cronômetro de descanso ajustável (30, 45, 60, 90, 120 e 180 s, além de ±10 s).
- Cronômetro do tempo total de treino.
- Finalização e histórico local dos treinos.
- Integração com Health Connect (Conexão Saúde).
- Gravação como sessão de `Strength Training`.
- Tentativa de gravar repetições, carga e índice da série usando Health Connect 1.2.0-alpha06.
- Fallback automático para uma sessão básica quando o provedor Health Connect do aparelho ainda não aceitar os campos detalhados.

## Minha Evolução
A versão 1.1.0 acrescenta uma tela de acompanhamento com:
- quantidade de treinos nos últimos 30 dias;
- frequência média semanal;
- volume total dos últimos 30 dias (carga × repetições das séries concluídas);
- total de treinos e séries registrados;
- gráfico de frequência das últimas 6 semanas;
- gráfico de volume dos últimos 8 treinos com carga registrada;
- seletor de exercício e gráfico de progressão de carga das últimas 10 sessões daquele exercício;
- recordes pessoais de maior carga e melhor volume por exercício;
- histórico detalhado das sessões;
- toque em uma sessão para abrir séries, repetições, cargas e volume daquele treino.

### Compatibilidade com histórico antigo
Sessões criadas na versão anterior continuam visíveis e entram nas métricas gerais quando a data estiver disponível. Como a versão anterior não armazenava um retrato completo de cada exercício ao finalizar, esses registros antigos não conseguem alimentar retrospectivamente os gráficos de carga/volume por exercício. Os novos treinos passam a guardar esse detalhamento automaticamente.

## Abrir no Android Studio
1. Instale/abra uma versão atual do Android Studio.
2. Use **File > Open** e escolha a pasta `TreinoABC_Android`.
3. Aguarde o Android Studio baixar Gradle/dependências e sincronizar o projeto.
4. Se solicitado, instale o Android SDK API 36.
5. Conecte seu celular Android por USB com Depuração USB habilitada, ou use um emulador.
6. Clique em **Run ▶**.

## Health Connect
O app solicita somente a permissão `WRITE_EXERCISE`.
- Android 14 ou superior: Health Connect faz parte do sistema.
- Android 13 ou inferior: pode ser necessário instalar/atualizar o app Health Connect pela Play Store.

Na primeira vez, toque em **Conectar Health** e autorize a gravação de exercícios.

## Dados e privacidade
Os dados necessários para a tela **Minha Evolução** ficam no armazenamento local privado da WebView do aplicativo (`localStorage`). Somente ao autorizar o Health Connect o app envia a sessão de musculação ao repositório do Health Connect no aparelho.

## Observação sobre o Gradle Wrapper
O arquivo `gradle-wrapper.properties` está configurado para Gradle 8.13. O JAR do wrapper não está incluído neste pacote. O Android Studio pode usar sua configuração local de Gradle; se quiser o wrapper completo, execute `gradle wrapper --gradle-version 8.13` em um ambiente que tenha Gradle instalado.

## Gerar APK automaticamente pelo GitHub
O projeto inclui `.github/workflows/build-apk.yml`.
1. Crie um repositório no GitHub e envie esta pasta.
2. Abra a aba **Actions** do repositório.
3. Execute o workflow **Build APK** (ou faça um push na branch `main`).
4. Ao final, baixe o artefato **TreinoABC-debug-apk**.
5. Dentro dele estará `app-debug.apk`, que pode ser instalado no Android para testes.

Para publicação na Play Store, crie depois uma configuração de assinatura de release e cumpra a declaração de apps de saúde exigida pelo Google Play.
