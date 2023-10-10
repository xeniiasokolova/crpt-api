package com.git.selsup;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrptApi {

    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create"; // URL для API
    private final Semaphore semaphore; // Семафор для ограничения количества запросов
    private final HttpClient httpClient; // HTTP-клиент для отправки запросов
    private final TimeUnit timeUnit; // Единица измерения времени для ограничения запросов
    private int requestLimit; // Максимальное количество запросов в определенный интервал времени
    private final Gson gson; // Объект Gson для сериализации/десериализации JSON
    private final Logger logger = LoggerFactory.getLogger(CrptApi.class); // Логгер


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        // Проверка на корректность аргумента requestLimit
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Request limit must be greater than zero");
        }
        // Инициализация полей класса
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.gson = new Gson();

        // Настройка параметров HTTP-клиента
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .build();
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public boolean createDocument(Document document, String signature) {
        try {
            semaphore.acquire(); // Захват семафора для ограничения запросов
            String jsonBody = gson.toJson(document); // Сериализация объекта Document в JSON

            HttpPost httpPost = new HttpPost(URL); // Создание HTTP POST запроса
            httpPost.setHeader("Content-Type", "application/json"); // Установка заголовка Content-Type
            httpPost.setHeader("Authorization", "Bearer " + signature); // Установка заголовка Authorization
            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON)); // Установка тела запроса

            HttpResponse response = httpClient.execute(httpPost); // Выполнение HTTP запроса

            StatusLine statusLine = response.getStatusLine(); // Получение статусной строки ответа
            int statusCode = statusLine.getStatusCode(); // Получение кода статуса

            if (statusCode == HttpStatus.SC_OK) { // Проверка успешного статуса (200 OK)
                logger.info("API request succeeded!"); // Логирование успешного запроса
                HttpEntity entity = response.getEntity(); // Получение тела ответа
                if (entity != null) {
                    String responseBody = EntityUtils.toString(entity); // Преобразование тела ответа в строку
                    logger.debug("Response body: " + responseBody); // Логирование тела ответа (для отладки)
                }
                return true; // Возврат успешного результата
            } else {
                logger.error("API request failed with status code: " + statusCode); // Логирование ошибки
                return false; // Возврат неуспешного результата
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Error executing API request", e); // Логирование ошибки при выполнении запроса
            return false; // Возврат неуспешного результата
        } finally {
            semaphore.release(); // Освобождение семафора
        }
    }

    // Вложенный класс Document для представления данных документа
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products;
        private String regDate;
        private String regNumber;
    }

    // Вложенный класс Description для представления описания документа
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class Description {
        private String participantInn;
    }

    // Вложенный класс Product для представления продукта
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }
}
