package simple;

import dev.langchain4j.data.message.ChatMessage;
import helper.MessageFormatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class HtmlBuilder {

    private static final String PAGE_TEMPLATE = loadTemplate();

    private static String loadTemplate() {
        try (var stream = HtmlBuilder.class.getResourceAsStream("/simple/chat.html")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load chat.html template", e);
        }
    }

    public static String buildPage(List<ChatMessage> history) {
        return PAGE_TEMPLATE.replace("{{HISTORY}}", buildHistoryHtml(history));
    }

    public static String buildHistoryHtml(List<ChatMessage> history) {
        if (history.isEmpty()) {
            return formatEmptyHistory();
        }

        return history.stream()
                .map(message -> MessageFormatter.formatMessage(message))
                .collect(Collectors.joining());
    }

    private static String formatEmptyHistory() {
        var message = "No messages yet. Start the conversation!";
        return "<p style='color:#555; font-size:0.85rem; text-align:center; margin-top:2rem;'>"
               + message
               + "</p>";
    }
}
