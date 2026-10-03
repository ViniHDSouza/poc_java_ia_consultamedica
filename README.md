# poc_java_ia_consultamedica

POC didática em **Java 21 + Spring Boot + LangChain4j + OpenRouter** para estudar como transformar uma mensagem em linguagem natural em uma ação de negócio controlada pelo Java.

O projeto foi refeito a partir do projeto e das transcrições do Módulo 03 fornecidos pelo aluno, mas com foco explícito em **entender o conceito e conseguir replicá-lo**.

---

## 1. O conceito principal

Não pense primeiro em LangChain4j, Spring ou OpenRouter. Pense neste fluxo:

```text
Mensagem escrita por uma pessoa
        ↓
IA identifica a intenção
        ↓
IA extrai dados para um JSON previsível
        ↓
JSON vira objeto Java
        ↓
Java valida os dados
        ↓
Java executa a regra de negócio
        ↓
IA gera uma resposta amigável
```

Exemplo:

```text
"Meu nome é Maria Santos. Quero marcar uma consulta com
Dr. Alicio da Silva amanhã às 16h para um consulta de rotina."
```

A IA não agenda a consulta diretamente. Primeiro ela transforma a frase em algo semelhante a:

```json
{
  "intencao": "agendar",
  "idProfissional": 1,
  "nomeProfissional": "Dr. Alicio da Silva",
  "dataHora": "2026-10-03T16:00:00Z",
  "nomePaciente": "Maria Santos",
  "motivo": "consulta de rotina"
}
```

Depois disso, o código Java valida os dados e executa o agendamento.

> Regra mental: **a IA interpreta; o Java decide e executa.**

---

## 2. As três intenções do projeto

Esta POC entende exatamente três intenções.

### 2.1 `agendar`

Use quando o paciente deseja criar uma nova consulta.

Exemplo:

```text
Meu nome é Maria Santos. Quero marcar uma consulta com
Dr. Alicio da Silva amanhã às 16h para um consulta de rotina.
```

Dados que a IA tenta extrair:

```json
{
  "intencao": "agendar",
  "idProfissional": 1,
  "nomeProfissional": "Dr. Alicio da Silva",
  "dataHora": "2026-10-03T16:00:00Z",
  "nomePaciente": "Maria Santos",
  "motivo": "consulta de rotina"
}
```

Para realmente agendar, o Java exige:

- profissional;
- data e hora;
- nome do paciente.

O motivo pode ser omitido.

Fluxo:

```text
EtapaIdentificarIntencao
        ↓
AGENDAR
        ↓
EtapaAgendarConsulta
        ↓
AgendaConsultasService.agendarConsulta(...)
        ↓
EtapaGerarRespostaPaciente
```

### 2.2 `cancelar`

Use quando o paciente deseja cancelar uma consulta existente.

Exemplo:

```text
Sou João da Silva. Quero cancelar minha consulta com
Dr. Alicio da Silva hoje às 11h.
```

Saída esperada da IA:

```json
{
  "intencao": "cancelar",
  "idProfissional": 1,
  "nomeProfissional": "Dr. Alicio da Silva",
  "dataHora": "2026-10-02T11:00:00Z",
  "nomePaciente": "João da Silva",
  "motivo": null
}
```

O Java procura uma consulta que combine:

```text
profissional + data/hora + paciente
```

Se encontrar, remove a consulta da lista em memória.

Fluxo:

```text
EtapaIdentificarIntencao
        ↓
CANCELAR
        ↓
EtapaCancelarConsulta
        ↓
AgendaConsultasService.cancelarConsulta(...)
        ↓
EtapaGerarRespostaPaciente
```

### 2.3 `desconhecida`

Use quando a mensagem não pede agendamento nem cancelamento.

Exemplo:

```text
Qual é a previsão do tempo para hoje?
```

Saída esperada:

```json
{
  "intencao": "desconhecida",
  "idProfissional": null,
  "nomeProfissional": null,
  "dataHora": null,
  "nomePaciente": null,
  "motivo": null
}
```

Nenhuma regra de negócio é executada.

O fluxo pula diretamente para a geração de uma resposta explicando que esta POC sabe ajudar com agendamento e cancelamento de consultas.

---

## 3. O workflow completo

```text
POST /chat
   ↓
AtendimentoConsultaController
   ↓
FluxoAtendimentoConsulta.processarMensagemDoPaciente()
   ↓
EtapaIdentificarIntencao
   ↓
LangChain4j + OpenRouter
   ↓
DadosIntencaoAtendimento
   ↓
        ┌───────────────┬────────────────┬────────────────────┐
        │ AGENDAR       │ CANCELAR       │ DESCONHECIDA       │
        ↓               ↓                ↓                    │
EtapaAgendar       EtapaCancelar     nenhuma ação            │
        │               │                │                    │
        └───────────────┴────────────────┴────────────────────┘
                                ↓
                    EtapaGerarRespostaPaciente
                                ↓
                       LangChain4j + OpenRouter
                                ↓
                         resposta ao cliente
```

---

## 4. Como o LangChain4j é usado

### 4.1 O que é responsabilidade dele neste projeto

O LangChain4j é usado como uma camada Java para conversar com o modelo de IA.

Ele ajuda em quatro pontos:

1. representar o modelo por meio de `ChatModel`;
2. criar mensagens de sistema e usuário;
3. solicitar uma resposta usando JSON Schema;
4. enviar a requisição ao endpoint compatível com OpenAI do OpenRouter.

Ele **não** agenda consultas.

Ele **não** cancela consultas.

Ele **não** substitui `AgendaConsultasService`.

### 4.2 Onde ele é configurado

Arquivo:

```text
ConfiguracaoModeloInteligenciaArtificial.java
```

A aplicação cria um `OpenAiChatModel`:

```java
return OpenAiChatModel.builder()
        .baseUrl(urlBase)
        .apiKey(chaveApi)
        .modelName(nomeModelo)
        .temperature(0.2)
        .supportedCapabilities(Set.of(RESPONSE_FORMAT_JSON_SCHEMA))
        .strictJsonSchema(true)
        .build();
```

Por que `OpenAiChatModel` se estamos usando OpenRouter?

Porque o OpenRouter expõe uma API compatível com o formato da API da OpenAI. Mudamos a `baseUrl` para:

```text
https://openrouter.ai/api/v1
```

### 4.3 Onde a chamada realmente acontece

Arquivo:

```text
ClienteOpenRouterLangChain4j.java
```

O processo foi deixado propositalmente explícito para estudo.

#### Passo 1 — criar o JSON Schema

```java
JsonSchema jsonSchema = JsonSchema.builder()
        .name(nomeDoEsquema)
        .rootElement(JsonRawSchema.from(esquemaJson))
        .build();
```

O JSON Schema informa ao modelo o formato esperado.

#### Passo 2 — criar o formato da resposta

```java
ResponseFormat formatoDaResposta = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(jsonSchema)
        .build();
```

Isso é diferente de simplesmente escrever no prompt:

```text
"responda em JSON"
```

Com Structured Output, o formato é enviado como parte específica da requisição.

#### Passo 3 — montar as mensagens

```java
ChatRequest requisicaoParaIa = ChatRequest.builder()
        .messages(
                SystemMessage.from(promptDoSistema),
                UserMessage.from(promptDoUsuario)
        )
        .responseFormat(formatoDaResposta)
        .build();
```

`SystemMessage` explica o papel, regras e contexto.

`UserMessage` contém a solicitação atual do paciente.

#### Passo 4 — chamar o modelo

```java
ChatResponse respostaDaIa = modeloDeChat.chat(requisicaoParaIa);
```

Nesse momento o LangChain4j envia a requisição para o OpenRouter.

#### Passo 5 — transformar JSON em Java

```java
String jsonRecebido = respostaDaIa.aiMessage().text();

T objetoJava = conversorJson.readValue(
        jsonRecebido,
        tipoDaResposta
);
```

Exemplo conceitual:

```text
modelo de IA
   ↓
JSON
   ↓
Jackson
   ↓
DadosIntencaoAtendimento.java
```

### 4.4 Por que não usei AI Services do LangChain4j?

LangChain4j também possui uma API de nível mais alto chamada **AI Services**, que consegue gerar estruturas Java com menos código.

Nesta POC foi usada a API de baixo nível `ChatModel` de propósito.

O aluno consegue enxergar:

```text
prompt
→ schema
→ ChatRequest
→ chamada ao modelo
→ texto JSON
→ Jackson
→ objeto Java
```

Depois de entender esse processo, usar uma abstração maior fica muito mais fácil.

---

## 5. Structured Output: por que ele é importante

Sem uma estrutura definida, um modelo poderia responder:

```text
Claro! Parece que Maria quer marcar uma consulta amanhã...
```

Isso é agradável para uma pessoa, mas ruim para código Java.

A aplicação precisa de dados previsíveis:

```json
{
  "intencao": "agendar",
  "idProfissional": 1,
  "nomePaciente": "Maria Santos"
}
```

Por isso temos:

```text
EsquemasJsonSaidaEstruturada.java
```

O schema funciona como um contrato entre a aplicação e o modelo.

---

## 6. Por que validar novamente no Java?

Mesmo quando usamos Structured Output, a regra de negócio não deve confiar cegamente na IA.

Exemplo:

```text
Quero marcar uma consulta com a Dra. Ana.
```

A intenção pode ser corretamente identificada como:

```text
AGENDAR
```

Mas faltam:

```text
nome do paciente
data e hora
```

Por isso `EtapaAgendarConsulta` valida novamente:

```java
if (estado.getDataHora() == null || estado.getDataHora().isBlank()) {
    erros.add("A data e a hora da consulta não foram identificadas");
}
```

Esse padrão é importante:

```text
IA interpreta
    ↓
Java valida
    ↓
Java executa
```

---

## 7. Por que existe EstadoAtendimentoConsulta?

O estado é um objeto que passa por todas as etapas.

No início temos praticamente apenas:

```text
mensagemOriginalDoPaciente
```

Depois da IA:

```text
mensagemOriginalDoPaciente
intencao
idProfissional
nomeProfissional
dataHora
nomePaciente
motivo
```

Depois da regra de negócio:

```text
acaoExecutadaComSucesso
erroDaAcao
consultaCriada
```

E finalmente:

```text
mensagens
```

Ele funciona como uma pasta sendo enriquecida ao longo do workflow.

---

## 8. Intention-Revealing Names

Os nomes foram refatorados para comunicar responsabilidade.

Evitei nomes genéricos como:

```text
execute()
process()
handle()
Service1
Node2
```

Preferi nomes como:

```java
processarMensagemDoPaciente()
identificarIntencaoEExtrairDados()
agendarConsultaSolicitada()
cancelarConsultaSolicitada()
gerarMensagemFinalParaPaciente()
horarioEstaDisponivel()
localizarConsulta()
```

Ao ler:

```java
etapaAgendarConsulta.agendarConsultaSolicitada(estado);
```

já é possível entender a intenção sem abrir o método.

---

## 9. Principais classes e o que aprender em cada uma

| Classe | Responsabilidade | Conceito para aprender |
|---|---|---|
| `FluxoAtendimentoConsulta` | Coordena o fluxo | workflow e roteamento |
| `EstadoAtendimentoConsulta` | Guarda os dados do fluxo | estado compartilhado |
| `EtapaIdentificarIntencao` | Usa IA para extrair intenção/dados | classificação e extração |
| `DadosIntencaoAtendimento` | Representa o JSON da IA em Java | objeto tipado |
| `EsquemasJsonSaidaEstruturada` | Define o contrato JSON | Structured Output |
| `ClienteOpenRouterLangChain4j` | Chama o modelo | LangChain4j + OpenRouter |
| `EtapaAgendarConsulta` | Valida e pede o agendamento | regra de aplicação |
| `EtapaCancelarConsulta` | Valida e pede o cancelamento | regra de aplicação |
| `AgendaConsultasService` | Implementa a agenda | regra de negócio |
| `EtapaGerarRespostaPaciente` | Gera texto final | IA como interface de linguagem |
| `AtendimentoConsultaController` | Expõe `POST /chat` | API REST |

---

## 10. Ordem recomendada para estudar

Não comece pela configuração do OpenRouter.

Estude nesta ordem:

```text
1. FluxoAtendimentoConsulta
2. EstadoAtendimentoConsulta
3. IntencaoAtendimento
4. EtapaIdentificarIntencao
5. DadosIntencaoAtendimento
6. EsquemasJsonSaidaEstruturada
7. AgendaConsultasService
8. EtapaAgendarConsulta
9. EtapaCancelarConsulta
10. EtapaGerarRespostaPaciente
11. ClienteOpenRouterLangChain4j
12. AtendimentoConsultaController
13. testes
```

A ideia é entender primeiro **o problema**, depois **a IA**, e só no final os detalhes da integração.

---

## 11. Estrutura do projeto

```text
poc_java_ia_consultamedica/
├── pom.xml
├── README.md
├── .env.example
├── docs/
│   └── COMO_REPLICAR_O_CONCEITO.md
├── postman/
│   └── poc_java_ia_consultamedica.postman_collection.json
└── src/
    ├── main/
    │   ├── java/br/com/unipds/consultamedica/
    │   │   ├── ConsultaMedicaApplication.java
    │   │   ├── configuracao/
    │   │   ├── controller/
    │   │   ├── dominio/
    │   │   ├── prompt/
    │   │   ├── servico/
    │   │   └── workflow/
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/br/com/unipds/consultamedica/
```

---

## 12. Configurar o OpenRouter

Você precisa de uma chave de API do OpenRouter para executar as partes do projeto que usam IA.

> **Importante:** um arquivo chamado `.env` não é lido automaticamente pelo Java ou pelo Spring Boot. Nesta versão do projeto, o `application.properties` foi configurado explicitamente para carregar o `.env` da raiz do projeto.

### 12.1 Forma recomendada para estudar: arquivo `.env`

Na raiz do projeto existe:

```text
.env.example
```

Crie uma cópia chamada exatamente:

```text
.env
```

No Windows PowerShell, estando na pasta que contém o `pom.xml`:

```powershell
Copy-Item .env.example .env
```

Depois abra o `.env` e coloque sua chave real:

```properties
OPENROUTER_API_KEY=sk-or-v1-sua-chave-aqui
OPENROUTER_MODEL=openai/gpt-4o-mini
```

A estrutura da raiz deve ficar parecida com:

```text
poc_java_ia_consultamedica/
├── .env
├── .env.example
├── pom.xml
├── README.md
└── src/
```

Não coloque aspas em volta da chave e não deixe espaços antes ou depois do `=`.

**Não envie o `.env` para o Git.** Ele já está listado no `.gitignore`.

### 12.2 Como o Spring Boot encontra o `.env`?

Em `src/main/resources/application.properties` existe:

```properties
spring.config.import=optional:file:./.env[.properties]
```

Esse comando pode ser lido assim:

```text
optional:
    se o arquivo não existir, não falhe imediatamente por causa do arquivo

file:./.env
    procure .env no diretório de execução da aplicação

[.properties]
    trate o arquivo sem extensão como um arquivo de propriedades Java
```

Depois temos:

```properties
openrouter.api-key=${OPENROUTER_API_KEY:}
```

O fluxo é:

```text
.env
  ↓
OPENROUTER_API_KEY
  ↓
Spring Environment
  ↓
openrouter.api-key
  ↓
ConfiguracaoModeloInteligenciaArtificial
  ↓
OpenAiChatModel
```

Isso é importante para entender que **LangChain4j não lê o `.env`**. Quem resolve a configuração é o Spring Boot; depois a chave pronta é entregue ao LangChain4j.

### 12.3 Alternativa: variável de ambiente no Windows PowerShell

Se não quiser usar `.env`, configure a variável na sessão do PowerShell:

```powershell
$env:OPENROUTER_API_KEY="sua-chave"
$env:OPENROUTER_MODEL="openai/gpt-4o-mini"
```

Depois, **no mesmo terminal**, execute:

```powershell
mvn spring-boot:run
```

A variável criada com `$env:` vale para aquela sessão do terminal. Se fechar o PowerShell, normalmente será necessário configurá-la novamente.

### 12.4 Alternativa: IntelliJ IDEA

Se executar pela IDE e não quiser usar `.env`:

```text
Run
→ Edit Configurations
→ Environment variables
```

Adicione:

```text
OPENROUTER_API_KEY=sua-chave
```

Se estiver usando o `.env`, confirme também que o **Working directory** da configuração de execução aponta para a raiz do projeto, isto é, a pasta que contém:

```text
pom.xml
.env
```

### 12.5 Por que apareceu o erro "A variável OPENROUTER_API_KEY não foi configurada"?

O bean que cria o modelo recebe a chave desta propriedade:

```java
@Value("${openrouter.api-key}") String chaveApi
```

E o `application.properties` obtém esse valor de:

```properties
openrouter.api-key=${OPENROUTER_API_KEY:}
```

O `:` no final significa que, se `OPENROUTER_API_KEY` não existir, será usado um valor vazio.

Em seguida existe uma validação proposital:

```java
if (chaveApi == null || chaveApi.isBlank()) {
    throw new IllegalStateException(...);
}
```

Portanto, o erro não significa que `AtendimentoConsultaController`, `FluxoAtendimentoConsulta` ou `EtapaIdentificarIntencao` estejam quebrados. O Spring estava apenas mostrando a cadeia de dependências até chegar à verdadeira causa:

```text
Controller
  ↓ depende de
FluxoAtendimentoConsulta
  ↓ depende de
EtapaIdentificarIntencao
  ↓ depende de
ClienteOpenRouterLangChain4j
  ↓ depende de
ChatModel
  ↓
OPENROUTER_API_KEY vazia  ← CAUSA RAIZ
```

A validação continua existindo porque iniciar uma aplicação de IA sem a credencial necessária esconderia o problema e produziria um erro menos claro apenas na primeira chamada.

### 12.6 Linux/macOS

Você também pode usar `.env`, ou exportar diretamente:

```bash
export OPENROUTER_API_KEY="sua-chave"
export OPENROUTER_MODEL="openai/gpt-4o-mini"
```

O modelo pode ser alterado, mas precisa aceitar **Structured Outputs / JSON Schema**.

### Observação didática sobre fuso horário

Para não misturar o estudo de IA com regras complexas de fuso horário, esta POC trata horários sem fuso explícito como **UTC**. Em um sistema médico real, a zona da clínica/paciente deveria ser modelada explicitamente.

---

## 13. Executar

```bash
mvn spring-boot:run
```

A aplicação será iniciada em:

```text
http://localhost:3000
```

---

## 14. Testar manualmente

### Agendar

```bash
curl -X POST http://localhost:3000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "pergunta": "Meu nome é Maria Santos. Quero marcar uma consulta com Dr. Alicio da Silva amanhã às 16h para um consulta de rotina."
  }'
```

### Cancelar

```bash
curl -X POST http://localhost:3000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "pergunta": "Sou João da Silva. Quero cancelar minha consulta com Dr. Alicio da Silva hoje às 11h."
  }'
```

### Intenção desconhecida

```bash
curl -X POST http://localhost:3000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "pergunta": "Qual é a previsão do tempo para hoje?"
  }'
```

---

## 15. Postman

Importe o arquivo:

```text
postman/poc_java_ia_consultamedica.postman_collection.json
```

A collection contém quatro exemplos:

```text
01 - Agendar consulta
02 - Cancelar consulta existente
03 - Intenção desconhecida
04 - Agendamento com dados incompletos
```

A variável `baseUrl` já está configurada como:

```text
http://localhost:3000
```

---

## 16. Testes automatizados

Execute:

```bash
mvn test
```

Os testes de workflow usam uma implementação falsa de IA:

```text
InteligenciaArtificialFalsaParaTeste
```

Assim podemos testar:

```text
roteamento
regras de negócio
estado do workflow
```

sem gastar chamadas no OpenRouter.

Esse também é um conceito replicável: **dependa de uma interface e substitua a integração externa durante os testes**.

---

## 17. Como replicar o conceito em outro domínio

O projeto não deve ser decorado.

Use este molde:

```text
1. Quais intenções meu usuário pode ter?
2. Quais dados cada intenção precisa?
3. Qual objeto Java representa esses dados?
4. Qual JSON Schema representa o mesmo contrato?
5. Como a IA identifica e extrai esses dados?
6. Como o Java valida o resultado?
7. Qual regra de negócio o Java executa?
8. Como transformo o resultado em uma resposta humana?
```

Exemplo para suporte técnico:

```text
"Meu notebook não liga. Abra um chamado urgente."
```

Pode virar:

```json
{
  "intencao": "abrir_chamado",
  "categoria": "hardware",
  "prioridade": "alta",
  "descricao": "Notebook não liga"
}
```

Depois:

```text
IA extrai
→ Java valida
→ Java cria chamado
→ IA comunica resultado
```

Veja também:

```text
docs/COMO_REPLICAR_O_CONCEITO.md
```

---

## 18. O que propositalmente não existe nesta POC

Para manter o foco no conceito das aulas, não foram adicionados:

- PostgreSQL;
- JPA/Hibernate;
- autenticação;
- Kafka;
- RAG;
- banco vetorial;
- MCP;
- persistência permanente;
- interface web.

Esses itens podem ser acrescentados depois, mas não são necessários para entender **intenção + Structured Output + workflow + regra de negócio**.

---

## 19. Referências técnicas

Documentação oficial do LangChain4j sobre Structured Outputs:

```text
https://docs.langchain4j.dev/tutorials/structured-outputs/
```

Integração OpenAI do LangChain4j:

```text
https://docs.langchain4j.dev/integrations/language-models/open-ai/
```

Structured Outputs no OpenRouter:

```text
https://openrouter.ai/docs/guides/features/structured-outputs
```
