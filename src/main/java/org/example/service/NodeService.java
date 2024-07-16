package org.example.service;

import lombok.AllArgsConstructor;
import org.example.domain.DTO.NodeDTO;
import org.example.domain.DTO.SectionDTO;
import org.example.domain.entitiy.Node;
import org.example.repository.NodeRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@AllArgsConstructor
public class NodeService {
    
    private NodeRepository nodeRepository;

    private NodeDTO convertToDto(Node node, Map<Long, NodeDTO> cache) {
        if (cache.containsKey(node.getId())) {
            return cache.get(node.getId());
        }

        NodeDTO nodeDto = new NodeDTO();
        nodeDto.setName(node.getName());
        nodeDto.setNodeType(node.getNodeType());
        nodeDto.setRelatedNodes(new HashMap<>());
        cache.put(node.getId(), nodeDto);

        node.getNodeConnections().stream().map(connection -> Map.entry(connection.getTargetNode(), connection.getConnectionType()))
        .forEach(entry -> {
            NodeDTO targetNodeDto = convertToDto(entry.getKey(), cache);
            nodeDto.getRelatedNodes().put(targetNodeDto, entry.getValue());
        });

        return nodeDto;
    }

    public List<NodeDTO> getNodesFromSection(SectionDTO sectionDTO) {
        List<Node> nodes = nodeRepository.findAll();
        nodes = nodes.stream()
                .filter(node -> Objects.equals(node.getSection().getId(), sectionDTO.getId()))
                .toList();
        Map<Long, NodeDTO> cache = new HashMap<>();
        return nodes.stream().map(node -> convertToDto(node, cache)).toList();
    }

    public List<NodeDTO> getAllNodesFromSectionList(List<SectionDTO> sections) {
        return sections.stream()
                .flatMap(section -> getNodesFromSection(section).stream())
                .toList();
    }

}
