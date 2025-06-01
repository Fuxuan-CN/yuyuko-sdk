package me.yuyuko.sdk.time;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 一个用于表示时间间隔的类，提供多种时间单位的设置和运算方法。
 * <p>
 * 该类旨在简化时间间隔的处理，支持链式调用，方便构建和操作时间间隔。
 * <p>
 * 示例：
 * <pre>
 * TimeDelta timeDelta = new TimeDelta()
 *     .days(10)
 *     .hours(5)
 *     .minutes(30)
 *     .seconds(45);
 * System.out.println(timeDelta);  // 输出: PT251H30M45S
 * </pre>
 *
 * @author castorice (遐蝶)
 */
public class TimeDelta implements Comparable<TimeDelta>, Serializable, Cloneable {
    private Duration duration;

    @java.io.Serial
    private static final long serialVersionUID = -3042686055658047285L;

    /**
     * 构造一个默认的时间间隔，初始值为零。
     */
    public TimeDelta() {
        this.duration = Duration.ZERO;
    }

    private TimeDelta(Duration duration) {
        this.duration = duration;
    }

    /**
     * 添加纳秒到当前时间间隔。
     *
     * @param nanos 纳秒数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta nanos(long nanos) {
        this.duration = this.duration.plusNanos(nanos);
        return this;
    }

    /**
     * 添加毫秒到当前时间间隔。
     *
     * @param milliseconds 毫秒数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta milliseconds(long milliseconds) {
        this.duration = this.duration.plusMillis(milliseconds);
        return this;
    }

    /**
     * 添加秒到当前时间间隔。
     *
     * @param seconds 秒数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta seconds(long seconds) {
        this.duration = this.duration.plusSeconds(seconds);
        return this;
    }

    /**
     * 添加分钟到当前时间间隔。
     *
     * @param minutes 分钟数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta minutes(long minutes) {
        this.duration = this.duration.plusMinutes(minutes);
        return this;
    }

    /**
     * 添加小时到当前时间间隔。
     *
     * @param hours 小时数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta hours(long hours) {
        this.duration = this.duration.plusHours(hours);
        return this;
    }

    /**
     * 添加天数到当前时间间隔。
     *
     * @param days 天数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta days(long days) {
        this.duration = this.duration.plusDays(days);
        return this;
    }

    /**
     * 添加周数到当前时间间隔。
     *
     * @param weeks 周数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta weeks(long weeks) {
        this.duration = this.duration.plusDays(weeks * 7);
        return this;
    }

    /**
     * 添加月数到当前时间间隔。注意：这里假设每月为30天，这是一种简化处理。
     *
     * @param months 月数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta months(long months) {
        this.duration = this.duration.plusDays(months * 30); // 简化处理，假设每月30天
        return this;
    }

    /**
     * 添加年数到当前时间间隔。注意：这里假设每年为365天，这是一种简化处理。
     *
     * @param years 年数
     * @return 当前 TimeDelta 对象
     */
    public TimeDelta years(long years) {
        this.duration = this.duration.plusDays(years * 365); // 简化处理，假设每年365天
        return this;
    }

    /**
     * 将时间间隔转换为毫秒。
     *
     * @return 时间间隔的毫秒数
     */
    public long toMillis() {
        return duration.toMillis();
    }

    /**
     * 获取底层的 Java 原生 Duration 对象。
     *
     * @return 底层的 Duration 对象
     */
    public Duration getDuration() {
        return this.duration;
    }

    /**
     * 将时间间隔转换为指定的时间单位。
     *
     * @param unit 目标时间单位
     * @return 转换后的时间间隔值
     */
    public long toTimeUnit(TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return duration.toNanos();
            case MICROSECONDS:
                return duration.toNanos() / 1_000;
            case MILLISECONDS:
                return duration.toMillis();
            case SECONDS:
                return duration.getSeconds();
            case MINUTES:
                return duration.toMinutes();
            case HOURS:
                return duration.toHours();
            case DAYS:
                return duration.toDays();
            default:
                throw new IllegalArgumentException("Unsupported TimeUnit: " + unit);
        }
    }

    @Override
    public String toString() {
        return duration.toString();
    }

    @Override
    public int compareTo(TimeDelta other) {
        return Long.compare(this.toMillis(), other.toMillis());
    }

    /**
     * 将另一个 TimeDelta 对象加到当前时间间隔。
     *
     * @param other 另一个 TimeDelta 对象
     * @return 新的 TimeDelta 对象，表示两个时间间隔的和
     */
    public TimeDelta plus(TimeDelta other) {
        return new TimeDelta(this.duration.plus(other.duration));
    }

    /**
     * 从当前时间间隔减去另一个 TimeDelta 对象。
     *
     * @param other 另一个 TimeDelta 对象
     * @return 新的 TimeDelta 对象，表示两个时间间隔的差
     */
    public TimeDelta minus(TimeDelta other) {
        return new TimeDelta(this.duration.minus(other.duration));
    }

    /**
     * 将当前时间间隔乘以一个因子。
     *
     * @param factor 乘数
     * @return 新的 TimeDelta 对象，表示乘法结果
     */
    public TimeDelta multiply(long factor) {
        return new TimeDelta(this.duration.multipliedBy(factor));
    }

    /**
     * 将当前时间间隔除以一个因子。注意：除数不能为零。
     *
     * @param divisor 除数
     * @return 新的 TimeDelta 对象，表示除法结果
     * @throws ArithmeticException 如果除数为零
     */
    public TimeDelta divide(long divisor) throws ArithmeticException {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return new TimeDelta(this.duration.dividedBy(divisor));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeDelta that = (TimeDelta) o;
        return this.duration.equals(that.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }

    @Override
    public TimeDelta clone() {
        return new TimeDelta(this.duration);
    }
}