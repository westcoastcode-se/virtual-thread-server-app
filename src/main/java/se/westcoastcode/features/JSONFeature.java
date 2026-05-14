package se.westcoastcode.features;

import com.dslplatform.json.DslJson;
import io.fusionauth.http.server.HTTPRequest;
import lombok.SneakyThrows;

import java.io.InputStream;

/**
 * A collection of useful JSON-related functions
 */
public final class JSONFeature {
    public static final DslJson<Object> JSON = new DslJson<>();

    /**
     * Deserialize the input stream into a java object
     *
     * @param type   The java class
     * @param stream The input stream
     * @param <T>    The type
     * @return The deserialized object
     */
    @SneakyThrows
    public static <T> T fromJson(Class<T> type, InputStream stream) {
        return JSON.deserialize(type, stream);
    }

    /**
     * Deserialize the http request's input stream into a java object
     *
     * @param type The java class
     * @param req  The incoming http request
     * @param <T>  The type
     * @return The deserialized object
     */
    @SneakyThrows
    public static <T> T fromJson(Class<T> type, HTTPRequest req) {
        return JSON.deserialize(type, req.getInputStream());
    }
}
