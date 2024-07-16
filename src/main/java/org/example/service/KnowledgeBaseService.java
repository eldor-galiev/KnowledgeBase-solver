package org.example.service;

import lombok.AllArgsConstructor;
import org.example.domain.DTO.KnowledgeBaseDTO;
import org.example.domain.entitiy.KnowledgeBase;
import org.example.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class KnowledgeBaseService {
    private KnowledgeBaseRepository knowledgeBaseRepository;

    public KnowledgeBaseDTO getKnowledgeBaseById(Long id) {
        KnowledgeBase knowledgeBaseEntity = knowledgeBaseRepository.findById(id).orElse(null);
        KnowledgeBaseDTO knowledgeBaseDTO = new KnowledgeBaseDTO();
        assert knowledgeBaseEntity != null;
        knowledgeBaseDTO.setId(knowledgeBaseEntity.getId());
        knowledgeBaseDTO.setName(knowledgeBaseEntity.getName());
        return knowledgeBaseDTO;
    }

}
