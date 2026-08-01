package dev.monorepo.apps.progen.model;

import dev.monorepo.apps.progen.constant.ROLE;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Table(name = "users")
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
    private boolean isEnable;
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workspace> workspaces = new ArrayList<>();

    public void addWorkspace(Workspace workspace) {
        this.workspaces.add(workspace);
        workspace.setOwner(this);
    }

    public void removeWorkspace(Workspace workspace) {
        this.workspaces.remove(workspace);
        workspace.setOwner(null);
    }

}
