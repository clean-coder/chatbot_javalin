package htmx;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;

public class HtmlBuilder {

    private static final String CSS = """
                *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
            
                body {
                  font-family: 'Segoe UI', system-ui, sans-serif;
                  background: white;
                  color: #e8eaf0;
                  display: flex;
                  flex-direction: column;
                  height: 100vh;
                  padding: 1rem;
                  gap: 1rem;
                }
            
                h1 {
                  font-size: 1.2rem;
                  font-weight: 600;
                  color: #7c8cf8;
                  letter-spacing: 0.05em;
                  text-transform: uppercase;
                  padding-bottom: 0.5rem;
                  border-bottom: 1px solid #2a2d3a;
                }
            
                #history-panel {
                  flex: 1;
                  overflow-y: auto;
                  display: flex;
                  flex-direction: column;
                  gap: 0.75rem;
                  padding: 0.5rem 0;
                }
            
                .bubble {
                  max-width: 100%;
                  padding: 0.65rem 1rem;
                  border-radius: 1rem;
                  line-height: 1.5;
                  font-size: 0.95rem;
                  white-space: pre-wrap;
                  word-break: break-word;
                }
            
                .bubble.user {
                  background: #3b4fd4;
                  align-self: flex-start;
                  border-bottom-right-radius: 0.2rem;
                }
            
                .bubble.ai {
                  background: #1e2130;
                  border: 1px solid #2a2d3a;
                  align-self: flex-start;
                  border-bottom-left-radius: 0.2rem;
                }
            
                .bubble .label {
                  font-size: 0.7rem;
                  font-weight: 700;
                  letter-spacing: 0.08em;
                  text-transform: uppercase;
                  margin-bottom: 0.3rem;
                  opacity: 0.6;
                }
            
                #input-bar {
                  display: flex;
                  gap: 0.5rem;
                  padding-top: 0.5rem;
                  border-top: 1px solid #2a2d3a;
                }
            
                #input-bar textarea {
                  flex: 1;
                  resize: none;
                  background: #1e2130;
                  border: 1px solid #2a2d3a;
                  border-radius: 0.6rem;
                  color: #e8eaf0;
                  font-size: 0.95rem;
                  padding: 0.65rem 1rem;
                  outline: none;
                  min-height: 56px;
                  max-height: 180px;
                  font-family: inherit;
                  transition: border-color 0.15s;
                }
                #input-bar textarea:focus { border-color: #7c8cf8; }
            
                .btn {
                  background: #3b4fd4;
                  border: none;
                  border-radius: 0.6rem;
                  color: #fff;
                  font-size: 0.9rem;
                  font-weight: 600;
                  padding: 0 1.2rem;
                  cursor: pointer;
                  transition: background 0.15s;
                  white-space: nowrap;
                }
                .btn:hover { background: #5063e0; }
                .btn.clear { background: #2a2d3a; color: #aaa; }
                .btn.clear:hover { background: #3a3d4a; color: #e8eaf0; }
            """;

    public static String buildPage(List<ChatMessage> history) {
        var historyHtml = buildHistoryHtml(history);
        return """
                       <!DOCTYPE html>
                       <html lang="en">
                       <head>
                         <meta charset="UTF-8">
                         <meta name="viewport" content="width=device-width, initial-scale=1.0">
                         <title>LangChain4j Chatbot</title>
                         <!-- htmx for partial updates — no JavaScript framework needed -->
                         <script src="https://unpkg.com/htmx.org@1.9.12"></script>
                         <style>"""
               + CSS
               + """
                         </style>
                       </head>
                       <body>
                         <h1>🤖 LangChain4j Chatbot Demo (HTMX)</h1>
                       
                         <!-- history is swapped here after each POST -->
                         <div id="history-panel">"""
               + historyHtml
               + """
                         </div>
                       
                         <div id="input-bar">
                           <form
                             hx-post="/chat"
                             hx-target="#history-panel"
                             hx-swap="innerHTML"
                             hx-on::after-request="this.reset(); scrollToBottom();"
                             style="display:contents"
                           >
                             <textarea
                               name="prompt"
                               placeholder="Type your message… (Enter to send)"
                               rows="2"
                               onkeydown="if(event.key==='Enter' && !event.shiftKey){ event.preventDefault(); this.closest('form').requestSubmit(); }"
                             ></textarea>
                             <button class="btn" type="submit">Send</button>
                           </form>
                       
                           <form
                             hx-post="/clear"
                             hx-target="#history-panel"
                             hx-swap="innerHTML"
                             style="display:contents"
                           >
                             <button class="btn clear" type="submit">Clear</button>
                           </form>
                         </div>
                       
                         <script>
                           function scrollToBottom() {
                             const panel = document.getElementById('history-panel');
                             panel.scrollTop = panel.scrollHeight;
                           }
                           // scroll on initial load and after htmx swaps
                           scrollToBottom();
                           document.body.addEventListener('htmx:afterSwap', scrollToBottom);
                         </script>
                       </body>
                       </html>
                       """;
    }


    public static String buildHistoryHtml(List<ChatMessage> history) {
        if (history.isEmpty()) {
            return formatEmptyHistory();
        }

        var sb = new StringBuilder();
        for (var msg : history) {
            if (msg instanceof UserMessage um) {
                sb.append(formatUserMessage(um.singleText()));
            } else if (msg instanceof AiMessage am) {
                sb.append(formatAiMessage(am.text()));
            }
        }
        return sb.toString();
    }

    private static String formatUserMessage(String message) {
        return "<div class='bubble user'>"
               + "<div class='label'>[You]</div>"
               + escapeHtml(message)
               + "</div>";
    }

    private static String formatAiMessage(String message) {
        return "<div class='bubble ai'>"
               + "<div class='label'>[Assistant]</div>"
               + escapeHtml(message)
               + "</div>";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String formatEmptyHistory() {
        var message = "No messages yet. Start the conversation!";
        return "<p style='color:#555; font-size:0.85rem; text-align:center; margin-top:2rem;'>"
               + message
               + "</p>";
    }
}
