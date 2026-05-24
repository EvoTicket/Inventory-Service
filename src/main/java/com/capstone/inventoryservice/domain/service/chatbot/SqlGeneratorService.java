package com.capstone.inventoryservice.domain.service.chatbot;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SqlGeneratorService {

    private final ChatClient.Builder chatClientBuilder;

    @Value("classpath:text-to-sql-prompt.st")
    private Resource systemPromptResource;

    @Value("classpath:text-to-sql-schema.sql")
    private Resource ddlSchemaResource;

    private ChatClient chatClient;

    public SqlGeneratorService(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    public void init() {
        try {
            String systemPromptContent = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            String ddlContent = ddlSchemaResource.getContentAsString(StandardCharsets.UTF_8);

            PromptTemplate template = new PromptTemplate(systemPromptContent);
            template.add("ddl", ddlContent);
            String renderedSystemPrompt = template.render();

            this.chatClient = chatClientBuilder
                    .defaultSystem(renderedSystemPrompt)
                    .build();

            log.info("[SqlGeneratorService] Khoi tao thanh cong voi prompt va DDL.");
        } catch (IOException e) {
            log.error("[SqlGeneratorService] Loi khi doc file system prompt hoac DDL schema: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Loi khoi tao SqlGeneratorService: " + e.getMessage());
        }
    }

    public String generate(String question) {
        try {
            log.info("[SqlGeneratorService] Dang tao SQL tu cau hoi: {}", question);
            String response = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();

            if (response == null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Model khong tra ve ket qua SQL.");
            }

            response = cleanSqlQuery(response);
            log.info("[SqlGeneratorService] SQL da tao: {}", response);

            boolean isSelectQuery = response.toUpperCase().startsWith("SELECT");
            if (!isSelectQuery) {
                // If it is not a SELECT query, it means LLM returned a refusal message (e.g. SQL Injection or unsupported operation)
                throw new AppException(ErrorCode.BAD_REQUEST, response);
            }

            return response;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SqlGeneratorService] Loi khi tao SQL: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Loi khi dich cau hoi sang SQL: " + e.getMessage());
        }
    }

    private String cleanSqlQuery(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```sql")) {
            cleaned = cleaned.substring(6);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
