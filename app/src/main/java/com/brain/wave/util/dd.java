package com.brain.wave.util;

import java.nio.ByteBuffer;

public class dd {

    static int readInt24(ByteBuffer buf)
    {
        byte[] data = new byte[3];
        for (int i = 0; i < 3; ++i)
            data[i] = buf.get();

        return (data[0] << 16)
                | (data[1] << 8 & 0xFF00)
                | (data[2] & 0xFF);
    }


}
