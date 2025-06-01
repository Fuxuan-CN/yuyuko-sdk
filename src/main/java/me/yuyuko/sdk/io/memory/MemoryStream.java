package me.yuyuko.sdk.io.memory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 内存流操作
 * @author castorice (遐蝶)
*/
public class MemoryStream {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayInputStream inputStream;

    public MemoryStream(byte[] data) {
        this.inputStream = new ByteArrayInputStream(data);
    }

    public MemoryStream() {
        this(new byte[0]);
    }

    public void write(byte[] data) throws IOException {
        outputStream.write(data);
    }

    public byte[] toByteArray() {
        return outputStream.toByteArray();
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}