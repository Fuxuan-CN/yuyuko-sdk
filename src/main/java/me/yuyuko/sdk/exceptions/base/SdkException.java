package me.yuyuko.sdk.exceptions.base;

/**
 * 所有SDK错误的基类
 */
public class SdkException extends Exception {

    /**
     * 构造一个没有详细信息的新 SdkException。
     */
    public SdkException() {
        super();
    }

    /**
     * 使用指定的详细信息构造一个新的 SdkException。
     *
     * @param message 详细信息
     */
    public SdkException(String message) {
        super(message);
    }

    /**
     * 使用指定的详细信息和原因构造一个新的 SdkException。
     *
     * @param message 详细信息
     * @param cause   异常原因
     */
    public SdkException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定的原因和详细信息（(cause==null ? null : cause.toString())，通常包含原因的类和详细信息）构造一个新的 SdkException。
     *
     * @param cause 异常原因
     */
    public SdkException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定的详细信息、原因、是否启用抑制和是否可写堆栈跟踪构造一个新的 SdkException。
     *
     * @param message            详细信息
     * @param cause              异常原因
     * @param enableSuppression  是否启用抑制
     * @param writableStackTrace 堆栈跟踪是否可写
     */
    public SdkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * 使用详细信息记录异常。
     */
    public void log() {
        // 使用详细信息记录异常
        System.err.println("SdkException 发生: " + getMessage());
        if (getCause() != null) {
            System.err.println("原因: " + getCause().getMessage());
        }
    }
}
