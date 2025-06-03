package me.yuyuko.sdk.io.network.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import me.yuyuko.sdk.interfaces.io.network.websocket.IWebsocket;
import me.yuyuko.sdk.exceptions.network.websocket.WebsocketConnectionException;
import me.yuyuko.sdk.exceptions.network.websocket.WebsocketConnectionTimedOut;
import me.yuyuko.sdk.time.TimeDelta;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NettyWebsocket implements IWebsocket {
    private final URI url;
    private final TimeDelta connectionTimeout;
    private final LinkedBlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean connected = false;
    private Channel channel;
    private String subProtocol;
    private final Map<String, String> headers = new HashMap<>();
    private final ObjectMapper defaultJsonMapper = new ObjectMapper();

    // 一些事件回调
    private Runnable onOpenCallback;
    private Consumer<String> onCloseCallback;
    private Consumer<Object> onMessageCallback;
    private Consumer<Throwable> onErrorCallback;

    public NettyWebsocket(String uri) {
        this(uri, new TimeDelta().seconds(30), null);
    }

    public NettyWebsocket(String uri, TimeDelta connectionTimeout, String subProtocol) {
        this.url = URI.create(uri);
        this.connectionTimeout = connectionTimeout;
        this.subProtocol = subProtocol;
    }

    @Override
    public boolean isConnected() {
        return connected && channel != null && channel.isActive();
    }

    @Override
    public void connect() throws WebsocketConnectionException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new WebSocketClientInitializer());

            channel = b.connect(url.getHost(), url.getPort()).sync().channel();

            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, url.toASCIIString(), Unpooled.wrappedBuffer(new byte[0]));
            request.headers().set(HttpHeaderNames.HOST, url.getHost());
            request.headers().set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
            request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_VERSION, "13");
            if (subProtocol != null) {
                request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, subProtocol);
            }
            if (!headers.isEmpty()) {
                headers.forEach(request.headers()::set);
            }

            channel.writeAndFlush(request);

            long timeoutMillis = connectionTimeout.toTimeUnit(TimeUnit.MILLISECONDS);
            if (!channel.closeFuture().await(timeoutMillis)) {
                throw new WebsocketConnectionTimedOut("Connect to " + url + " timed out after " + connectionTimeout);
            }
        } catch (Exception e) {
            throw new WebsocketConnectionException("Failed to connect to " + url, e);
        } finally {
            group.shutdownGracefully();
        }
    }

    @Override
    public void disconnect(int status, String reason) {
        if (isConnected()) {
            WebSocketFrame closeFrame = new CloseWebSocketFrame(status, reason);
            channel.writeAndFlush(closeFrame);
            channel.close();
        }
        connected = false;
    }

    @Override
    public void send(Object message, boolean last) throws WebsocketConnectionException {
        checkIsConnected();
        if (message instanceof String) {
            TextWebSocketFrame textFrame = new TextWebSocketFrame((String) message);
            channel.writeAndFlush(textFrame);
        } else if (message instanceof byte[]) {
            BinaryWebSocketFrame binaryFrame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer((byte[]) message));
            channel.writeAndFlush(binaryFrame);
        } else if (message instanceof Map) {
            String json_msg;
            try {
                json_msg = defaultJsonMapper.writeValueAsString(message);
                send(json_msg, last);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("failed to processing Map Type message when trying covering to json.", e);
            }
        }
        else {
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
        }
    }

    @Override
    public CompletableFuture<?> sendAsync(Object message, boolean last) throws WebsocketConnectionException {
        return CompletableFuture.runAsync(() -> {
            try {
                send(message, last);
            } catch (WebsocketConnectionException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Object recv(long timeout, TimeUnit timeUnit) throws WebsocketConnectionException, InterruptedException {
        checkIsConnected();
        return messageQueue.poll(timeout, timeUnit);
    }

    @Override
    public CompletableFuture<Object> recvAsync(long waitTime, TimeUnit unit) throws WebsocketConnectionException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return recv(waitTime, unit);
            } catch (InterruptedException | WebsocketConnectionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for message", e);
            }
        });
    }

    @Override
    public IWebsocket setSubProtocol(String... subProtocolName) {
        this.subProtocol = String.join(",", subProtocolName);
        return this;
    }

    @Override
    public IWebsocket addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public void onOpen(Runnable callback) {
        this.onOpenCallback = callback;
    }

    @Override
    public void onClose(Consumer<String> callback) {
        this.onCloseCallback = callback;
    }

    @Override
    public void onMessage(Consumer<Object> callback) {
        this.onMessageCallback = callback;
    }

    @Override
    public void onError(Consumer<Throwable> callback) {
        this.onErrorCallback = callback;
    }

    private void checkIsConnected() throws WebsocketConnectionException {
        if (!isConnected()) {
            throw new WebsocketConnectionException("WebSocket is not connected.");
        }
    }

    private class WebSocketClientInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("http-codec", new HttpClientCodec());
            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
            pipeline.addLast("ws-handler", new WebSocketClientHandler());
        }
    }

    private class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            connected = true;
            if (onOpenCallback != null) {
                onOpenCallback.run();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            connected = false;
            if (onCloseCallback != null) {
                onCloseCallback.accept("Connection closed");
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof TextWebSocketFrame) {
                TextWebSocketFrame frame = (TextWebSocketFrame) msg;
                String text = frame.text();
                messageQueue.offer(text);
                if (onMessageCallback != null) {
                    onMessageCallback.accept(text);
                }
            } else if (msg instanceof BinaryWebSocketFrame) {
                BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
                ByteBuf buffer = frame.content();
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                messageQueue.offer(bytes);
                if (onMessageCallback != null) {
                    onMessageCallback.accept(bytes);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            connected = false;
            if (onErrorCallback != null) {
                onErrorCallback.accept(cause);
            }
            ctx.close();
        }
    }
}