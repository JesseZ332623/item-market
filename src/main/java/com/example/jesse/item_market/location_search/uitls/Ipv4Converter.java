package com.example.jesse.item_market.location_search.uitls;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** IPV4 转换器。*/
public class Ipv4Converter
{
    @Contract(pure = true)
    public static @NotNull String
    IntToIPV4(int ipNumber)
    {
        return
        ((ipNumber >> 24) & 0xFF) + "." +
        ((ipNumber >> 16) & 0xFF) + "." +
        ((ipNumber >> 8)  & 0xFF) + "." +
        (ipNumber         & 0xFF);
    }

    public static int
    IPV4ToInt(@NotNull String ipv4String)
    {
        if (ipv4String.trim().isEmpty()) {
            throw new IllegalArgumentException("IPv4 address not be empty!");
        }

        int result = 0;

        String[] ipParts
            = ipv4String.trim().split("\\.");

        if (ipParts.length != 4) {
            throw new IllegalArgumentException("IPv4 address must include 4 parts!");
        }

        for (String ipPart : ipParts)
        {
            if (!ipPart.matches("\\d+"))
            {
                throw new IllegalArgumentException(
                    "IPv4 address include non-number character!"
                );
            }

            int partNumber;

            try {
                partNumber = Integer.parseInt(ipPart);
            }
            catch (NumberFormatException exception)
            {
                throw new IllegalArgumentException(
                    "Invalid IP part number!" + ipPart, exception
                );
            }

            if (partNumber < 0 || partNumber > 255)
            {
                throw new IllegalArgumentException(
                    "Value of IP part number must betwwen 0 and 255!"
                );
            }

            result = result * 256 + partNumber;
        }

        return result;
    }
}
