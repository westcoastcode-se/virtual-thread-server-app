package se.westcoastcode.features;

import com.dslplatform.json.CompiledJson;
import io.fusionauth.http.server.HTTPRequest;
import io.fusionauth.http.server.HTTPResponse;
import jakarta.validation.ConstraintViolation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static se.westcoastcode.features.JSONFeature.JSON;
import static se.westcoastcode.features.UUIDFeatures.parseUUID;

/**
 * A collection of useful http-related functions
 */
public final class HTTPFeatures {

    @Data
    @AllArgsConstructor
    @CompiledJson
    public static class ErrorMessage {
        private String message;
        private String method;
        private String path;
        private int httpCode;
    }

    @Data
    @CompiledJson
    @AllArgsConstructor
    public static class ConstraintViolationErrorMessage {
        @Data
        @AllArgsConstructor
        @CompiledJson
        public static class Violation {
            private String path;
            private String violation;
        }

        private String message;
        private String method;
        private String path;
        private int httpCode;
        private List<Violation> violations;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class HttpError extends RuntimeException {
        private String method;
        private String path;
        private int httpCode;

        public HttpError(String message, String method, String path, int httpCode) {
            super(message);
            this.method = method;
            this.path = path;
            this.httpCode = httpCode;
        }

        public Object createErrorMessage() {
            return new ErrorMessage(getMessage(), method, path, httpCode);
        }
    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ConstraintViolationHttpError extends HttpError {
        private List<ConstraintViolationErrorMessage.Violation> violations;

        public ConstraintViolationHttpError(String message, String method, String path, int httpCode, Set<ConstraintViolation<?>> violations) {
            super(message, method, path, httpCode);
            this.violations = new ArrayList<>(violations.size());
            for (ConstraintViolation<?> violation : violations) {
                this.violations.add(new ConstraintViolationErrorMessage.Violation(violation.getPropertyPath().toString(), violation.getMessage()));
            }
        }

        public Object createErrorMessage() {
            return new ConstraintViolationErrorMessage(getMessage(), getMethod(), getPath(), getHttpCode(), violations);
        }
    }

    public static HttpError unauthorized(HTTPRequest req) {
        return new HttpError("Unauthorized", req.getMethod().name(), req.getPath(), 401);
    }

    public static HttpError forbidden(HTTPRequest req) {
        return new HttpError("Forbidden", req.getMethod().name(), req.getPath(), 403);
    }

    public static HttpError badRequest(HTTPRequest req) {
        return new HttpError("Bad Request", req.getMethod().name(), req.getPath(), 400);
    }

    public static ConstraintViolationHttpError badRequest(HTTPRequest req, Set<ConstraintViolation<?>> violations) {
        return new ConstraintViolationHttpError("Bad Request", req.getMethod().name(), req.getPath(), 400, violations);
    }

    public static HttpError notFound(HTTPRequest req) {
        return new HttpError("Not Found", req.getMethod().name(), req.getPath(), 404);
    }

    public static HttpError internalServerError(HTTPRequest req) {
        return new HttpError("Internal Server Error", req.getMethod().name(), req.getPath(), 500);
    }

    /**
     * Return the supplied object as json to in the response
     *
     * @param obj        The response object
     * @param res        The response
     * @param statusCode The response status code
     * @param <T>        The response object type
     */
    @SneakyThrows
    public static <T> void status(T obj, HTTPResponse res, int statusCode) {
        res.setStatus(statusCode);
        res.setContentType("application/json");
        JSON.serialize(obj, res.getOutputStream());
    }

    /**
     * Return the supplied object as json to in the response as a 200 OK
     *
     * @param obj The response object
     * @param res The response
     * @param <T> The response object type
     */
    @SneakyThrows
    public static <T> void ok(T obj, HTTPResponse res) {
        status(obj, res, 200);
    }

    /**
     * Return the supplied object as json to in the response as a 201 Created
     *
     * @param obj The response object
     * @param res The response
     * @param <T> The response object type
     */
    @SneakyThrows
    public static <T> void created(T obj, HTTPResponse res) {
        status(obj, res, 201);
    }

    /**
     * Extract a value from a request path
     *
     * @param req    The request
     * @param search Search information
     * @param type   The type we want to return
     * @param <T>    The type
     * @return The value if found
     */
    public static <T> T valueFromPath(HTTPRequest req, String search, Class<T> type) {
        var startIdx = search.indexOf('%');
        if (startIdx == -1) throw new IllegalArgumentException("Expected a '%' character to search for");
        var path = req.getPath().substring(startIdx);
        if (path.isEmpty()) {
            throw badRequest(req);
        }

        var endIdx = path.indexOf('/');
        if (endIdx != -1) {
            path = path.substring(0, endIdx);
        }

        if (type.equals(String.class)) {
            return (T) path;
        } else if (type.equals(UUID.class)) {
            try {
                return (T) parseUUID(path);
            } catch (IllegalArgumentException _) {
                throw badRequest(req);
            }
        } else if (type.equals(Long.class)) {
            try {
                Long result = Long.parseLong(path);
                return (T) result;
            } catch (NumberFormatException _) {
                throw badRequest(req);
            }
        }
        throw new RuntimeException("Unknown type: " + type);
    }
}
