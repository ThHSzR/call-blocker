# Verificação da versão 1.0

Executada em 28/09/2026.

- `assembleDebug`: concluído com sucesso.
- `testDebugUnitTest`: 13 testes aprovados, zero falhas, erros ou testes ignorados.
- `lintDebug`: concluído, zero erros e quatro avisos descritos abaixo.
- Assinatura do APK validada por `apksigner` (APK Signature Scheme v2).
- Manifesto do APK conferido: pacote `br.com.bloqueiototal`, versão 1.0, mínimo API 29, alvo API 36, única permissão solicitada `android.permission.READ_CONTACTS`.

Os avisos de lint são: versões mais recentes disponíveis do Gradle e Robolectric; sugestão de configurar `fullBackupContent` também para Android 10/11 (nessas versões o projeto já desativa backup com `allowBackup=false`); e sugestão genérica de tornar telefonia opcional (este app exige um dispositivo com telefonia por sua finalidade).

Os testes verificam os três flags de bloqueio, liberação com o switch desligado, preservação de chamadas de saída e outros esquemas, estado persistente, ativação com papel/permissão, recusa de ativação incompleta e atualização do status após revogação.

Não foram executados testes em Samsung físico, rede Claro, ligação real, dois SIMs ou WhatsApp. O README contém o roteiro para validar esses casos. Não há garantia de rejeição de chamadas que o Android não entrega ao serviço, como certas chamadas ocultas/restritas, nem de supressão de avisos independentes da operadora.

Ferramentas utilizadas: JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.2.21, SDK 36 e Build Tools 35.0.0. A execução local usou um único trabalhador e memória reduzida devido aos recursos disponíveis no computador; downloads e temporários de teste foram direcionados ao disco D:.
