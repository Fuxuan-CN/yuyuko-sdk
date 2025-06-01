package me.yuyuko.sdk.exceptions;

import me.yuyuko.sdk.exceptions.base.SdkException;

/**
* 当StringUtil.fString格式化错误的时候引发这个异常
* @see me.yuyuko.sdk.utils.StringUtil#fString(String, java.util.Map)
*/
public class FormatException extends SdkException 
{

    public FormatException(String message)
    {
        super(message);
    }
    
    public FormatException(String message, Throwable cause) 
    {
        super(message, cause);
    }
}
