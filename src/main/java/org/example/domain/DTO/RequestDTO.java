package org.example.domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class RequestDTO {
    private final Long userId;
    private final Long kbId;
    private final List<Parameter> parameters;
    private final boolean HypothesesSetDifferentiationNeeded;
    private final boolean HypothesesSetMinimizationNeeded;

    @Getter
    @AllArgsConstructor
    public static class Parameter {
        private Long featureId;
        private String attributeValue;
    }
}
