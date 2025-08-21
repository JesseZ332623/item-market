package com.example.jesse.item_market.location_search.uitls;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** IPV4 转换器。*/
public class Ipv4Converter
{
    @Contract(pure = true)
    public static @NotNull String
    intToIPv4(long ipNumber)
    {
        byte[] ipBytes = new byte[4];
        int    offset  = 24;
        StringBuilder resBuilder = new StringBuilder();

        for (int index = 0; index < ipBytes.length; ++index)
        {
            ipBytes[index] = (byte) ((ipNumber >> offset) & 0xFF);
            offset         = offset - 8;
        }

        for (int index = 0; index < ipBytes.length; ++index)
        {
            resBuilder.append(ipBytes[index] & 0xFF);

            if (index < 3) {
                resBuilder.append(".");
            }
        }

        return resBuilder.toString();

    }

    public static long
    ipv4ToInt(@NotNull String ipv4String)
    {
        if (ipv4String.trim().isEmpty()) {
            throw new IllegalArgumentException("IPv4 address not be empty!");
        }

        long result = 0;

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

            if (ipPart.length() > 1 && ipPart.startsWith("0"))
            {
                throw new IllegalArgumentException(
                    "Leading zeros are not allowed in IP parts."
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
                    "Value of IP part number must between 0 and 255!"
                );
            }

            result = result * 256 + partNumber;
        }

        return result;
    }
}
