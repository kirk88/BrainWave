package com.brain.wave.util;

import java.math.BigInteger;

public class BitConverter {

    public static String generateKString(String binary){
        // k is the smallest positive number the actual binary scheme cannot represent
        // for 1101 it is 1000
        StringBuilder kStr = new StringBuilder("1");
        for(int i = 1; i < binary.length(); i++)
            kStr.append("0");
        return kStr.toString();
    }

    public static BigInteger convertSignedBigInt(String binary){
        BigInteger bInt = new BigInteger(binary, 2);
        String kStr = generateKString(binary);
        BigInteger k = new BigInteger(kStr, 2);
        if (bInt.compareTo(k) > 0) bInt = bInt.subtract(k.multiply(new BigInteger("2")));
        return bInt;
    }

    public static int convertSignedInt(String binary){
        int i = Integer.parseInt(binary, 2);
        String kStr = generateKString(binary);
        int k =  Integer.parseInt(kStr, 2);
        if (i >= k) i -= 2 * k;
        return i;
    }

    public static int interpret24bitAsInt32(byte[] byteArray) {
        int newInt = (
                ((0xFF & byteArray[0]) << 16) |
                        ((0xFF & byteArray[1]) << 8) |
                        (0xFF & byteArray[2])
        );

        if ((newInt & 0x00800000) > 0) {
            newInt |= 0xFF000000;
        } else {
            newInt &= 0x00FFFFFF;
        }

        return newInt;
    }

}
