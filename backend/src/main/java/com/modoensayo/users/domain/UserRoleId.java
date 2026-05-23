package com.modoensayo.users.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserRoleId implements Serializable {
    private UUID userId;
    private Integer roleId;
}
