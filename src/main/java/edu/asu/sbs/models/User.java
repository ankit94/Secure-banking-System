package edu.asu.sbs.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.asu.sbs.config.Constants;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.sql.Date;
import java.time.Instant;

@Entity
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Pattern(regexp = Constants.USERNAME_REGEX)
    @Column(unique = true, nullable = false, length = 50)
    @Size(min = 1, max = 50)
    private String userName;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(nullable = false, length = 50)
    private String firstName;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(nullable = false, length = 50)
    private String lastName;

    @NotNull
    @Column(nullable = false)
    @DateTimeFormat(pattern = "mm-dd-yyyy")
    private Date dateOfBirth;

    @NotNull
    @Column(nullable = false)
    private boolean isActive = false;

    @NotNull
    @Pattern(regexp = Constants.SSN_REGEX)
    @Column(unique = true, nullable = false)
    private String ssn;

    @NotNull
    @Column(nullable = false, length = 50)
    private String userType;

    @NotNull
    @Pattern(regexp = Constants.PHONE_NUMBER_REGEX)
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @NotNull
    @Email
    @Size(min = 5, max = 254)
    @Column(unique = true, nullable = false, length = 254)
    private String email;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Size(max = 20)
    @Column(length = 20)
    @JsonIgnore
    private String resetKey;

    private Instant resetDate = null;

    @Size(max = 20)
    @Column(length = 20)
    @JsonIgnore
    private String activationKey;

}
