package se.westcoastcode.persistence;

import com.dslplatform.json.CompiledJson;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@CompiledJson
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private UUID id;
    @NotEmpty
    private String name;
}
