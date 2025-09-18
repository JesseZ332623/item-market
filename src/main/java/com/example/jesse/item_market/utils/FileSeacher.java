package com.example.jesse.item_market.utils;

import io.github.jessez332623.excel_to_markdown.exception.NotSupportFileExtension;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.jessez332623.excel_to_markdown.utils.FileExtensionChecker.extractFileExtension;

/** 文件查询操作工具类。*/
@Slf4j
public class FileSeacher
{
    /**
     * 查找 directoryPath 下所有与 extensions 文件扩展名集合匹配的文件名，
     * 以列表的形式返回。
     */
    public static List<String>
    findFilesByExtension(String directoryPath, Set<String> extensions)
    {
        try (Stream<Path> stream = Files.walk(Paths.get(directoryPath).normalize()))
        {
            return
            stream.filter(Files::isRegularFile)
                  .map((path) ->
                      path.getFileName().toString())
                  .filter(fileName -> {
                      try {
                          return
                          extensions.contains(extractFileExtension(fileName));
                      }
                      catch (NotSupportFileExtension notSupport)
                      {
                          log.error(
                              "{}", notSupport.getMessage(), notSupport
                          );

                          return false;
                      }
                  })
                 .collect(Collectors.toList());
        }
        catch (IOException exception)
        {
            final String errorMessage
                = String.format(
                "Find file by extensions failed! Caused by: %s",
                exception.getMessage()
            );

            log.error(errorMessage, exception);

            throw new
                RuntimeException(errorMessage, exception);
        }
    }
}