package br.sc.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import br.sc.service.LlamaAiService;
import dev.langchain4j.service.TokenStream;

@Path("/llama")
public class LlamaResource {

    @Inject
    LlamaAiService llamaAiService;

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void chatSse(@QueryParam("msg") String msg, @jakarta.ws.rs.core.Context SseEventSink sink, @jakarta.ws.rs.core.Context Sse sse) {
        if (sink == null) return;

        StringBuilder buffer = new StringBuilder();
        AtomicInteger wordCount = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);

        TokenStream stream = llamaAiService.chat(
                msg != null ? msg : "Diga apenas: funcionando"
        );

        stream
            .onPartialResponse(token -> {
                buffer.append(token);
                if (token.matches("\\s+|,|\\.")) {
                    wordCount.incrementAndGet();
                }
                if (wordCount.get() >= 5 || token.equals(",") || token.equals(".")) {
                    synchronized (sink) {
                        if (!sink.isClosed()) {
                            sink.send(sse.newEvent(buffer.toString().trim()));
                        }
                    }
                    buffer.setLength(0);
                    wordCount.set(0);
                }
            })
            .onCompleteResponse(response -> {
                if (buffer.length() > 0) {
                    synchronized (sink) {
                        if (!sink.isClosed()) {
                            sink.send(sse.newEvent(buffer.toString().trim()));
                        }
                    }
                }
                completed.set(true);
                sink.close();
            })
            .onError(err -> {
                completed.set(true);
                sink.close();
            })
            .start();

        while (!completed.get()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}