package me.yuyuko.sdk.exceptions.network.websocket;
import me.yuyuko.sdk.exceptions.base.SdkException;

/**
 * 当websocket连接错误的时候被抛出
*/
public class WebsocketConnectionException extends SdkException {
    
    public WebsocketConnectionException(String message)
    {
        super(message);
    }

    public WebsocketConnectionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
