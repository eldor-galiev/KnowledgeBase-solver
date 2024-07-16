package org.example.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RequestDTO {
    private Long userId;
    private Long kbId;
    private List<Parameter> parameters;

    @Getter
    public class Parameter {
        private String name;
        private String value;
    }
}
