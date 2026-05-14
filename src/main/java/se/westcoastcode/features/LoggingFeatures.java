package se.westcoastcode.features;

import io.fusionauth.http.log.BaseLogger;
import io.fusionauth.http.log.Logger;
import io.fusionauth.http.log.SystemOutLoggerFactory;

/**
 * Useful logging-related functions
 */
public final class LoggingFeatures {

    public static final LevelLoggerFactory LOGGING_FACTORY = new LevelLoggerFactory();

    public static Logger getLogger(Class<?> clz) {
        return LOGGING_FACTORY.getLogger(clz);
    }

    public static final class LevelLoggerFactory extends SystemOutLoggerFactory {
        private final Logger logger = new LevelLogger();

        @Override
        public Logger getLogger(Class<?> klass) {
            return logger;
        }

        public static final class LevelLogger extends BaseLogger {
            @Override
            public void debug(String message) {
                if (isDebugEnabled()) {
                    message = "DEBUG T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.debug(message);
                }
            }

            @Override
            public void debug(String message, Object... values) {
                if (isDebugEnabled()) {
                    message = "DEBUG T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.debug(message, values);
                }
            }

            @Override
            public void debug(String message, Throwable throwable) {
                if (isDebugEnabled()) {
                    message = "DEBUG T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.debug(message, throwable);
                }
            }

            @Override
            public void error(String message, Throwable throwable) {
                if (isErrorEnabled()) {
                    message = "ERROR T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.error(message, throwable);
                }
            }

            @Override
            public void error(String message) {
                if (isErrorEnabled()) {
                    message = "ERROR T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.error(message);
                }
            }

            @Override
            public void info(String message) {
                if (isInfoEnabled()) {
                    message = "INFO T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.info(message);
                }
            }

            @Override
            public void info(String message, Object... values) {
                if (isInfoEnabled()) {
                    message = "INFO T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.info(message, values);
                }
            }

            @Override
            public void trace(String message, Object... values) {
                if (isTraceEnabled()) {
                    message = "TRACE T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.trace(message, values);
                }
            }

            @Override
            public void trace(String message) {
                if (isTraceEnabled()) {
                    message = "TRACE T:\"" + Thread.currentThread().getName() + "\" " + message;
                    super.trace(message);
                }
            }

            @Override
            protected void handleMessage(String message) {
                System.out.println(message);
            }

            @Override
            protected String format(String message, Object... values) {
                // Sanitize all varargs so that non-printable Unicode characters are removed
                var object = new Object[values.length];
                for (int i = 0; i < values.length; i++) {
                    Object value = values[i];
                    if (value != null) {
                        value = value.toString().replace("[\\p{C}]", "");
                    }
                    object[i] = value;
                }
                return super.format(message, object);
            }
        }
    }
}
