package org.example.domain.DTO;

import lombok.Getter;
import lombok.Setter;
import org.example.domain.types.AccessLevel;

import java.util.HashMap;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String username;
    private HashMap<Long, AccessLevel> availableKb;
}
