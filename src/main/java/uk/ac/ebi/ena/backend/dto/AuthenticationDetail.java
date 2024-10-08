package uk.ac.ebi.ena.backend.dto;

import lombok.Data;

@Data
public class AuthenticationDetail {
    String userName;
    String password;
    boolean isAuthenticated;
}
