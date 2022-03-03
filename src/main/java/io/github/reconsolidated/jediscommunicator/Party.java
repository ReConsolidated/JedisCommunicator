package io.github.reconsolidated.jediscommunicator;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;


public class Party {
    @Getter
    private final String owner;
    @Getter
    private final Set<String> members;

    public Party(String owner) {
        this.owner = owner;
        this.members = new HashSet<>();
    }

    public Party(String owner, Set<String> members) {
        this.owner = owner;
        this.members = members;
    }

    public Set<String> getAllMembers() {
        Set<String> result = new HashSet<>(members);
        result.add(owner);
        return result;
    }
}
