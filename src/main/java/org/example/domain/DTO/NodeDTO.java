package org.example.domain.DTO;

import lombok.Getter;
import lombok.Setter;
import org.example.domain.types.ConnectionType;
import org.example.domain.types.NodeType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class NodeDTO {
    private Long id;
    private String name;
    private NodeType nodeType;
    private AttributeDto attribute;
    private HashMap<NodeDTO, ConnectionType> outgoingConnections;
    private List<NodeDTO> incomingConnections;
    private Set<NodeDTO> arguments;

    @Setter
    @Getter
    public class AttributeDto{
        private String name;
        private String attributeValueArea;
        private String activationCondition;
    }

    public boolean checkFeatureActivationCondition(String input) {
        if (attribute.getActivationCondition().matches("\\d+:\\d+")) {
            return checkRangeCondition(attribute.getActivationCondition(), input);
        } else if (attribute.getActivationCondition().matches("\\[\\d+(,\\d+)*\\]")) {
            return checkDiscreteCondition(attribute.getActivationCondition(), input);
        } else {
            return checkStringCondition(attribute.getActivationCondition(), input);
        }
    }

    private boolean checkRangeCondition(String condition, String input) {
        String[] parts = condition.split(":");
        int start = Integer.parseInt(parts[0]);
        int end = Integer.parseInt(parts[1]);

        try {
            int value = Integer.parseInt(input);
            return value >= start && value <= end;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean checkDiscreteCondition(String condition, String input) {
        String trimmedCondition = condition.substring(1, condition.length() - 1);
        List<String> discreteValues = Arrays.asList(trimmedCondition.split(","));
        return discreteValues.contains(input);
    }

    private boolean checkStringCondition(String condition, String input) {
        return condition.equals(input);
    }
}
