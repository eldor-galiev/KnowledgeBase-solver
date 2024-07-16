package org.example.domain.DTO;

import lombok.Getter;
import lombok.Setter;
import org.example.domain.types.ConnectionType;
import org.example.domain.types.NodeType;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class NodeDTO {
    private String name;
    private NodeType nodeType;
    private List<AttributeDto> attributes;
    private HashMap<NodeDTO, ConnectionType> relatedNodes;

    @Setter
    private class AttributeDto{
        private String attributeName;
        private String attributeValueArea;
        private String activationCondition;
    }
}
