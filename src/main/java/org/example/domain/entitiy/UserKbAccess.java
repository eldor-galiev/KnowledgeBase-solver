package org.example.domain.entitiy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.types.AccessLevel;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_kb_access")
@Getter
@AllArgsConstructor
public class UserKbAccess implements Serializable{
    @EmbeddedId
    private UserKbAccessId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessLevel accessLevel;

    @OneToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    public UserKbAccess() {
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    public static class UserKbAccessId implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "kb_id")
        private Long kbId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserKbAccessId that = (UserKbAccessId) o;
            return Objects.equals(userId, that.userId) &&
                    Objects.equals(kbId, that.kbId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, kbId);
        }
    }
}
