package org.example.domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnswerDto {
    private Long id;
    private String name;
    private List<AnswerDto> arguments;

    public static AnswerDto fromNodeDTO(NodeDTO nodeDTO) {
        if (nodeDTO == null) {
            return null;
        }

        List<AnswerDto> arguments = nodeDTO.getArguments() == null ?
                new ArrayList<>() :
                nodeDTO.getArguments().stream()
                        .map(AnswerDto::fromNodeDTO)
                        .toList();

        return new AnswerDto(nodeDTO.getId(), nodeDTO.getName(), arguments);
    }
}
