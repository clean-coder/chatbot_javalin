package htmx;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;

public class HtmxChatbotApp {

    // In-memory chat history (per server instance — not per user session)
    // For multi-user support, key by session ID
    private static final List<ChatMessage> history = new ArrayList<>();

    private static final ChatModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY")) // set via env variable
            .modelName("gpt-4o-mini")
            .build();

    public static void main(String[] args) {
        Javalin app = Javalin
                .create()
                .start(7070);

        app.get("/", HtmxChatbotApp::renderChat);
        app.post("/chat", HtmxChatbotApp::handleChat);
        app.post("/clear", HtmxChatbotApp::clearHistory);
    }

    // endpoint: GET /
    // renders the full page
    private static void renderChat(Context ctx) {
        ctx.html(HtmlBuilder.buildPage(history));
    }

    // endpoint: POST /chat
    // receive prompt, call LLM, return updated history
    private static void handleChat(Context ctx) {
        String prompt = ctx.formParam("prompt");
        if (prompt == null || prompt.isBlank()) {
            ctx.redirect("/");
            return;
        }

        history.add(UserMessage.from(prompt));
        AiMessage response = model.chat(history).aiMessage();
        history.add(response);

        // Return only the updated history fragment (for htmx swap)
        ctx.html(HtmlBuilder.buildHistoryHtml(history));
    }

    // endpoint: POST /clear
    // wipes history
    private static void clearHistory(Context ctx) {
        history.clear();
        ctx.html(HtmlBuilder.buildHistoryHtml(history));
    }
}