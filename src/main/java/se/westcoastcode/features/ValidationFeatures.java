package se.westcoastcode.features;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

public final class ValidationFeatures {
    private static ValidatorFactory FACTORY = Validation
            .byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
    public static Validator VALIDATOR = FACTORY.getValidator();

    /**
     * Validate the supplied object
     *
     * @param value The object
     * @param <T>   The type
     */
    public static <T> void validate(T value) {
        var violations = VALIDATOR.validate(value);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
