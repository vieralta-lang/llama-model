package br.sc.service;


import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface LlamaAiService {

    @SystemMessage("Você é um assistente técnico objetivo.")
    TokenStream chat(String message);
}