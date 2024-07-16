package org.example.domain.entitiy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.types.ConnectionType;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "node_connections")
@Getter
public class NodeConnection {
    @EmbeddedId
    private NodeConnectionId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, insertable = false, updatable = false)
    private ConnectionType connectionType;

    @ManyToOne
    @MapsId("sourceNodeId")
    @JoinColumn(name = "source_node_id")
    private Node sourceNode;

    @ManyToOne
    @MapsId("targetNodeId")
    @JoinColumn(name = "target_node_id")
    private Node targetNode;

    @Embeddable
    @Getter
    @NoArgsConstructor
    public static class NodeConnectionId implements Serializable {
        @Column(name = "source_node_id")
        private Long sourceNodeId;

        @Column(name = "target_node_id")
        private Long targetNodeId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeConnectionId that = (NodeConnectionId) o;
            return Objects.equals(sourceNodeId, that.sourceNodeId) &&
                    Objects.equals(targetNodeId, that.targetNodeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetNodeId, targetNodeId);
        }

    }
}
