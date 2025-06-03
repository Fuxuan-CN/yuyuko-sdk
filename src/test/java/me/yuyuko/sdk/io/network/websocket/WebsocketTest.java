package me.yuyuko.sdk.io.network.websocket;

import me.yuyuko.sdk.exceptions.network.websocket.WebsocketConnectionException;
import me.yuyuko.sdk.time.TimeDelta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebsocketTest {

    private Websocket websocket;

    @BeforeEach
    void setUp() {
        // 初始化 Websocket 实例
        websocket = new Websocket("wss://echo.websocket.org/");
    }

    void log(String message)
    {
        System.out.println("[TEST LOG]: " + message);
    }

    @Test
    void testIsConnected() {
        // 测试 isConnected 方法
        log("ensure is connected.");
        assertFalse(websocket.isConnected(), "Websocket should not be connected initially");
        log("ensure success");
    }

    @Test
    void testConnect() {
        // 测试 connect 方法
        try {
            log("starting connection.");
            websocket.connect();
            assertTrue(websocket.isConnected(), "Websocket should be connected after calling connect");
            log("connection success.");
        } catch (WebsocketConnectionException e) {
            fail("Websocket connection should not throw an exception");
        }
    }

    @Test
    void testDisconnect() {
        // 测试 disconnect 方法
        try {
            log("testing disconnect.");
            websocket.connect();
            assertTrue(websocket.isConnected(), "Websocket should be connected initially");
            websocket.disconnect(1000, "Normal closure");
            assertFalse(websocket.isConnected(), "Websocket should not be connected after disconnect");
            log("testing disconnect success.");
        } catch (WebsocketConnectionException e) {
            fail("Websocket connection should not throw an exception");
        }
    }

    @Test
    void testSend() {
        // 测试 send 方法
        try {
            log("testing send.");
            websocket.connect();
            websocket.send("Hello", true);
            log("send success.");
        } catch (WebsocketConnectionException e) {
            fail("Websocket send should not throw an exception");
        }
    }

    @Test
    void testSendJson()
    {
        try {
            log("testing send json.");
            websocket.connect();
            HashMap<String, String> json_dat = new HashMap<String, String>();
            json_dat.put("key1", "value1");
            websocket.send(json_dat, true);
            log("received: "+ websocket.recv(1, TimeUnit.SECONDS));
            log("received: "+ websocket.recv(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            fail("send json throw an exception");
        }
        
    }

    @Test
    void testSendAsync() {
        // 测试 sendAsync 方法
        try {
            log("testing sendAsync.");
            websocket.connect();
            CompletableFuture<?> future = websocket.sendAsync("Hello", true);
            assertNotNull(future, "Future should not be null");
            assertTrue(future.isDone(), "Future should be completed");
            log("testing sendAsync success.");
        } catch (WebsocketConnectionException e) {
            fail("Websocket sendAsync should not throw an exception");
        }
    }

    @Test
    void testRecv() {
        // 测试 recv 方法
        try {
            log("testing recv.");
            websocket.connect();
            Object message = websocket.recv(1, TimeUnit.SECONDS);
            assertNotNull(message, "Received message should not be null");
            log("testing recv success.");
        } catch (WebsocketConnectionException | InterruptedException e) {
            fail("Websocket recv should not throw an exception");
        }
    }

    @Test
    void testRecvAsync() {
        // 测试 recvAsync 方法
        try {
            log("testing recvAsync.");
            websocket.connect();
            CompletableFuture<Object> future = websocket.recvAsync(new TimeDelta().seconds(1).toTimeUnit(TimeUnit.SECONDS), TimeUnit.SECONDS);
            assertNotNull(future, "Future should not be null");
            Thread.sleep(1000);
            assertTrue(future.isDone(), "Future should be completed");
            log("testing recvAsync success.");
        } catch (WebsocketConnectionException | InterruptedException e) {
            fail("Websocket recvAsync should not throw an exception");
        }
    }

    @Test
    void testSetSubProtocol() {
        // 测试 setSubProtocol 方法
        log("testing set sub protocol");
        Websocket websocketWithSubProtocol = websocket.setSubProtocol("subprotocol1", "subprotocol2");
        assertNotNull(websocketWithSubProtocol, "Websocket with subprotocol should not be null");
    }

    @Test
    void testAddHeader() {
        log("testing add header.");
        // 测试 addHeader 方法
        Websocket websocketWithHeader = websocket.addHeader("Authorization", "Bearer token");
        assertNotNull(websocketWithHeader, "Websocket with header should not be null");
    }

    @Test
    void testEventCallbacks() {
        // 测试事件回调
        log("set event callbacks.");
        Runnable openCallback = mock(Runnable.class);
        Consumer<String> closeCallback = mock(Consumer.class);
        Consumer<Object> messageCallback = mock(Consumer.class);
        Consumer<Throwable> errorCallback = mock(Consumer.class);

        websocket.onOpen(openCallback);
        websocket.onClose(closeCallback);
        websocket.onMessage(messageCallback);
        websocket.onError(errorCallback);

        // 模拟事件触发
        try
        {
            log("set event call triggers.");
            websocket.connect();
            verify(openCallback).run();

            websocket.disconnect(1000, "Normal closure");
            verify(closeCallback).accept("Normal closure");

            websocket.send("Hello", true);
            verify(messageCallback).accept("Hello");

            websocket.onError(error -> System.err.println(error));
            verify(errorCallback).accept(any(Throwable.class));
        }
        catch (Exception err)
        {
            err.printStackTrace();
        }
    }
}