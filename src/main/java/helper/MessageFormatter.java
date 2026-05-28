package helper;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

public class MessageFormatter {

    public static String formatMessage(ChatMessage message) {
        if (message instanceof UserMessage um) {
            return formatUserMessage(um);
        } else if (message instanceof AiMessage am) {
            return formatAiMessage(am);
        }
        return "";
    }

    private static String formatUserMessage(UserMessage message) {
        return "<div class='bubble user'>"
               + "<div class='label'>[You]</div>"
               + escapeHtml(message.singleText())
               + "</div>";
    }

    private static String formatAiMessage(AiMessage message) {
        return "<div class='bubble ai'>"
               + "<div class='label'>[Assistant]</div>"
               + escapeHtml(message.text())
               + "</div>";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
