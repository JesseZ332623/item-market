package com.example.jesse.item_market.location_search.exception;

/**
 * 在没有把 CSV 文件打包进 JAR 的情况下，
 * 从 classpath 读取 .csv 文件失败时，抛出本异常。
 */
public class CSVFileNotInClassPath extends IllegalStateException
{
    public CSVFileNotInClassPath(String message) { super(message); }
    public CSVFileNotInClassPath(String message, Throwable throwable) {
        super(message, throwable);
    }
}
