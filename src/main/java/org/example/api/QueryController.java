package org.example.api;

import lombok.AllArgsConstructor;
import org.example.domain.DTO.*;
import org.example.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;  

import java.util.List;

@RestController
@RequestMapping("/knowledge-base")
@AllArgsConstructor
public class QueryController {
    private KnowledgeBaseService knowledgeBaseService;
    private SectionService sectionService;
    private NodeService nodeService;
    private UserService userService;
    private QuerySolver querySolver;

    @GetMapping("/solve")
    public ResponseEntity<AnswerDTO> solve(@RequestBody RequestDTO request) {
        if (!checkAccess(request.getUserId(), request.getKbId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        querySolver.solve(request, getAllNodesOfKnowledgeBase(request.getKbId()));

        AnswerDTO answer = new AnswerDTO();
        // упаковка ответа алгоритма

        return ResponseEntity.ok(answer);
    }

    private List<NodeDTO> getAllNodesOfKnowledgeBase(Long kbId) {
        KnowledgeBaseDTO knowledgeBaseDTO = knowledgeBaseService.getKnowledgeBaseById(kbId);
        List<SectionDTO> sectionDTOS = sectionService.getAllSectionFromKnowledgeBase(knowledgeBaseDTO);
        return nodeService.getAllNodesFromSectionList(sectionDTOS);
    }

    private boolean checkAccess(Long userId, Long kbId) {
        UserDTO userDTO = userService.getUserById(userId);
        return userDTO.getAvailableKb().containsKey(kbId);
    }
}
