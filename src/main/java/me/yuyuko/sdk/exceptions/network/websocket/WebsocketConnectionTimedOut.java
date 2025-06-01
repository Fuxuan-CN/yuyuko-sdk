package me.yuyuko.sdk.exceptions.network.websocket;

/**
 * 当websocket连接超时的时候被抛出
*/
public class WebsocketConnectionTimedOut extends WebsocketConnectionException {
    
    public WebsocketConnectionTimedOut(String message)
    {
        super(message);
    }

    public WebsocketConnectionTimedOut(String message, Throwable cause)
    {
        super(message, cause);
    }
}
