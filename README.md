# Bloqueio Total

Aplicativo Android nativo mínimo em Kotlin. Permite escolher entre bloquear somente números não salvos ou todas as ligações telefônicas **recebidas pelo CallScreeningService**, incluindo contatos. Android 10 ou superior; compilação e alvo Android 16 (API 36).

**[Baixar APK de desenvolvimento](https://github.com/ThHSzR/call-blocker/raw/refs/heads/main/apk/BloqueioTotal-debug.apk)** · [Verificação e testes](VERIFICACAO.md) · [SHA-256 do APK](apk/SHA256SUMS.txt)

O código-fonte completo está na raiz deste repositório. O APK é uma versão debug para instalação manual; ainda precisa de validação com chamadas reais no Samsung.

**Atualização da versão 1.0:** desinstale o APK antigo antes de instalar a versão 1.1. A chave debug usada na compilação anterior não está disponível, portanto o Android não aceita a instalação direta por cima. Depois, selecione novamente o app como filtro e escolha o modo desejado.

## Instalar no Samsung

1. Transfira `BloqueioTotal-debug.apk` para o celular e abra em **Meus Arquivos**.
2. Se solicitado, permita que **Meus Arquivos** instale apps desconhecidos. Se o Samsung indicar que o **Bloqueador automático** impediu a instalação, desative-o temporariamente em **Configurações > Segurança e privacidade > Bloqueador automático**. Depois da instalação, reative a proteção e retire a autorização de instalação da fonte.
3. Abra **Bloqueio Total**, escolha **Somente números não salvos** ou **Todas as ligações, incluindo contatos** e ligue **Ativar bloqueio**.
4. Na janela do Android, escolha **Bloqueio Total** como app de identificação/filtro de chamadas e confirme. O app não pode assumir esse papel sem sua seleção.
5. Se escolher bloquear todas, autorize **Contatos**. O Android usa essa permissão para entregar chamadas de números salvos ao filtro. No modo de números não salvos, essa permissão é opcional.
6. Confira a mensagem de status correspondente ao modo escolhido.

Você também pode usar os botões **Definir como filtro de chamadas** e, no modo que inclui contatos, **Incluir contatos no filtro** antes de ligar o switch. Se uma permissão necessária for negada, a ativação inicial não é concluída. Se o Android não mostrar mais a solicitação de Contatos, o app oferece um atalho para **Permissões > Contatos**; conceda, volte e ligue o switch novamente.

### Seleção manual do filtro

Em versões compatíveis da One UI, procure **Configurações > Aplicativos > Escolher aplicativos padrão > App de identificação de chamadas e spam** (o texto varia por versão) e escolha **Bloqueio Total**. Use a busca de Configurações por “identificação de chamadas” ou o botão dentro do app caso esse caminho varie. Mantenha o aplicativo Telefone habitual como discador; este projeto solicita somente `ROLE_CALL_SCREENING`. Outro identificador/filtro padrão deixa de ocupar esse papel.

Para desativar, desligue o switch. Para remover, desinstale normalmente. Se trocar o filtro padrão por outro app, Bloqueio Total deixa de receber as chamadas.

## O que faz

- Usa `CallScreeningService` protegido por `BIND_SCREENING_SERVICE` no manifesto, com serviço exportado para o Android.
- Solicita `RoleManager.ROLE_CALL_SCREENING`. A permissão `READ_CONTACTS` é necessária para bloquear também os contatos e opcional no modo que bloqueia somente números não salvos.
- No modo **Todas as ligações**, rejeita toda chamada de entrada com esquema `tel` entregue pelo Android.
- No modo **Somente números não salvos**, libera contatos identificados pelo Android ou encontrados na agenda e rejeita apenas números confirmados como não salvos. Se a consulta à agenda falhar, a chamada é liberada por segurança.
- Ao rejeitar, responde com `setDisallowCall(true)`, `setRejectCall(true)` e `setSkipNotification(true)`.
- Desligado, responde permitindo a chamada; chamadas de saída e de direção desconhecida não são rejeitadas. Esquemas diferentes de `tel` são permitidos.
- Persiste o switch e o modo escolhido em `SharedPreferences` no armazenamento protegido do dispositivo. O modo padrão é **Todas as ligações**, preservando o comportamento da versão anterior. O serviço declara suporte a Direct Boot; a entrega real de chamadas antes do primeiro desbloqueio depende do sistema.
- Responde de forma síncrona e sem rede dentro do caminho de execução do callback, cujo prazo da plataforma é de cinco segundos. A consulta local à agenda só ocorre no modo de números não salvos quando a permissão está concedida.
- Não pede internet, acessibilidade, VPN, acesso às notificações, registro de chamadas ou papel de discador padrão. Não modifica dados móveis, Wi-Fi, WhatsApp ou o modo Não Perturbe. Não cria notificações próprias nem telas de chamadas.
- Mantém o histórico de chamadas bloqueadas do sistema (`setSkipCallLog(false)`) para diagnóstico. Não salva números ou contatos no app.

## Limites reais

**“Todas” significa todas as chamadas telefônicas de entrada que o Android entrega a este filtro. Não existe garantia de bloqueio absoluto com esta API.** Números ocultos, restritos, indisponíveis e outras apresentações excluídas pela plataforma podem não ser entregues ao serviço. O app não tem como rejeitar chamadas que não recebe. O bloqueio nativo de números privados/desconhecidos no aplicativo Telefone pode ser um complemento, quando disponível; precisa ser testado na sua One UI.

`setSkipNotification(true)` solicita a supressão da notificação de chamada perdida daquela chamada bloqueada. Isso não suprime todas as notificações do celular, o histórico de bloqueios, SMS da operadora ou avisos independentes de correio de voz. A rejeição pode resultar em caixa postal conforme o serviço da operadora.

Se Contatos for revogada no modo **Todas as ligações**, chamadas de números salvos podem passar; a tela mostra **Bloqueio parcial** quando reaberta. No modo **Somente números não salvos**, a permissão não é obrigatória porque o Android normalmente entrega ao filtro apenas chamadas fora da agenda quando ela não foi concedida. Se o papel for removido, a tela mostra que o app não está bloqueando. O switch representa a preferência salva, e o texto abaixo mostra a situação efetiva. A plataforma pode impor exceções e indisponibilidades, incluindo tratamento de emergência, tempo limite e suspensão/forçar parada do app. Não dependa deste app para impedir retornos de serviços de emergência.

Após reiniciar, desbloqueie o aparelho e confira a configuração. Se houver falhas com a tela apagada, confira as permissões, o filtro padrão e se a One UI colocou o app em suspensão profunda. Os nomes desses ajustes dependem do modelo. Teste antes de depender do bloqueio.

## Testar no aparelho

Use outro telefone para testar; não ligue para serviços de emergência.

| Teste | Resultado esperado |
|---|---|
| Modo **Somente números não salvos**, ligação de número não salvo | Rejeição sem toque/tela/notificação de perdida pelo filtro |
| Mesmo modo, ligação de contato salvo | Chamada permitida pelo app |
| Modo **Todas as ligações**, papel e Contatos concedidos, contato salvo ou não | Rejeição pelo filtro |
| Switch desligado | Chamada permitida pelo app; outros bloqueios do celular ainda podem atuar |
| Ligação recebida pelo WhatsApp e navegação em 4G/5G | Funcionamento preservado |
| Ligação telefônica feita pelo próprio Samsung | Funcionamento preservado |
| Tela bloqueada, app fora da tela de recentes | Confirmar bloqueio |
| Reinício e primeiro desbloqueio | Switch preservado; repetir chamada de teste |
| Dois SIMs, se usados | Repetir os testes para cada linha; não há seleção de SIM no código |
| Revogar Contatos e reabrir o app | Mensagem de bloqueio parcial |
| Remover papel padrão e reabrir o app | Mensagem de filtro não definido |
| Número oculto e caixa postal | Medir a limitação real do aparelho/operadora |

Os testes locais automatizados não simulam rádio, Claro, WhatsApp ou firmware Samsung. A validação final desses comportamentos é feita no aparelho.

Em 29/09/2026, os 17 testes automatizados passaram: 12 verificações da regra/resposta e persistência em Android 10 e 15, e 5 verificações do fluxo de ativação, seleção e status em Android 15. O APK de desenvolvimento 1.1 foi compilado e sua assinatura validada. Não houve teste em Samsung físico.

## Abrir e compilar

Projeto completo com wrapper Gradle. Abra esta pasta em Android Studio compatível com AGP 8.13.2. Use **JDK 17**, **SDK Platform 36** e **Build Tools 35.0.0**. O Gradle 8.13 e as dependências são baixados na primeira compilação. Não requer bibliotecas de UI externas; utiliza widgets nativos.

Configure o SDK pelo Android Studio, por `ANDROID_HOME` ou por um `local.properties` local (não incluído no ZIP):

```properties
sdk.dir=C\:/Users/SEU_USUARIO/AppData/Local/Android/Sdk
```

No Windows, dentro da pasta do projeto:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

No Linux/macOS:

```sh
chmod +x gradlew
./gradlew assembleDebug testDebugUnitTest lintDebug
```

O APK é gerado em `app/build/outputs/apk/debug/app-debug.apk`. A variante debug é assinada automaticamente com a chave de desenvolvimento local e serve para instalação manual. A chave não é distribuída. Ao recompilar em outro computador, uma assinatura diferente pode exigir desinstalar a versão anterior, apagando seu estado. Para distribuição duradoura, gere uma versão release com sua própria chave de assinatura no Android Studio.

Instalação opcional com as ferramentas Android e depuração USB autorizada:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

As confirmações do filtro e da permissão continuam sendo feitas no celular.

### Arquivos principais

- `MainActivity.kt`: interface, seleção do modo, papel padrão, permissão de contatos e atualização do status.
- `BlockAllCallsService.kt`: identificação de contatos, resposta ao Android e regra dos dois modos.
- `BlockingPreferences.kt`: switch e modo persistentes, com o bloqueio desligado na primeira instalação.
- `ScreeningTest.kt`: respostas para contatos salvos, não salvos e indeterminados, direção, esquemas e persistência, em Android 10 e 15 simulados pelo Robolectric.
- `ActivationTest.kt`: ativação dos dois modos, reabertura do app, falta de Contatos e atualização do status após revogação, em Android 15 simulado.

## Referências oficiais

- [CallScreeningService: papel, prazo, contatos e escopo](https://developer.android.com/reference/android/telecom/CallScreeningService)
- [CallResponse.Builder: rejeição e supressão de notificação](https://developer.android.com/reference/android/telecom/CallScreeningService.CallResponse.Builder)
- [RoleManager e seleção do papel](https://developer.android.com/reference/android/app/role/RoleManager)
- [Código AOSP e apresentações excluídas](https://android.googlesource.com/platform/prebuilts/fullsdk/sources/android-31/+/refs/heads/main/android/telecom/CallScreeningService.java)
- [Samsung: Bloqueador automático e instalação externa](https://www.samsung.com/us/support/answer/ANS10003636/)
- [Compatibilidade do AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
