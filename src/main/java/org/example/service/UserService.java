package org.example.service;

import lombok.AllArgsConstructor;
import org.example.domain.DTO.UserDTO;
import org.example.domain.entitiy.User;
import org.example.domain.types.AccessLevel;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@AllArgsConstructor
@Service
public class UserService {
    UserRepository userRepository;

    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        assert user != null;
        return convertToDTO(user);
    }

    public UserDTO convertToDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        HashMap<Long, AccessLevel> availableKb = new HashMap<>();
        user.getAvailableKb()
                .forEach(UserKbAccess -> availableKb.put(UserKbAccess.getId().getUserId(), UserKbAccess.getAccessLevel()));
        userDTO.setAvailableKb(availableKb);
        return userDTO;
    }
}
