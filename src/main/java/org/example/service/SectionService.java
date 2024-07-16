package org.example.service;

import lombok.AllArgsConstructor;
import org.example.domain.DTO.KnowledgeBaseDTO;
import org.example.domain.DTO.SectionDTO;
import org.example.domain.entitiy.Section;
import org.example.repository.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class SectionService {
    private SectionRepository sectionRepository;

    public List<SectionDTO> getAllSectionFromKnowledgeBase(KnowledgeBaseDTO knowledgeBaseDTO) {
        List<Section> nodes = sectionRepository.findAll();
        return nodes.stream()
                .filter(node -> Objects.equals(node.getKnowledgeBase().getId(), knowledgeBaseDTO.getId()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private SectionDTO convertToDto(Section section) {
        SectionDTO sectionDTO = new SectionDTO();
        sectionDTO.setId(section.getId());
        sectionDTO.setName(section.getName());
        return sectionDTO;
    }
}
