package edu.asu.sbs.repositories;

import edu.asu.sbs.models.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Integer> {

    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String username);

    Optional<User> findOneWithUserTypeByUserName(String lowercaselogin);

    Optional<User> findOneByUserName(String userName);

    void flush();

    Optional<User> findOneByEmailIgnoreCase(String email);

    Optional<User> findOneBySsn(String ssn);

    Optional<User> findOneByPhoneNumber(String phoneNumber);

    Optional<User> findOneByUserNameOrEmailIgnoreCaseOrSsnOrPhoneNumber(String userName, String email, String ssn, String phoneNumber);

    Optional<User> findOneByActivationKey(String activationKey);

    Optional<User> findByUsernameOrEmail(String username);

    Optional<User> findById(Long id);
}
