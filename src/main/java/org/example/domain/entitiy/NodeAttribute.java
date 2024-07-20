package org.example.domain.entitiy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "node_attributes")
@Getter
public class NodeAttribute {
    @EmbeddedId
    private NodeAttributeId id;

    @OneToOne
    @MapsId("nodeId")
    @JoinColumn(name = "node_id")
    private Node node;

    @ManyToOne
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    private Attribute attribute;

    @Column(name = "activation_condition")
    private String activationCondition;

    public NodeAttribute() {
    }

    @Embeddable
    @NoArgsConstructor
    @Getter
    public static class NodeAttributeId implements Serializable {
        @Column(name = "node_id")
        private Long nodeId;

        @Column(name = "attribute_id")
        private Long attributeId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeAttributeId that = (NodeAttributeId) o;
            return Objects.equals(nodeId, that.nodeId) &&
                    Objects.equals(attributeId, that.attributeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, attributeId);
        }
    }
}
