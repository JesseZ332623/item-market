package com.example.jesse.item_market.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/** HTTP 通用响应构建器。*/
@Slf4j
@Component
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class ResponseBuilder
{
    /**
     * 响应体静态模板类。
     *
     * @param <T> 响应体数据类型
     */
    @Data
    @NoArgsConstructor
    public static class APIResponse<T>
    {
        /** 响应时间戳（默认是构造本响应体那一刻的时间）*/
        private final long timestamp = Instant.now().toEpochMilli();

        /** 响应码 */
        private HttpStatus status;

        /** 响应消息 */
        private String message;

        /**
         * 响应数据，
         * 使用 @JsonInclude(JsonInclude.Include.NON_NULL) 注解，</br>
         * 在 data == null 的情况下，这个字段就不会序列化到 JSON 中去。
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private T data;

        /**
         * 响应元数据，
         * 使用 @JsonInclude(JsonInclude.Include.NON_EMPTY) 注解，</br>
         * metadata 为 null、ImmutableCollections.EMPTY_MAP、
         * 或者其他没有任何元素的空 Map 时，这个字段不会被序列化到 JSON 中去。
         */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Object> metadata = new HashMap<>();

        /**
         * 检查当前响应体中有没有键 _links 映射的 HATEOAS 链接表，
         * 有转换后返回，没有则创建一个后返回。
         */
        private Map<String, Object> getOrCreateLinksMap()
        {
            Object linksObj = this.getMetadata().get("_links");

            if (linksObj instanceof Map)
            {
                @SuppressWarnings(value = "unchecked")
                Map<String, Object> linksMap = (Map<String, Object>) linksObj;

                return linksMap;
            }
            else
            {
                Map<String, Object> linksMap = new HashMap<>();
                this.getMetadata().put("_links", linksMap);

                return linksMap;
            }
        }

        /**
         * 创建单个 HATEOAS 链接表。
         *
         * @param href   链接字符串
         * @param method 请求方法
         */
        private @NotNull Map<String, String>
        createLinkObject(String href, String method)
        {
            Map<String, String> link = new HashMap<>();

            link.put("href", href);
            link.put("method", method);

            return link;
        }

        /** 响应体的构造方法。*/
        public APIResponse(@NotNull HttpStatus status) {
            this.status   = status;
            this.message  = status.getReasonPhrase();
        }

        /**
         * 在响应体中添加分页元数据。
         *
         * @param page        第几页？
         * @param size        一页几条数据？
         * @param totalItems  总共几条数据？
         *
         * @return 添加完分页元数据的响应体
         */
        public APIResponse<T>
        withPagination(int page, int size, long totalItems)
        {
            Map<String, Object> pagination = new HashMap<>();

            pagination.put("page", page);
            pagination.put("size", size);
            pagination.put("totalItem", totalItems);

            this.getMetadata().put("pagination", pagination);

            return this;
        }

        /**
         * 在响应体元数据中添加 HATEOAS 元数据。
         * <p>
         *      HATEOAS 全称为 Hypermedia as the Engine of Application State，
         *      作为 REST 架构的核心约束之一，它是一种可以让响应体能够自我描述的理念
         *     （即响应体不仅包括了相关数据，还包括了相关操作的链接）。
         * </p>
         * <p>假设没有添加 HATEOAS 的响应体是这样的：</p>
         * <pre><code>
         * {
         *     "message": "Query score record id = 300 success!",
         *     "data": {
         *         "scoreId": 300,
         *         "userId": 1,
         *         "userName": "Jesse",
         *         "submitDate": [2025, 2, 3, 10, 22, 6],
         *         "correctCount": 10,
         *         "errorCount": 26,
         *         "noAnswerCount": 38
         *     },
         *     "statues": "OK",
         *     "time_STAMP": 1750942236538
         * }
         * </code></pre>
         *
         * <p>
         *     那么添加了 HATEOAS 元数据的响应体可能是这样的
         *     （多出了相关操作的链接，前端可以更方便地执行相关操作，不需要再手动的拼接 URL）：
         * </p>
         * <pre><code>
         * {
         *     "message": "Query score record id = 300 success!",
         *     "data": {
         *         "scoreId": 300,
         *         "userId": 1,
         *         "userName": "Jesse",
         *         "submitDate": [2025, 2, 3, 10, 22, 6],
         *         "correctCount": 10,
         *         "errorCount": 26,
         *         "noAnswerCount": 38
         *     },
         *     "metadata": {
         *          "_links": {
         *              "self": {
         *                  "href": "/api/score_record?id=300",
         *                  "method": "GET"
         *              },
         *              "next": {
         *                  "href": "/api/score_record?id=301",
         *                  "method": "GET"
         *              },
         *              "previous" : {
         *                  "href": "/api/score_record?id=299",
         *                  "method": "GET"
         *              },
         *              "update" : {
         *                  "href" : "/api/score_record?id=300",
         *                  "method" : "PUT"
         *              },
         *              "related": [
         *              {
         *                  "href": "/api/score_record/rank",
         *                  "method": "GET"
         *              },
         *              {
         *                  ....
         *              }
         *          ]
         *     }
         *     "statues": "OK",
         *     "time_STAMP": 1750942236538
         * }
         * </code></pre>
         *
         * <p>
         *     HATEOAS 元数据添加规则：
         *     <ol>
         *         <li>首次添加不存在的链接   -> 存储为单对象</li>
         *         <li>再次添加简述相同的链接 -> 转换为数组后再存储</li>
         *         <li>后续添加简述相同的链接 -> 追加到数组</li>
         *     </ol>
         * </p>
         *
         * @param rel    链接简述
         * @param href   链接字符串
         * @param method 请求方法
         *
         * @return 添加完 HATEOAS 元数据的响应体
         */
        public APIResponse<T>
        withLink(String rel, String href, HttpMethod method)
        {
            Map<String, Object> linksMap = this.getOrCreateLinksMap();

            Object existLinkingMap = linksMap.get(rel);

            switch (existLinkingMap)
            {
                case null -> linksMap.put(rel, this.createLinkObject(href, method.name()));

                case Map<?, ?> ignored ->
                {
                    List<Object> linksList = new ArrayList<>();

                    linksList.add(existLinkingMap);
                    linksList.add(this.createLinkObject(href, method.name()));

                    linksMap.put(rel, linksList);
                }
                case List<?> ignored ->
                {
                    @SuppressWarnings(value = "unchecked")
                    List<Object> linksList = (List<Object>) existLinkingMap;

                    linksList.add(this.createLinkObject(href, method.name()));
                }
                default -> {}
            }

            return this;
        }

        /**
         * 在响应体元数据中添加 HATEOAS 元数据（默认使用 GET 方法）。
         *
         * @param rel    链接简述
         * @param href   链接字符串
         */
        public APIResponse<T>
        withLink(String rel, String href) {
            return this.withLink(rel, href, HttpMethod.GET);
        }
    }

    /**
     * 响应体构建器。
     *
     * @param <T> 响应体数据类型
     *
     * @param status        响应码
     * @param data          响应体
     * @param customMessage 响应消息
     * @param hateOASLink   HATEOAS 元数据集合
     *
     * @return 构造好地响应体
     */
    private <T> APIResponse<T> produceBody(
        HttpStatus status, T data,
        String customMessage,
        Set<Link> hateOASLink
    )
    {
        APIResponse<T> response = new APIResponse<>(status);
        response.setData(data);

        if (customMessage != null) {
            response.setMessage(customMessage);
        }

        if (hateOASLink != null)
        {
            for (Link link : hateOASLink)
            {
                response.withLink(
                    link.getRel(),
                    link.getHref(), link.getMethod()
                );
            }
        }

        return response;
    }

    /**
     * 基础响应的构建其一。
     *
     * <p>用例如下所式：</p>
     * <pre><code>
     * ResponseBuilder builder;
     * builder.build(
     *     OK, data,
     *     headers -> {
     *         headers.add("X-RateLimit-Remaining", "100");
     *         headers.setContentType(MediaType.APPLICATION_JSON);
     *     }, hateOASLink
     * );
     * </code></pre>
     *
     * @param status            响应码
     * @param body              响应体
     * @param headersCustomizer 响应头消费者
     * @param hateOASLink       HATEOAS 元数据集合
     *
     * @return 构造好地响应体 Mono
     */
    public @NotNull Mono<ServerResponse> build(
        HttpStatus status, Object body, String customMessage,
        Consumer<HttpHeaders> headersCustomizer,
        Set<Link> hateOASLink
    )
    {
        return ServerResponse.status(status)
            .headers(headersCustomizer)
            .bodyValue(
                this.produceBody(
                    status, body,
                    customMessage, hateOASLink
                )
            );
    }

    /**
     * 基础响应的构建其二。
     *
     * @param <T> 响应数据类型
     *
     * @param headersCustomizer 响应头消费者
     * @param response 响应体数据（外部构建）
     *
     * @return 构造好地响应体 Mono
     */
    public @NotNull <T> Mono<ServerResponse>
    build(
        Consumer<HttpHeaders>   headersCustomizer,
        @NotNull APIResponse<T> response
    )
    {
        return ServerResponse.status(response.getStatus())
            .headers(headersCustomizer)
            .bodyValue(response);
    }

    /**
     * 创建 201 响应码构成的响应体，
     * 多用于 POST 或 PUT 方法。
     *
     * <p>用例如下所式：</p>
     * <pre><code>
     * ResponseBuilder builder;
     * builder.buildCreated(
     *     URI.create("/api/query?id=114"),
     *     newResource,
     *     headers -> {
     *         headers.add("X-RateLimit-Remaining", "100");
     *         headers.add("X-Custom-Header", "value");
     *         headers.setContentType(MediaType.APPLICATION_JSON);
     *     }
     * );
     * </code></pre>
     *
     * @param location          新创建的资源的 URI
     * @param body              响应体
     * @param headersCustomizer 响应头消费者
     *
     * @return 构造好地响应体 Mono
     */
    public @NotNull Mono<ServerResponse>
    buildCreated(
        URI location, Object body, String customMessage,
        Consumer<HttpHeaders> headersCustomizer,
        Set<Link> hateOASLink
    )
    {
        Consumer<HttpHeaders> headersCombined
            = (headers) -> {
            headers.setLocation(location);

            if (headersCustomizer != null) {
                headersCustomizer.accept(headers);
            }
        };

        return ServerResponse.status(HttpStatus.CREATED)
            .headers(headersCombined)
            .bodyValue(
                this.produceBody(
                    HttpStatus.CREATED, body,
                    customMessage, hateOASLink
                )
            );
    }

    /**
     * 错误响应的构建。
     *
     * <p>用例如下所式：</p>
     * <pre><code>
     * ResponseBuilder builder;
     * builder.buildError(
     *     HttpStatus.NOT_FOUND, message,
     *     new RuntimeException("Resource NOT FOUND!"),
     *     headers ->
     *         headers.setContentType(MediaType.APPLICATION_JSON)
     * );
     * </code></pre>
     *
     * @param status            响应码
     * @param message           错误消息
     * @param exception         异常接口
     * @param headersCustomizer 响应头消费者
     *
     * @return 构造好地响应体 Mono
     */
    public @NotNull Mono<ServerResponse>
    buildError(
        HttpStatus status, String message,
        Throwable exception,
        Consumer<HttpHeaders> headersCustomizer
    )
    {
        log.error("API ERROR: {}.", message, exception);

        APIResponse<?> response
            = this.produceBody(
            status, null, message, null
        );
        response.getMetadata().put("errorCode", "ERR_" + status.value());

        return ServerResponse.status(status)
            .headers(headersCustomizer)
            .bodyValue(response);
    }

    /** OK 响应的预设构建。*/
    public @NotNull Mono<ServerResponse>
    OK(
        Object data, String customMessage,
        Consumer<HttpHeaders> headerCustomizer,
        Set<Link> hateOASLink
    )
    {
        return this.build(
            HttpStatus.OK, data, customMessage,
            headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (headerCustomizer != null) {
                    headerCustomizer.accept(headers);
                }
            }, hateOASLink
        );
    }

    /** OK 响应的预设构建（使用默认响应头，无 HATEOAS 元数据）。 */
    public @NotNull Mono<ServerResponse>
    OK(Object data, String customMessage)
    {
        return
        this.OK(data, customMessage, null, null);
    }

    /** CREATED 响应的预设构建。*/
    public @NotNull Mono<ServerResponse>
    CREATED(
        URI location, String customMessage,
        Object body, Set<Link> hateOASLink
    )
    {
        return this.buildCreated(
            location, body, customMessage,
            (headers) -> {
                /*
                 * X-RateLimit-Remaining 用于提示客户端，
                 * 还可以发起多少个请求，如果客户端的请求数超过这个值就会被拦截，
                 * 并返回 429 Too Many Requests 状态码。
                 */
                headers.add("X-RateLimit-Remaining", "100");
                headers.add("X-Custom-Header", "value");
                headers.setContentType(MediaType.APPLICATION_JSON);
            }, hateOASLink
        );
    }

    /** BAD REQUEST 响应的预设构建。*/
    public @NotNull Mono<ServerResponse>
    BAD_REQUEST(String message, Throwable exception)
    {
        return this.buildError(
            HttpStatus.BAD_REQUEST, message,
            exception,
            headers ->
                headers.setContentType(MediaType.APPLICATION_JSON)
        );
    }

    /** NOT FOUND 响应的预设构建。*/
    public @NotNull Mono<ServerResponse>
    NOT_FOUND(String message, Throwable exception)
    {
        return this.buildError(
            HttpStatus.NOT_FOUND, message,
            exception,
            headers ->
                headers.setContentType(MediaType.APPLICATION_JSON)
        );
    }

    /** INTERNAL_SERVER_ERROR 响应的预设构建。*/
    public @NotNull Mono<ServerResponse>
    INTERNAL_SERVER_ERROR(String message, Throwable exception)
    {
        return this.buildError(
            HttpStatus.INTERNAL_SERVER_ERROR, message,
            exception,
            headers ->
                headers.setContentType(MediaType.APPLICATION_JSON)
        );
    }

    /* 后续的可以继续补充响应的预设构建。*/
}