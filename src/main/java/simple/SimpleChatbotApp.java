package simple;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;

public class SimpleChatbotApp {

    private static final List<ChatMessage> history = new ArrayList<>();

    private static final ChatModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY")) // set via env variable
            .modelName("gpt-4o-mini")
            .build();

    static void main(String[] args) {
        Javalin app = Javalin
                .create()
                .start(7070);

        app.get("/", SimpleChatbotApp::buildPage);
        app.post("/chat", SimpleChatbotApp::handleChat);
        app.post("/clear", SimpleChatbotApp::handleClear);
    }

    // endpoint: GET /
    // renders the full page
    private static void buildPage(Context ctx) {
        ctx.html(HtmlBuilder.buildPage(history));
    }

    // endpoint: POST /chat
    // receive prompt, call LLM, return updated history and reload html page
    private static void handleChat(Context ctx) {
        String prompt = ctx.formParam("prompt");
        if (prompt != null && !prompt.isBlank()) {
            history.add(UserMessage.from(prompt));
            AiMessage response = model.chat(history).aiMessage();
            history.add(response);
        }
        ctx.redirect("/");
    }

    private static void handleClear(Context ctx) {
        history.clear();
        ctx.redirect("/");
    }
}