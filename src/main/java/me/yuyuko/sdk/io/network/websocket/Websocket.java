package me.yuyuko.sdk.io.network.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.concurrent.TimeoutException;

import me.yuyuko.sdk.exceptions.network.websocket.WebsocketConnectionException;
import me.yuyuko.sdk.exceptions.network.websocket.WebsocketConnectionTimedOut;
import me.yuyuko.sdk.exceptions.network.websocket.WebsocketSubProtocolException;
import me.yuyuko.sdk.interfaces.io.network.websocket.IWebsocket;
import me.yuyuko.sdk.time.TimeDelta;

/**
 * 让websocket 连接变得更加简单！
 * @author castorice (遐蝶)
 * @param uri 连接到websocket的URL
 * @param connectionTimeout 连接超时时间
 * @see me.castorice.sdk.interfaces.io.network.websocket.IWebsocket
*/
public final class Websocket implements IWebsocket, AutoCloseable {
    private final HttpClient client;
    private WebSocket webSocket;
    private final URI url;
    private TimeDelta connectionTimeout;
    private final LinkedBlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean connected = false;
    private String subProtocol;
    private final Map<String, String> headers = new HashMap<>();
    private final ObjectMapper defaultJsonMapper = new ObjectMapper();

    // 一些事件回调
    private Runnable onOpenCallback;
    private Consumer<String> onCloseCallback;
    private Consumer<Object> onMessageCallback;
    private Consumer<Throwable> onErrorCallback;

    /**
     * 传入uri，默认超时时间30秒
     * @param uri
    */
    public Websocket(String uri) {
        this(uri, new TimeDelta().seconds(30));
    }

    /**
     * 传入uri，设置超时时间
     * @param uri
     * @param connectionTimeout
    */
    public Websocket(String uri, TimeDelta connectionTimeout) {
        this.url = URI.create(uri);
        this.connectionTimeout = connectionTimeout;
        this.client = HttpClient.newHttpClient();
    }

    /**
     * 支持子协议
    */
    public Websocket setSubProtocol(String... subProtocols) {
        this.subProtocol = String.join(",", subProtocols);
        return this;
    }

    /**
     * 添加子协议请求头
    */
    public Websocket addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /*
     * 断开websocket连接
    */
    @Override
    public void disconnect(int statusCode, String reason)
    {
        if (isConnected())
        {
            connected = false;
            webSocket.sendClose(statusCode, reason != null ? reason : "client want to disconnect.");
            webSocket = null;
        }
        client.close();
    }

    /**
     * 建立websocket连接
     * 请在调用此方法之前设置onOpen回调，确保它能被正确调用
     * @see Websocket#onOpen(Runnable)
    */
    public void connect() throws WebsocketConnectionException {

        WebSocket.Builder builder = client.newWebSocketBuilder();

        if (subProtocol != null) {
            builder.subprotocols(subProtocol);
        }

        headers.forEach(builder::header);
        CompletableFuture<WebSocket> wsFuture = builder.buildAsync(this.url, new WebsocketHandler());

        try 
        {
            this.webSocket = wsFuture.get(connectionTimeout.toTimeUnit(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
            this.connected = true;

            if (subProtocol != null)
            {
                String[] requestedSubProtocols = subProtocol.split(",");
                String selectedSubProtocol = webSocket.getSubprotocol();

                if (selectedSubProtocol == null || !Arrays.asList(requestedSubProtocols).contains(selectedSubProtocol))
                {
                    throw new WebsocketSubProtocolException("Requested subprotocols: " + subProtocol + ", but server selected: " + selectedSubProtocol);
                }
            }
        } 
        catch (TimeoutException e) 
        {
            throw new WebsocketConnectionTimedOut("Connect to " + this.url + " timed out after " + connectionTimeout, e);
        } 
        catch (Exception e) 
        {
            throw new WebsocketConnectionException("Failed to connect to " + this.url, e);
        }
    }

    private void checkIsConnected() throws WebsocketConnectionException
    {
        if (!isConnected())
        {
            throw new WebsocketConnectionException("websocket is not connect.");
        }
    }

    /**
     * 检查此ws连接是否是活跃状态
     * @return 如果连接在线则返回true，否则返回false
     */
    @Override
    public boolean isConnected() {
        return connected && webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed();
    }

    /**
     * 连接打开的时候调用的回调
     * 需要注意的是，你要在调用 connect 方法之前设置这个回调，否则这个将不会被调用
     * @see Websocket#connect()
    */
    @Override
    public void onOpen(Runnable callback) {
        this.onOpenCallback = callback;
    }

    /**
     * 连接关闭的时候调用的回调
    */
    @Override
    public void onClose(Consumer<String> callback) {
        this.onCloseCallback = callback;
    }

    /**
     * 收到消息的时候调用的回调
    */
    @Override
    public void onMessage(Consumer<Object> callback) {
        this.onMessageCallback = callback;
    }

    /**
     * 连接发生错误的时候调用的回调
    */
    @Override
    public void onError(Consumer<Throwable> callback) {
        this.onErrorCallback = callback;
    }

    /**
     * 同步发送消息
    */
    @Override
    public void send(Object message, boolean last) throws WebsocketConnectionException, IllegalArgumentException {
        checkIsConnected();
        Objects.requireNonNull(message);
        CompletableFuture<?> future = sendInternal(message, last);
        future.join();
    }

    /**
     * 异步发送消息
    */
    @Override
    public CompletableFuture<?> sendAsync(Object message, boolean last) throws WebsocketConnectionException, IllegalArgumentException {
        checkIsConnected();
        Objects.requireNonNull(message);
        return sendInternal(message, last);
    }

    /**
     * 发送消息的内部实现
    */
    private CompletableFuture<?> sendInternal(Object message, boolean last) throws IllegalArgumentException {
        if (message instanceof String) {
            return webSocket.sendText((String) message, last);
        } else if (message instanceof ByteBuffer) {
            return webSocket.sendBinary((ByteBuffer) message, last);
        } else if (message instanceof byte[]) {
            return webSocket.sendBinary(ByteBuffer.wrap((byte[]) message), last);
        } else if (message instanceof Map) {
            try {
                String json = defaultJsonMapper.writeValueAsString(message);
                return webSocket.sendText(json, last);
            } catch (IOException e) {
                CompletableFuture<?> failed = new CompletableFuture<>();
                failed.completeExceptionally(new IllegalArgumentException("Failed to convert Map to JSON", e));
                return failed;
            }
        } else {
            CompletableFuture<?> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("Unsupported message type: " + message.getClass()));
            return failed;
        }
    }

    /**
     * 同步接收消息（阻塞，直到有消息或超时）
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @return 如果在超时时间内有消息，返回消息；否则返回null
     * @throws InterruptedException 如果线程被中断
     */
    public Object recv(long timeout, TimeUnit timeUnit) throws InterruptedException, WebsocketConnectionException {
        checkIsConnected();
        return messageQueue.poll(timeout, timeUnit);
    }

    /**
     * 关闭ws连接，同时关闭HttpClient
    */
    @Override
    public void close()
    {
        if (isConnected())
        {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "client want to close the connection.");
            webSocket = null;
        }
        client.close();
    }

    /**
     * 异步接收消息
    */
    @Override
    public CompletableFuture<Object> recvAsync(long waitTime, TimeUnit unit) throws WebsocketConnectionException, RuntimeException {
        checkIsConnected();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return messageQueue.poll(waitTime, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for message", e);
            }
        });
    }

    private class WebsocketHandler implements Listener {
        private final StringBuilder textBuilder = new StringBuilder();
        private ByteBuffer binaryBuffer = null;

        public WebsocketHandler() {}

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuilder.append(data);
            if (last) {
                String message = textBuilder.toString();
                messageQueue.offer(message);
                textBuilder.setLength(0);
                if (onMessageCallback != null) {
                    onMessageCallback.accept(message);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (binaryBuffer == null) {
                binaryBuffer = ByteBuffer.allocate(data.remaining());
                binaryBuffer.put(data);
            } else {
                ByteBuffer newBuffer = ByteBuffer.allocate(binaryBuffer.position() + data.remaining());
                binaryBuffer.flip();
                newBuffer.put(binaryBuffer);
                newBuffer.put(data);
                binaryBuffer = newBuffer;
            }
            if (last) {
                binaryBuffer.flip();
                byte[] bytes = new byte[binaryBuffer.remaining()];
                binaryBuffer.get(bytes);
                messageQueue.offer(bytes);
                if (onMessageCallback != null) {
                    onMessageCallback.accept(bytes);
                }
                binaryBuffer = null;
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            if (onOpenCallback != null) {
                onOpenCallback.run();
            }
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected = false;

            String effectiveReason = reason != null && !reason.isEmpty() ? reason : "Normal closure";
            
            if (onCloseCallback != null) {
                onCloseCallback.accept(effectiveReason);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected = false;
            if (onErrorCallback != null) {
                onErrorCallback.accept(error);
            }
        }
    }
}