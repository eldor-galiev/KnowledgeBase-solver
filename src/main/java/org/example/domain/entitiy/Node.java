package org.example.domain.entitiy;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.domain.types.NodeType;

import java.util.List;

@Entity
@Table(name = "nodes")
@Getter
public class Node {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType nodeType;

    @OneToOne(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private NodeAttribute nodeAttribute;

    @OneToMany(mappedBy = "sourceNode", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NodeConnection> nodeConnections;

}