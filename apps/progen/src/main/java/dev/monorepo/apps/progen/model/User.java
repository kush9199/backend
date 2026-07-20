package dev.monorepo.apps.progen.model;

import dev.monorepo.apps.progen.constant.ROLE;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private String username;
    private String password;
    @Column(unique = true)
    private String email;
    private ROLE role;
}
