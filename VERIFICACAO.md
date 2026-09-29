# Verificação da versão 1.1

Executada em 29/09/2026.

- `assembleDebug`: concluído com sucesso.
- `testDebugUnitTest`: 17 testes aprovados, zero falhas, erros ou testes ignorados.
- `lintDebug`: concluído, zero erros e três avisos descritos abaixo.
- Assinatura do APK validada por `apksigner` (APK Signature Scheme v2).
- Manifesto do APK conferido: pacote `br.com.bloqueiototal`, versão 1.1 (`versionCode` 2), mínimo API 29, alvo API 36, única permissão solicitada `android.permission.READ_CONTACTS`.

A chave debug da versão 1.0 não está disponível e o certificado do APK 1.1 é diferente. Por isso, a versão anterior precisa ser desinstalada antes da instalação deste APK.

Os avisos de lint são: versão mais recente disponível do Robolectric; sugestão de configurar `fullBackupContent` também para Android 10/11 (nessas versões o projeto já desativa backup com `allowBackup=false`); e sugestão genérica de tornar telefonia opcional (este app exige um dispositivo com telefonia por sua finalidade).

Os testes verificam os dois modos, contatos salvos/não salvos/indeterminados, os três flags de bloqueio, liberação com o switch desligado, preservação de chamadas de saída e outros esquemas, estado persistente, ativação com papel/permissão, ativação sem Contatos no modo de números não salvos e atualização do status após revogação.

Não foram executados testes em Samsung físico, rede Claro, ligação real, dois SIMs ou WhatsApp. O README contém o roteiro para validar esses casos. Não há garantia de rejeição de chamadas que o Android não entrega ao serviço, como certas chamadas ocultas/restritas, nem de supressão de avisos independentes da operadora.

Ferramentas utilizadas: JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.2.21, SDK 36 e Build Tools 35.0.0. A execução local usou um único trabalhador e memória reduzida devido aos recursos disponíveis no computador; downloads e temporários de teste foram direcionados ao disco D:.
