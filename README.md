# SimpleChatbotApp

A minimal chatbot web app built with [Javalin](https://javalin.io/) and [LangChain4j](https://docs.langchain4j.dev/), backed by OpenAI's `gpt-4o-mini`.

---

## Analogy: A Waiter with a Notepad

Think of this app as a restaurant. You (the browser) sit at a table and write your order on a slip of paper. A waiter (Javalin) runs it to the kitchen (OpenAI's `gpt-4o-mini`), brings back the dish, and writes both your order and the kitchen's response on a running notepad (`history`). Every time the page reloads, the whole notepad is read out loud to redraw the table — full conversation visible.

---

## Architecture

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

---

## How It Works

### 1. Static state (`SimpleChatbotApp`, lines 16–21)

```java
private static final List<ChatMessage> history = new ArrayList<>();
private static final ChatModel model = OpenAiChatModel.builder()...build();
```

Both live as `static` fields on the class — they exist once for the entire JVM lifetime. The `history` list is the chatbot's "memory". `model` is the configured OpenAI client.

### 2. Three routes (lines 24–30)

Javalin registers three HTTP handlers:

| Route | Method | Purpose |
|-------|--------|---------|
| `/` | GET | Renders the full chat page |
| `/chat` | POST | Receives user message, calls LLM, appends both turns, redirects |
| `/clear` | POST | Wipes history, redirects |

### 3. The chat loop (lines 41–49)

```java
history.add(UserMessage.from(prompt));       // 1. append user turn
AiMessage response = model.chat(history)     // 2. send WHOLE history to LLM
                          .aiMessage();
history.add(response);                       // 3. append AI turn
ctx.redirect("/");                           // 4. reload page
```

The key move is passing the *entire* `history` list to `model.chat()`. This is how the LLM "remembers" the conversation — it doesn't store anything itself; you re-send everything each time.

### 4. HTML rendering (`HtmlBuilder`)

`buildPage()` stringifies the `history` list into chat bubbles (user = blue, AI = dark). All text is HTML-escaped via `escapeHtml()` to prevent injection. A small `<script>` at the bottom auto-scrolls to the newest message.

---

## Gotcha: History is Global, Not Per-User

`history` is a `static` field — one shared list for all browser sessions. If two people open the app at the same time, they share the same conversation and one person's "Clear" wipes the other's chat.

This is fine for a demo, but a real app would need per-session state (e.g., a `Map<String, List<ChatMessage>>` keyed by session ID).

---

## Setup

1. Set your OpenAI API key:
   ```bash
   export OPENAI_API_KEY=your-key-here
   ```

2. Run the app and open `http://localhost:7070` in your browser.
