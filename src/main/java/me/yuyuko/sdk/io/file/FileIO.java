package me.yuyuko.sdk.io.file;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 让文件处理变得更加简单!
 * @author castorice (遐蝶)
 */
public class FileIO {
    private final File file;
    private final ReentrantReadWriteLock lock;
    private final Charset charset;

    public FileIO(String path, Charset charset) {
        this.file = new File(path);
        this.lock = new ReentrantReadWriteLock();
        this.charset = charset;
    }

    public FileIO(String path) {
        this(path, StandardCharsets.UTF_8); // 使用默认的UTF-8字符集
    }

    public String read() throws IOException {
        lock.readLock().lock();
        try {
            return Files.readString(file.toPath(), charset);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void write(String content) throws IOException {
        lock.writeLock().lock();
        try {
            Files.writeString(file.toPath(), content, charset);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void append(String content) throws IOException {
        lock.writeLock().lock();
        try {
            Files.writeString(file.toPath(), content, charset, StandardOpenOption.APPEND);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean exists() {
        return file.exists();
    }

    public boolean delete() {
        lock.writeLock().lock();
        try {
            return file.delete();
        } finally {
            lock.writeLock().unlock();
        }
    }
}