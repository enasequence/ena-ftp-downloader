package uk.ac.ebi.ena.cv19fd.app.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public final class HttpUtils {

    /*
     * Utility function for combining key-value parameter pairs into a URL-encoded string.
     */
    public static final Function<Map.Entry<String, String>, String> parameterAssembler = entry ->
            entry.getKey() + "=" + entry.getValue();

    /**
     * Convenience method for creating parameter maps from a list of strings.
     * <p>
     * Converts {@code "query", "*:*", "format", "json"} to {@code ["query": "*:*", "format": "json"]}
     * </p>
     *
     * @param args a list of key-value pairs denoting parameter names and corresponding values;
     * @return the parameters as a {@code Map}
     */
    public static Map<String, String> asParamsMap(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Expected an even list of key-value pairs (e.g. format, json)");
        }
        final Map<String, String> result = new LinkedHashMap<>(args.length / 2 + 1, 1.0f);
        for (int i = 0; i < args.length - 1; i += 2) {
            if (Objects.nonNull(args[i + 1])) {
                String key = args[i];
                String val = args[i + 1];
                result.put(key, val);
            }
        }
        return result;
    }

    /**
     * Concate params with given prefix and suffix
     *
     * @param params
     * @param prefix
     * @param suffix
     * @return
     */
    private static String concatenateParams(Map<String, String> params, String prefix,
                                            String suffix) {
        if (params.isEmpty()) {
            return "";
        }

        return params.entrySet().stream()
                .map(parameterAssembler)
                .collect(Collectors.joining("&", prefix, suffix));
    }

    /**
     * Convert map key value pair into string of url with prefix '?'
     *
     * @param endPoint
     * @param paramsMap
     * @return
     */
    public static String buildUrl(String endPoint, Map<String, String> paramsMap) {
        String contactedParams = concatenateParams(paramsMap, "?", "");
        return endPoint + contactedParams;
    }

}
