package com.example.jesse.item_market.location_search.impl;

import com.example.jesse.item_market.location_search.dto.IpToCityID;
import com.example.jesse.item_market.location_search.dto.LocationInfo;
import com.example.jesse.item_market.location_search.exception.CSVFileNotInClassPath;
import jakarta.annotation.PostConstruct;
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
import static com.example.jesse.item_market.utils.UUIDGenerator.generateAsHex;
import static java.lang.String.format;

/** 地理信息相关 CSV 文件读取并存储至 Redis 实现。*/
@Slf4j
@Component
final public class GeoLiteCityReader
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

    /** 以城市 ID 为成员，IPv4 地址为分数的有序集合键。*/
    private static final String blocksRedisKey
        = "location:ip_to_cityid";

    /** 以城市 ID 为哈希键，对应的城市详细地理信息为哈希值的散列表键。*/
    private static final String locationRedisKey
        = "location:cityid_to_locationinfo";

    /** 每一批数据的缓冲区大小。*/
    private static final int MAX_BUFFER_SIZE = 30000;

    /** 延迟调用 excuteLoad() 方法的时间。*/
    private static final Duration INIT_DELAY
        = Duration.ofSeconds(5L);

    /** 字符串序列化 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, String> stringRedisTemplate;

    /** 通用 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 若 CSV 中的某字符串为空，返回 N/A 作为默认值。*/
    private static String
    setDefaultForString(@NotNull String csvField) {
        return (csvField.trim().isEmpty()) ? "N/A" : csvField;
    }

    /** 若 CSV 中的某整数值为空，返回 0 作为默认值。*/
    private static String
    setDefaultForInteger(@NotNull String csvField) {
        return (csvField.trim().isEmpty()) ? "0" : csvField;
    }

    /** 若 CSV 中的某浮点数为空，返回 0.0 作为默认值。*/
    private static String
    setDefaultForDouble(@NotNull String csvField) {
        return (csvField.trim().isEmpty()) ? "0.0" : csvField;
    }

    /**
     * 从 GeoLiteCity-Blocks.csv 文件中构建出 csvParser 后，
     * 正式的开始读取并存入 Redis。
     */
    private @NotNull Mono<Void>
    saveBlocksFromCSVParser(CSVParser csvParser)
    {
        return
        this.redisTemplate
            .hasKey(blocksRedisKey)
            .flatMap((isExist) -> {
                if (isExist) {
                    log.info("saveBlocksFromCSVParser() Key {} exist!", blocksRedisKey);
                    return Mono.empty();
                }

                return
                Flux.fromIterable(csvParser)
                    .parallel()
                    .runOn(Schedulers.parallel())
                    .map((record) ->
                        new IpToCityID()
                            .setStartIpScore(Double.parseDouble(record.get("startIpNum")))
                            .setLocalId(record.get("locId") + "_" + generateAsHex()))
                    .sequential()
                    .buffer(MAX_BUFFER_SIZE)
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
                            .timeout(Duration.ofSeconds(30L))
                            .onErrorResume((excption) ->
                                redisGenericErrorHandel(excption, null));
                    }).then();
            });
    }

    /**
     * 从 GeoLiteCity-Location.csv 文件中构建出 csvParser 后，
     * 正式的开始读取并存入 Redis。
     */
    private @NotNull Mono<Void>
    saveLocationFromParser(CSVParser csvParser)
    {
        return
        this.redisTemplate
            .hasKey(locationRedisKey)
            .flatMap((isExist) -> {
                if (isExist) {
                    log.info("saveLocationFromParser() Key {} exist!", locationRedisKey);
                    return Mono.empty();
                }

                return
                Flux.fromIterable(csvParser)
                    .parallel()
                    .runOn(Schedulers.parallel())
                    .map((record) -> {
                        /* CSV 文件中有空串，需要做一下判断，对于没有的值需要填一个默认值进去。*/
                        return new LocationInfo()
                            .setLocationId(record.get("locId"))
                            .setCountry(setDefaultForString(record.get("country")))
                            .setRegion(setDefaultForString(record.get("region")))
                            .setCity(setDefaultForString(record.get("city")))
                            .setPostalCode(setDefaultForString(record.get("postalCode")))
                            .setLatitude(Double.parseDouble(setDefaultForDouble(record.get("latitude"))))
                            .setLongitude(Double.parseDouble(setDefaultForDouble(record.get("longitude"))))
                            .setMetroCode(Integer.parseInt(setDefaultForInteger(record.get("metroCode"))))
                            .setAreaCode(Integer.parseInt(setDefaultForInteger(record.get("areaCode"))));
                    })
                    .sequential()
                    .buffer(MAX_BUFFER_SIZE)
                    .flatMap((records) -> {
                        Map<String, LocationInfo> recordsMap
                            = IntStream.range(0, records.size())
                            .boxed()
                            .collect(
                                Collectors.toMap(
                                    (i) -> records.get(i).getLocationId(),
                                    records::get
                                )
                            );

                        return
                        this.redisTemplate
                            .opsForHash()
                            .putAll(locationRedisKey, recordsMap)
                            .timeout(Duration.ofSeconds(30L))
                            .onErrorResume((excption) ->
                                redisGenericErrorHandel(excption, null));
                    }).then();
            });
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
    private @NotNull Mono<Void>
    readBlocksFromFile()
    {
        Mono<Void> processing
            = Flux.using(
                () -> this.getCSVParserFromFile("GeoLiteCity-Blocks.csv"),
                this::saveBlocksFromCSVParser,
                this::closeCSVParser)
            .subscribeOn(Schedulers.boundedElastic())
            .then();

        return
        processing.then(
            Mono.fromRunnable(() ->
                log.info(
                    "Sava GeoLiteCity-Blocks.csv (from filesystem)" +
                    " data to redis complete!"
                )
            )
        );
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
    private @NotNull Mono<Void>
    readBlocksFromClassPath()
    {
        Mono<Void> processing
            = Flux.using(
                () -> this.getCSVParserFromClassPath("GeoLiteCity-Blocks.csv"),
                this::saveBlocksFromCSVParser,
                this::closeCSVParser)
            .subscribeOn(Schedulers.boundedElastic())
            .then();

        return
        processing.then(
            Mono.fromRunnable(() ->
                log.info(
                    "Sava GeoLiteCity-Blocks.csv (from classpath)" +
                    " data to redis complete!"
                )
            )
        );
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
    private @NotNull Mono<Void>
    readLocationFromFile()
    {
        Mono<Void> processing
            = Flux.using(
                () -> this.getCSVParserFromFile("GeoLiteCity-Location.csv"),
                this::saveLocationFromParser,
                this::closeCSVParser)
            .subscribeOn(Schedulers.boundedElastic())
            .then();

        return
        processing.then(
            Mono.fromRunnable(() ->
                log.info(
                    "Sava GeoLiteCity-Location.csv (from filesystem)" +
                    " data to redis complete!"
                )
            )
        );
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
    private @NotNull Mono<Void>
    readLocationFromClassPath()
    {
        Mono<Void> processing
            = Flux.using(
                () -> this.getCSVParserFromClassPath("GeoLiteCity-Location.csv"),
                this::saveLocationFromParser,
                this::closeCSVParser)
            .subscribeOn(Schedulers.boundedElastic())
            .then();

        /* 任务处理放在弹性线程池，日志输出放在调用者线程池。*/
        return
        processing.then(
            Mono.fromRunnable(() ->
                log.info(
                    "Sava GeoLiteCity-Location.csv (from classpath)" +
                    " data to redis complete!"
                )
            )
        );
    }

    /**
     * 在服务器启动时，
     * 根据不同的环境自动执行 CSV 文件的读取与 Redis 的存储。
     */
    @PostConstruct
    public void excuteLoad()
    {
        log.info("Starting async CSV data loading...");

        Mono.delay(INIT_DELAY)
            .then(Mono.defer(() -> {
                switch (this.csvReadMode)
                {
                    case "filesystem" ->
                    {
                        return
                        Mono.when(
                            this.readBlocksFromFile(),
                            this.readLocationFromFile()
                        );
                    }

                    case "classpath" ->
                    {
                        return
                        Mono.when(
                            this.readBlocksFromClassPath(),
                            this.readLocationFromClassPath()
                        );
                    }

                    case null, default ->
                    {
                        return
                        Mono.error(
                            new UnsupportedOperationException(
                                format(
                                    "Unexpect csv read mode! (Which is: %s)" +
                                        "Load csv file failed!",
                                    this.csvReadMode
                                )
                            )
                        );
                    }
                }
            }))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            null,
            error -> log.error("CSV data loading failed", error)
        );
    }
}
