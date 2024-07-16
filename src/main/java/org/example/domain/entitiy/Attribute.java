package org.example.domain.entitiy;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.domain.types.AttributeType;

import java.util.List;

@Entity
@Table(name = "attributes")
@Getter
public class Attribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttributeType type;

    @Column(name = "value_area", nullable = false)
    private String valueArea;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NodeAttribute> nodeAttributes;
}
