-- Create a new table
CREATE TABLE IF NOT EXISTS employee
(
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100)
);

-- Create a function for notifications
CREATE OR REPLACE FUNCTION notify_employee_changed() RETURNS TRIGGER AS
$$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        PERFORM pg_notify('employee_changed', format('employee:D:%s', OLD.id));
        RETURN OLD;
    ELSEIF (TG_OP = 'UPDATE') THEN
        PERFORM pg_notify('employee_changed', format('employee:U:%s', NEW.id));
        RETURN NEW;
    ELSEIF (TG_OP = 'INSERT') THEN
        PERFORM pg_notify('employee_changed', format('employee:I:%s', NEW.id));
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Add trigger on employee table that notifies all subscribers
DO
$$
    BEGIN
        CREATE TRIGGER employee_changed
            AFTER INSERT OR UPDATE OR DELETE
            ON employee
            FOR EACH ROW
        EXECUTE PROCEDURE notify_employee_changed();
    EXCEPTION
        WHEN duplicate_object THEN
            NULL;
    END;
$$;
