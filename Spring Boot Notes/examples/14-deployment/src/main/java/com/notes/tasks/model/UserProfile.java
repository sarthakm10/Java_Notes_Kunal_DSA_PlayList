package com.notes.tasks.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One-to-one with a SHARED primary key: user_profile.user_id is both the PK and the FK. */
@Entity
@Table(name = "user_profile")
@Getter @Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    private Long id;                          // copied from the user by @MapsId

    @MapsId                                   // "my id is the id of this association"
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")             // owning side: this table holds the FK
    private AppUser user;

    private String bio;
    private String github;
}
