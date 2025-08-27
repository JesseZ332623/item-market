package com.example.jesse.item_market.response;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static java.lang.String.format;

/** URL 参数解析工具类。（响应式版本）*/
@NoArgsConstructor(access  = AccessLevel.PRIVATE)
final public class URLParamPrase
{
    /**
     * <p>从 HTTP 请求的 URI 中查询指定的参数。</p>
     *
     * <p>
     *     比如 URL：<code>/api/query?name=perter</code>
     *     调用本方法 <code>getRequestParam(request, "name")</code>，返回字符串 perter。
     * </p>
     *
     * @param request    从前端传来的 HTTP 请求实例
     * @param paramName  参数名
     *
     * @throws IllegalArgumentException
     *         当 paramName 在 URI 中查询不到时抛出。
     */
    public static @NotNull Mono<String>
    praseRequestParam(final ServerRequest request, String paramName)
    {
        return Mono.fromCallable(
            () -> {
                if (request.queryParam(paramName).isEmpty())
                {
                    throw new IllegalArgumentException(
                        format(
                            "Parameter name [%s] not exist in request!",
                            paramName
                        )
                    );
                }

                return
                request.queryParam(paramName).get();
            }
        );
    }

    /**
     * <p>从 HTTP 请求的 URL 中查询指定的参数，将其转化成数字类型使用。</p>
     *
     * <p>
     *     比如 URL：<code>/api/query?id=114</code>
     *     调用本方法
     *     <code>
     *         getRequestParam(request, "id", Integer::parseInt)
     *     </code>
     *     ，返回整数 114。
     * </p>
     *
     * @param <T> 由于本方法是 T 的生成者，所以需要规定 T 的上界，
     *            即 T 可以说由 Number 派生下来的所以类型
     *
     * @param request    从前端传来的 HTTP 请求实例
     * @param paramName  参数名
     * @param converter  调用哪个转换方法？
     *
     * @return 从 URL 中解析下来的参数
     *
     * @throws IllegalArgumentException
     *         1. 当 paramName 在 URI 中查询不到时抛出。</br>
     *         2. 当 phraseVal 的值小于 0 时抛出。
     *
     * @throws NumberFormatException
     *         当 paramName 转化成数字失败时抛出。
     */
    public static <T extends Number> @NotNull Mono<T>
    praseNumberRequestParam(
        final ServerRequest request, String paramName,
        Function<String, T> converter

    )
    {
        return praseRequestParam(request, paramName)
            .map((param) -> {
                T number = converter.apply(param);

                if (number.doubleValue() < 0)
                {
                    throw new IllegalArgumentException(
                        format("Parameter: [%s] not less than 0!", paramName)
                    );
                }

                return number;
            })
            .onErrorMap(NumberFormatException.class,
                exception ->
                    new IllegalArgumentException("Invalid number format for parameter: " + paramName))
            .onErrorMap(IllegalArgumentException.class,
                exception -> exception)
            .onErrorResume(Mono::error);
    }
}