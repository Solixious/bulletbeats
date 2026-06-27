package in.bulletbeats.domain.shared.enums;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {

    STAFF("Staff"),
    MANAGER("Manager"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
