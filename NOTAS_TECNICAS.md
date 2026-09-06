# Notas técnicas — v1.1.0

- applicationId: `br.com.treinoabc`
- versionCode: 2
- versionName: `1.1.0`
- minSdk: 28
- targetSdk/compileSdk: 36
- Health Connect SDK: `androidx.health.connect:connect-client:1.2.0-alpha06`
- Interface: HTML/CSS/JS empacotada localmente em WebView, com bridge nativa Kotlin para Health Connect.
- Nenhum conteúdo remoto é carregado na WebView.
- A permissão do Health Connect é solicitada somente por ação do usuário.
- O histórico local usa `localStorage` da WebView.
- O mesmo storage key da v1.0 (`TreinoABC_native_v1`) foi mantido para preservar os dados existentes.
- A v1.1 passa a armazenar snapshots detalhados por sessão para alimentar os gráficos de evolução.
- A última carga de cada exercício é mantida em `state.lastLoads` e reaparece no treino seguinte.
- Histórico máximo local: 180 sessões.
- Métrica de volume: soma de `carga × repetições` somente nas séries marcadas como concluídas; exercícios sem carga registrada não contribuem para o volume.
- Gráficos são renderizados localmente com HTML/SVG, sem bibliotecas externas.
