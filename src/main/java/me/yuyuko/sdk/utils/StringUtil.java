package me.yuyuko.sdk.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import me.yuyuko.sdk.exceptions.FormatException;

final class TemplateCacheKey {
    private final String template;
    private final Map<String, Object> values;

    public TemplateCacheKey(String template, Map<String, Object> values) {
        this.template = template;
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateCacheKey that = (TemplateCacheKey) o;
        return template.equals(that.template) && values.keySet().equals(that.values.keySet());
    }

    @Override
    public int hashCode() {
        return 31 * template.hashCode() + values.keySet().hashCode();
    }
}

/**
 * 让字符串处理更简单！
*/
public final class StringUtil {

    private static final ConcurrentMap<TemplateCacheKey, String> cache = new ConcurrentHashMap<>();

    public static String fString(String template, Map<String, Object> values) throws FormatException {
        if (template == null) return null;
        if (values == null) values = Map.of();

        TemplateCacheKey cacheKey = new TemplateCacheKey(template, values);
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        StringBuilder result = new StringBuilder(template.length() + 16);
        int len = template.length();
        int i = 0;
        while (i < len) {
            char c = template.charAt(i);
            if (c == '{') {
                if (i + 1 < len && template.charAt(i + 1) == '{') {
                    result.append('{');
                    i += 2;
                } else {
                    int end = i + 1;
                    int braceCount = 1;
                    while (end < len && braceCount > 0) {
                        char ch = template.charAt(end);
                        if (ch == '{') braceCount++;
                        else if (ch == '}') braceCount--;
                        if (braceCount == 0) break;
                        end++;
                    }
                    if (braceCount != 0 || end > len) {
                        throw new FormatException("模板中有未匹配的 '{'");
                    }
                    String expr = template.substring(i + 1, end).trim();
                    if (expr.isEmpty()) {
                        throw new FormatException("模板中有空的占位符");
                    }
                    String keyOrExpr = expr, format = null;
                    int colon = expr.indexOf(':');
                    if (colon != -1) {
                        keyOrExpr = expr.substring(0, colon).trim();
                        format = expr.substring(colon + 1).trim();
                    }
                    Object value;
                    if (isSimpleIdentifier(keyOrExpr)) {
                        value = values.get(keyOrExpr);
                        if (value == null && !values.containsKey(keyOrExpr)) {
                            throw new FormatException("在值映射中找不到键 '" + keyOrExpr + "'");
                        }
                    } else {
                        value = evalJavaExpression(keyOrExpr, values);
                    }
                    String replacement;
                    if (value == null) {
                        replacement = "null";
                    } else if (format != null && !format.isEmpty()) {
                        replacement = applyFormat(value, format);
                    } else {
                        replacement = String.valueOf(value);
                    }
                    result.append(replacement);
                    i = end + 1;
                }
            } else if (c == '}') {
                if (i + 1 < len && template.charAt(i + 1) == '}') {
                    result.append('}');
                    i += 2;
                } else {
                    throw new FormatException("模板中出现单独的 '}'");
                }
            } else {
                result.append(c);
                i++;
            }
        }
        String finalResult = result.toString();
        cache.putIfAbsent(cacheKey, finalResult);
        return finalResult;
    }

    private static boolean isSimpleIdentifier(String s) {
        if (s == null || s.isEmpty()) return false;
        char[] chars = s.toCharArray();
        if (!Character.isJavaIdentifierStart(chars[0])) return false;
        for (int i = 1; i < chars.length; i++) {
            if (!Character.isJavaIdentifierPart(chars[i])) return false;
        }
        return true;
    }

    private static String applyFormat(Object value, String format) throws FormatException {
        try {
            if (value == null) return "null";
            String fmt = "%" + pythonToJavaFormat(format, value);
            return String.format(fmt, value);
        } catch (Exception e) {
            throw new FormatException("无效的格式: " + format + "，值为: " + value);
        }
    }

    private static String pythonToJavaFormat(String format, Object value) {
        StringBuilder sb = new StringBuilder();
        char align = 0;
        int width = -1;
        int precision = -1;
        char type = 0;

        int i = 0, len = format.length();
        if (i < len && (format.charAt(i) == '<' || format.charAt(i) == '>')) {
            align = format.charAt(i++);
        }
        StringBuilder num = new StringBuilder();
        while (i < len && Character.isDigit(format.charAt(i))) {
            num.append(format.charAt(i++));
        }
        if (num.length() > 0) width = Integer.parseInt(num.toString());
        if (i < len && format.charAt(i) == '.') {
            i++;
            num.setLength(0);
            while (i < len && Character.isDigit(format.charAt(i))) {
                num.append(format.charAt(i++));
            }
            if (num.length() > 0) precision = Integer.parseInt(num.toString());
        }
        if (i < len) type = format.charAt(i);

        if (align == '<') sb.append('-');
        if (width > 0) sb.append(width);
        if (precision >= 0) sb.append('.').append(precision);
        if (type == 'f' || type == 'd') sb.append('f');
        else sb.append('s');
        return sb.toString();
    }

    // Cache reflection for property/method access to improve performance
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, java.lang.reflect.AccessibleObject>> reflectionCache = new ConcurrentHashMap<>();

    private static Object evalJavaExpression(String expr, Map<String, Object> values) throws FormatException {
        if (!expr.matches("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*(\\(\\))?)*")) {
            throw new FormatException("不支持的表达式: " + expr + "。仅支持简单的属性/方法访问，如 user.name 或 user.getAge()");
        }
        String[] parts = expr.split("\\.");
        Object current = values.get(parts[0]);
        if (current == null) {
            throw new FormatException("表达式 '" + expr + "' 中 '" + parts[0] + "' 为 null 或不存在");
        }
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            Class<?> clazz = current.getClass();
            ConcurrentMap<String, java.lang.reflect.AccessibleObject> classCache =
                reflectionCache.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
            try {
                if (part.endsWith("()")) {
                    String methodName = part.substring(0, part.length() - 2);
                    java.lang.reflect.Method method = (java.lang.reflect.Method) classCache.computeIfAbsent(
                        methodName + "()", k -> {
                            try {
                                java.lang.reflect.Method m = clazz.getMethod(methodName);
                                m.setAccessible(true);
                                return m;
                            } catch (Exception e) {
                                return null;
                            }
                        });
                    if (method == null) throw new NoSuchMethodException(methodName);
                    current = method.invoke(current);
                } else {
                    java.lang.reflect.Field field = (java.lang.reflect.Field) classCache.computeIfAbsent(
                        part, k -> {
                            try {
                                java.lang.reflect.Field f = clazz.getField(part);
                                f.setAccessible(true);
                                return f;
                            } catch (Exception e) {
                                return null;
                            }
                        });
                    if (field != null) {
                        current = field.get(current);
                    } else {
                        String getter = "get" + Character.toUpperCase(part.charAt(0)) + part.substring(1);
                        java.lang.reflect.Method getterMethod = (java.lang.reflect.Method) classCache.computeIfAbsent(
                            getter + "()", k -> {
                                try {
                                    java.lang.reflect.Method m = clazz.getMethod(getter);
                                    m.setAccessible(true);
                                    return m;
                                } catch (Exception e) {
                                    return null;
                                }
                            });
                        if (getterMethod == null) throw new NoSuchMethodException(getter);
                        current = getterMethod.invoke(current);
                    }
                }
            } catch (Exception e) {
                throw new FormatException("表达式 '" + expr + "' 访问 '" + part + "' 失败: " + e.getMessage(), e);
            }
            if (current == null) {
                break;
            }
        }
        return current;
    }
}