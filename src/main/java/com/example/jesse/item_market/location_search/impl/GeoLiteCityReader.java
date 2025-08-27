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
import org.springframework.data.redis.core.ScanOptions;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static com.example.jesse.item_market.location_search.uitls.ShardingOperator.*;
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
     * 正式的开始读取并存入 Redis（采用分片策略读取）。
     */
    private @NotNull Mono<Void>
    saveLocationFromParser(CSVParser csvParser)
    {
        return
        this.redisTemplate
            /* 由于数据采用了分片策略，判断数据是否存在的策略也要变。*/
            .scan(ScanOptions.scanOptions().match(locationRedisKey + ":*").build())
            .collectList()
            .map((keys) -> !keys.isEmpty())
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
                    /*
                     * 在存储每一个 LocationInfo 实例至 Redis 之前，
                     * 先计算它将会去到哪个分片，并将这个结果收集成一个：
                     *      Map<String, Collection<LocationInfo>> 类型
                     * 键代表 Redis 的 sharding-key，值代表该分片将要存储的所有数据。
                     */
                    .collectMultimap(
                        (info) ->
                            getShardingKey(Integer.parseInt(info.getLocationId())),
                        (info) -> info)
                    /*
                     * 将每一个分片中的数据数据部分转化成
                     *      Map<String, LocationInfo> 类型
                     * 键代表 locationId，值则是 LocationInfo 的实例，
                     * 对这个 Map 执行 putAll() 批量操作，能大幅减少与 Redis 的网络往返时间。
                     */
                    .flatMapMany((shardingMap) ->
                        Flux.fromIterable(shardingMap.entrySet())
                            /* 终极优化：并行！*/
                            .parallel()
                            .runOn(Schedulers.parallel())
                            .flatMap((entry) -> {
                                /* 该分片的 reids key */
                                final String shardingKey = entry.getKey();
                                /* 该分片的所有数据 */
                                final Collection<LocationInfo> infoInShard = entry.getValue();

                                Map<String, LocationInfo> records
                                    = infoInShard.stream()
                                        .collect(
                                            Collectors.toMap(
                                                LocationInfo::getLocationId,
                                                // 等价于 (locationInfo) -> locationInfo
                                                Function.identity()
                                            )
                                        );

                                return
                                this.redisTemplate
                                    .opsForHash()
                                    .putAll(shardingKey, records)
                                    .timeout(Duration.ofSeconds(20L))
                                    .onErrorResume((exception) ->
                                        redisGenericErrorHandel(exception, null));
                            })
                    ).then();
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
     *     <li>Key: location:cityid_to_locationinfo:{@literal sharding-id}</li>
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
     *     <li>Key: location:cityid_to_locationinfo:{@literal sharding-id}</li>
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
                        this.readBlocksFromFile()
                            .then(this.readLocationFromFile());
                    }

                    case "classpath" ->
                    {
                        return
                        this.readBlocksFromClassPath()
                            .then(this.readLocationFromClassPath());
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
