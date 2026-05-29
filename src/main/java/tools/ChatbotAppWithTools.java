package tools;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import helper.HtmlBuilder;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;

public class ChatbotAppWithTools {
    // for rendering in the ui only
    private static final List<ChatMessage> history = new ArrayList<>();

    private static final String SYSTEM_PROMPT = """
            You are a helpful travel assistant. 
            
            IMPORTANT: When users ask about:
            - What to pack for a trip
            - What clothes to bring
            - Weather conditions
            - Temperature in a city
            
            You MUST use the get_forecast tool to check the current weather before providing advice. Never give generic packing advice without checking the actual weather forecast first.
            """;

    // assistant interface for AI services with tool integration and system prompt
    interface Assistant {
        @SystemMessage(SYSTEM_PROMPT)
        String chat(String message);
    }

    private static final ChatModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY")) // set via env variable
            .modelName("gpt-4o-mini")
            .logRequests(true)
            .logResponses(true)
            .build();

    private static Assistant assistantWithWeatherTool = AiServices.builder(Assistant.class)
            .chatModel(model)
            .tools(new WeatherTool())
            .build();

    static void main(String[] args) {
        Javalin app = Javalin
                .create(config -> config.staticFiles.add("/"))
                .start(7070);

        app.get("/", ChatbotAppWithTools::buildPage);
        app.post("/chat", ChatbotAppWithTools::handleChat);
        app.post("/clear", ChatbotAppWithTools::handleClear);
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
            history.add(UserMessage.from(prompt)); // for rendering the full history, we still need to add user message to the history
            String response = assistantWithWeatherTool.chat(prompt);
            history.add(AiMessage.from(response)); // for rendering ....
        }
        ctx.redirect("/");
    }

    private static void handleClear(Context ctx) {
        history.clear();
        ctx.redirect("/");
    }
}