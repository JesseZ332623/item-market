package com.example.jesse.item_market.email.utils;

import java.util.HashMap;
import java.util.Map;

/** Mine type 获取器。*/
public class MimeTypeGetter
{
    /** 默认的二进制流类型。*/
    private final static
    String DEFAULT_MIME_TYPE = "application/octet-stream";

    /** 常用的 Mine Type 映射表。*/
    private static final
    Map<String, String> MIME_TYPES = new HashMap<>();

    /* 在这个工具类构建时填入常用的 Mine Type 映射。*/
    static {
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("doc", "application/msword");
        MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPES.put("xls", "application/vnd.ms-excel");
        MIME_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("xml", "application/xml");
        MIME_TYPES.put("zip", "application/zip");
    }

    /** 通过文件名后缀尝试映射对应的 Mine Type。*/
    public static String
    getMimeTypeFromExtention(String fileName)
    {
        if (fileName == null || fileName.trim().isEmpty()) {
            return DEFAULT_MIME_TYPE;
        }

        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex > 0)
        {
            String extention
                = fileName.substring(dotIndex + 1)
                          .toLowerCase();

            return
            MIME_TYPES.getOrDefault(extention, DEFAULT_MIME_TYPE);
        }

        return DEFAULT_MIME_TYPE;
    }
}
