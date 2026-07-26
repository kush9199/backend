package dev.monorepo.apps.progen.model;

import dev.monorepo.apps.progen.constant.ROLE;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String username;
    private String password;
    @Column(unique = true)
    private String email;
    @Enumerated(value = EnumType.STRING)
    private ROLE role;
    @ManyToMany(cascade = CascadeType.ALL)
    private Set<Workspace> workspaces = new HashSet<>();

    public void addWorkspace(Workspace workspace) {
        if(workspaces == null){
            workspaces = new HashSet<>();
        }
        this.workspaces.add(workspace);
    }

    public void removeWorkspace(Workspace workspace) {
        this.workspaces.remove(workspace);
    }

}
