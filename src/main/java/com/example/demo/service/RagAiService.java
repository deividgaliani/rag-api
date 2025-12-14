package com.example.demo.service;

import dev.langchain4j.service.spring.AiService;

@AiService
public interface RagAiService {
    @dev.langchain4j.service.SystemMessage("""
            Você é um assistente de IA especializado.
            Sua tarefa é responder à pergunta do usuário utilizando **ESTRITAMENTE** o contexto fornecido.
            Não utilize conhecimento prévio ou externo.
            Se a resposta para a pergunta não estiver presente no contexto, responda apenas: "Não há essa informação no contexto fornecido".""")
    String chat(String userMessage);
}
