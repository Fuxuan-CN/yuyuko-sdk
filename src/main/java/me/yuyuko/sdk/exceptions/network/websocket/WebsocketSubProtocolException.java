package me.yuyuko.sdk.exceptions.network.websocket;

public class WebsocketSubProtocolException extends WebsocketConnectionException {
    public WebsocketSubProtocolException(String message)
    {
        super(message);
    }

    public WebsocketSubProtocolException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
