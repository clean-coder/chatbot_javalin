# Javalin + LangChain4j Chatbot

A set of chatbot web apps built with [Javalin](https://javalin.io/) and [LangChain4j](https://docs.langchain4j.dev/), backed by OpenAI's `gpt-4o-mini`. Three progressively more capable implementations live side by side.

---

## Implementations

| Class | Package | What it adds |
|-------|---------|--------------|
| `SimpleChatbotApp` | `simple` | Full-page reload on every message |
| `HtmxChatbotApp` | `htmx` | Partial HTML swap via HTMX (no full reload) |
| `ChatbotAppWithTools` | `tools` | AI Services + tool use (weather lookup) + system prompt |

All three run on port `7070`.

---

## Analogy: A Waiter with a Notepad

Think of this app as a restaurant. You (the browser) sit at a table and write your order on a slip of paper. A waiter (Javalin) runs it to the kitchen (OpenAI's `gpt-4o-mini`), brings back the dish, and writes both your order and the kitchen's response on a running notepad (`history`). Every time you ask a question, the whole notepad is sent to the kitchen — that is how the model "remembers" the conversation.

---

## SimpleChatbotApp

### Architecture

```
Browser                 SimpleChatbotApp              OpenAI
   │                          │                          │
   │──── GET /  ─────────────▶│                          │
   │◀─── Full HTML page ──────│ (renders history list)   │
   │                          │                          │
   │──── POST /chat ─────────▶│                          │
   │     (form: prompt=...)   │──── model.chat(history)─▶│
   │                          │◀─── AiMessage ───────────│
   │                          │  history.add(user msg)   │
   │                          │  history.add(ai msg)     │
   │◀─── redirect to GET / ───│                          │
   │                          │                          │
   │──── POST /clear ────────▶│                          │
   │                          │  history.clear()         │
   │◀─── redirect to GET / ───│                          │
```

### How it works

**Static state** — both `history` and `model` are `static` fields, existing once for the entire JVM lifetime.

**Three routes:**

| Route | Method | Purpose |
|-------|--------|---------|
| `/` | GET | Renders the full chat page |
| `/chat` | POST | Receives user message, calls LLM, appends both turns, redirects |
| `/clear` | POST | Wipes history, redirects |

**The chat loop:**

```java
history.add(UserMessage.from(prompt));       // 1. append user turn
AiMessage response = model.chat(history)     // 2. send WHOLE history to LLM
                          .aiMessage();
history.add(response);                       // 3. append AI turn
ctx.redirect("/");                           // 4. reload page
```

The key move is passing the *entire* `history` list to `model.chat()`. The LLM doesn't store anything itself — you re-send everything each time.

---

## HtmxChatbotApp

Same concept as `SimpleChatbotApp`, but the `POST /chat` response returns only the updated chat history fragment instead of redirecting to a full page reload. [HTMX](https://htmx.org/) swaps the fragment directly into the DOM.

```
Browser                 HtmxChatbotApp                OpenAI
   │                          │                          │
   │──── GET /  ─────────────▶│                          │
   │◀─── Full HTML page ──────│                          │
   │                          │                          │
   │──── POST /chat ─────────▶│                          │
   │     (hx-post, hx-swap)   │──── model.chat(history)─▶│
   │                          │◀─── AiMessage ───────────│
   │◀─── history HTML fragment│                          │
   │   (HTMX swaps in-place)  │                          │
   │                          │                          │
   │──── POST /clear ────────▶│                          │
   │◀─── empty history frag ──│                          │
```

This avoids re-sending the full HTML page on every message and gives a smoother UX without any JavaScript framework.

---

## ChatbotAppWithTools

Extends the basic pattern with two LangChain4j features:

**1. AI Services** — instead of calling `model.chat(history)` directly, an `Assistant` interface is declared and LangChain4j generates a proxy at runtime:

```java
interface Assistant {
    @SystemMessage(SYSTEM_PROMPT)
    String chat(String message);
}

Assistant assistant = AiServices.builder(Assistant.class)
        .chatModel(model)
        .chatMemory(memory)
        .tools(new WeatherTool())
        .build();
```

**2. Tool use** — `WeatherTool` exposes a `@Tool`-annotated method. When the user asks about weather or packing, the model calls `get_forecast(city)` automatically before composing its answer. The tool returns hardcoded forecasts for Paris, Stockholm, London, Berlin, and Madrid.

**3. Managed memory** — `MessageWindowChatMemory.withMaxMessages(10)` is used instead of the raw `ArrayList`. It automatically trims old messages so the context window doesn't grow unboundedly.

> Note: `ChatbotAppWithTools` keeps a separate `history` list purely for rendering the UI. The actual LLM memory is managed by `ChatMemory`.

### System prompt

The assistant is configured as a travel assistant instructed to always call `get_forecast` before giving packing or weather advice.

---

## Shared Infrastructure

### HtmlBuilder (`helper` package)

Loads `chat.html` from the classpath as a template and injects chat history HTML via a `{{HISTORY}}` placeholder. Used by `SimpleChatbotApp` and `ChatbotAppWithTools`.

`HtmxChatbotApp` uses its own `HtmlBuilder` in the `htmx` package, which also exposes `buildHistoryHtml()` for returning partial fragments.

### chat.html / chat.css

Static resources served from the classpath. The HTML template defines the page structure; CSS handles bubble styling. HTMX attributes (`hx-post`, `hx-target`, `hx-swap`) are present in the template for the HTMX variant.

---

## Gotcha: History is Global, Not Per-User

`history` is a `static` field in all three apps — one shared list for all browser sessions. If two people open the app simultaneously, they share the same conversation and one person's "Clear" wipes the other's chat.

This is fine for a demo. A real app would key history by session ID (e.g., `Map<String, List<ChatMessage>>`).

---

## Setup

1. Set your OpenAI API key:
   ```bash
   export OPENAI_API_KEY=your-key-here
   ```

2. Run the desired app and open `http://localhost:7070` in your browser.
