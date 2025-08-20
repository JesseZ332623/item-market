package com.example.jesse.item_market.location_search.impl;

import com.example.jesse.item_market.location_search.GeoLiteCityReader;
import com.example.jesse.item_market.location_search.dto.IpToCityID;
import com.example.jesse.item_market.location_search.dto.LocationInfo;
import com.example.jesse.item_market.location_search.exception.CSVFileNotInClassPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static java.lang.String.format;

/** 地理信息相关 CSV 文件读取并存储至 Redis 实现。*/
@Slf4j
@Component
public class GeoLiteCityReaderImpl implements GeoLiteCityReader
{
    /** CSV 文件在 JAR 包中的路径前缀。*/
    private static final String
    CSV_SCRIPT_CLASSPATH_PREFIX = "csv/";

    /** CSV 文件的读取模式。*/
    @Value("${app.csv-file-mode}")
    private String csvReadMode;

    /** CSV 文件在文件系统中的路径。*/
    @Value("${app.csv-file-path}")
    private String csvFilePath;

    private static final String blocksRedisKey   = "location:ip_to_cityid";
    private static final String locationRedisKey = "location:cityid_to_cityname";

    /** 字符串序列化 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, String> stringRedisTemplate;

    /** 通用 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * 从  GeoLiteCity-Blocks.csv 文件中构建出 csvParser 后，
     * 正式的开始读取并存入 Redis。
     */
    private @NotNull Mono<Void>
    saveBlocksFromCSVParser(CSVParser csvParser)
    {
        return
        Flux.fromIterable(csvParser)
            .map((record) ->
                new IpToCityID()
                    .setStartIpScore(Double.parseDouble(record.get("startIpNum")))
                    .setLocalId(record.get("locId")))
            .buffer(2048)
            .flatMap((records) -> {
                List<ZSetOperations.TypedTuple<String>> recordTuples
                    = records.stream()
                        .map(record ->
                            ZSetOperations.TypedTuple
                                .of(record.getLocalId(), record.getStartIpScore()))
                        .toList();

                return
                this.stringRedisTemplate
                    .opsForZSet()
                    .addAll(blocksRedisKey, recordTuples)
                    .timeout(Duration.ofSeconds(5L))
                    .onErrorResume((excption) ->
                        redisGenericErrorHandel(excption, null));
            }).then();
    }

    private String
    setDefaultForString(@NotNull String csvField) {
        return (csvField.trim().isEmpty()) ? "N/A" : csvField;
    }

    private String
    setDefaultForNumber(@NotNull String csvField) {
        return (csvField.trim().isEmpty()) ? "-1" : csvField;
    }

    /**
     * 从 GeoLiteCity-Location.csv 文件中构建出 csvParser 后，
     * 正式的开始读取并存入 Redis。
     */
    private @NotNull Mono<Void>
    saveLocationFromParser(CSVParser csvParser)
    {
        return
        Flux.fromIterable(csvParser)
            .map((record) -> {
                /* CSV 文件中有空串，需要做一下判断，对于没有的值填一个默认值进去就行。*/
                return new LocationInfo()
                    .setCountry(setDefaultForString(record.get("country")))
                    .setRegion(setDefaultForString(record.get("region")))
                    .setCity(setDefaultForString(record.get("city")))
                    .setPostalCode(setDefaultForString(record.get("postalCode")))
                    .setLatitude(Double.parseDouble(setDefaultForNumber(record.get("latitude"))))
                    .setLongitude(Double.parseDouble(setDefaultForNumber(record.get("longitude"))))
                    .setMetroCode(Integer.parseInt(setDefaultForNumber(record.get("metroCode"))))
                    .setAreaCode(Integer.parseInt(setDefaultForNumber(record.get("areaCode"))));
            })
            .buffer(2048)
            .flatMap((records) -> {
                Map<String, LocationInfo> recordsMap
                    = IntStream.range(0, records.size())
                               .boxed()
                               .collect(Collectors.toMap(String::valueOf, records::get));

                return
                this.redisTemplate
                    .opsForHash()
                    .putAll(locationRedisKey, recordsMap)
                    .timeout(Duration.ofSeconds(5L))
                    .onErrorResume((excption) ->
                        redisGenericErrorHandel(excption, null));
            }).then();
    }

    /** 从文件系统中读取指定 CSV 文件。*/
    private @NotNull CSVParser
    getCSVParserFromFile(String csvFileName) throws IOException
    {
        final Path path
            = Path.of(this.csvFilePath)
            .resolve(csvFileName)
            .normalize();

        Reader reader = Files.newBufferedReader(path);

        CSVFormat format
            = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();

        return new CSVParser(reader, format);

    }

    /** 从 classpath 中读取指定 CSV 文件。*/
    private @NotNull CSVParser
    getCSVParserFromClassPath(String csvFileName) throws IOException
    {
        final String classpathPath
            = CSV_SCRIPT_CLASSPATH_PREFIX + csvFileName;

        InputStream inputStream
            = getClass().getClassLoader().getResourceAsStream(classpathPath);

        if (inputStream == null)
        {
            throw new CSVFileNotInClassPath(
                format("CSV file: %s not found in classpath!",
                    classpathPath), null);
        }

        Reader reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );

        CSVFormat format
            = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();

        return new CSVParser(reader, format);
    }

    /** 读取完毕后关闭 csvParser。*/
    private void
    closeCSVParser(@NotNull CSVParser csvParser)
    {
        try { csvParser.close(); }
        catch (IOException exception) {
            log.error("Failed to close CSV parser!", exception);
        }
    }

    /**
     * （测试时用）
     * 从文件系统中读取 GeoLiteCity-Blocks.csv 文件，
     * 将所有数据处理并保存到 Redis 中去，Redis 数据设计如下：
     *
     * <ul>
     *     <li> Key: location:ip_to_cityid</li>
     *     <li> Type: ZSET</li>
     *     <li> Member: cityid_uuid</li>
     *     <li> Score: start_ip_num</li>
     * </ul>
     */
    @Override
    public Mono<Void> readBlocksFromFile()
    {
        return
        Flux.using(
            () -> this.getCSVParserFromFile("GeoLiteCity-Blocks.csv"),
            this::saveBlocksFromCSVParser,
            this::closeCSVParser)
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * （生产环境用）
     * 从文件系统中读取 GeoLiteCity-Blocks.csv 文件，
     * 将所有数据处理并保存到 Redis 中去，Redis 数据设计如下：
     *
     * <ul>
     *     <li> Key: location:ip_to_cityid</li>
     *     <li> Type: ZSET</li>
     *     <li> Member: cityid_uuid</li>
     *     <li> Score: start_ip_num</li>
     * </ul>
     */
    @Override
    public Mono<Void> readBlocksFromClassPath()
    {
        return
        Flux.using(
            () -> this.getCSVParserFromClassPath("GeoLiteCity-Blocks.csv"),
            this::saveBlocksFromCSVParser,
            this::closeCSVParser)
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * （测试时用）
     * 读取 GeoLiteCity-Location.csv 文件，
     * 将所有数据处理并保存到 Redis 中去，Redis 数据设计如下：
     *
     * <ul>
     *     <li>Key: location:cityid_to_cityname</li>
     *     <li>Type: Hash</li>
     *     <li>Hash-Key: cityid</li>
     *     <li>
     *         Hash-Value: (JSON)
     *         {country, region, city, postalCode, latitude, longitude, metroCode, areaCode}
     *     </li>
     * </ul>
     */
    @Override
    public Mono<Void> readLocationFromFile()
    {
        return
        Flux.using(
            () -> this.getCSVParserFromFile("GeoLiteCity-Location.csv"),
            this::saveLocationFromParser,
            this::closeCSVParser)
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * （生产环境用）
     * 读取 GeoLiteCity-Location.csv 文件，
     * 将所有数据处理并保存到 Redis 中去，Redis 数据设计如下：
     *
     * <ul>
     *     <li>Key: location:cityid_to_cityname</li>
     *     <li>Type: Hash</li>
     *     <li>Hash-Key: cityid</li>
     *     <li>
     *         Hash-Value: (JSON)
     *         {country, region, city, postalCode, latitude, longitude, metroCode, areaCode}
     *     </li>
     * </ul>
     */
    @Override
    public Mono<Void> readLocationFromClassPath()
    {
        return
        Flux.using(
            () -> this.getCSVParserFromClassPath("GeoLiteCity-Location.csv"),
                this::saveLocationFromParser,
                this::closeCSVParser)
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}
