package org.example.domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.domain.types.ConnectionType;
import org.example.domain.types.NodeType;

import java.util.HashMap;
import java.util.Set;

@Getter
@Setter
public class NodeDTO {
    private Long id;
    private String name;
    private NodeType nodeType;
    private AttributeDto attribute;
    private HashMap<NodeDTO, ConnectionType> outgoingConnections;
    private HashMap<NodeDTO, ConnectionType> incomingConnections;
    private Set<NodeDTO> arguments;
    private boolean activated;

    @Setter
    @Getter
    @AllArgsConstructor
    public static class AttributeDto{
        private String name;
        private String attributeValueArea;
        private String activationCondition;
    }
}
