package me.yuyuko.sdk.io.memory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 内存映射文件
 * @author castorice (遐蝶)
*/
public class MemoryMappedFile {
    private final File file;
    private final RandomAccessFile randomAccessFile;
    private final FileChannel fileChannel;
    private final MappedByteBuffer buffer;

    public MemoryMappedFile(String filePath, long size) throws IOException {
        this.file = new File(filePath);
        this.randomAccessFile = new RandomAccessFile(file, "rw");
        this.fileChannel = randomAccessFile.getChannel();
        this.buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, size);
    }

    public void write(int index, byte value) {
        buffer.put(index, value);
    }

    public byte read(int index) {
        return buffer.get(index);
    }

    public void close() throws IOException {
        fileChannel.close();
        randomAccessFile.close();
    }
}