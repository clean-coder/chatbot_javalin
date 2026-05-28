import io.javalin.Javalin;

void main() {
    var app = Javalin.create().start(7070);
    app.get("/hello", ctx -> ctx.result("Hello, World!"));
}
