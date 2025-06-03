package me.yuyuko.sdk.interfaces.io.network.websocket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import me.yuyuko.sdk.exceptions.network.websocket.WebsocketConnectionException;

/*
 * 如果你想实现自己的websocket，请实现这个接口
*/
public interface IWebsocket {
    boolean isConnected();
    void connect() throws WebsocketConnectionException;
    void disconnect(int status, String reason);
    void send(Object message, boolean last) throws WebsocketConnectionException;
    CompletableFuture<?> sendAsync(Object message, boolean last) throws WebsocketConnectionException;
    Object recv(long timeout, TimeUnit timeUnit) throws WebsocketConnectionException, InterruptedException;
    CompletableFuture<Object> recvAsync(long waitTime, TimeUnit unit) throws WebsocketConnectionException;
    IWebsocket setSubProtocol(String... subProtocolName);
    IWebsocket addHeader(String key, String value);

    // 事件回调
    void onOpen(Runnable callback);
    void onClose(Consumer<String> callback);
    void onMessage(Consumer<Object> callback);
    void onError(Consumer<Throwable> callback);
}
