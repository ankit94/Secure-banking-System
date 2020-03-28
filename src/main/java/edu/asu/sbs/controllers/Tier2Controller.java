package edu.asu.sbs.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.StatusType;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.Exceptions;
import edu.asu.sbs.errors.UnauthorizedAccessExcpetion;
import edu.asu.sbs.loader.HandlebarsTemplateLoader;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.RequestService;
import edu.asu.sbs.services.TransactionService;
import edu.asu.sbs.services.UserService;
import edu.asu.sbs.services.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@PreAuthorize("hasAnyAuthority('" + UserType.EMPLOYEE_ROLE2 + "')")
@RestController
@RequestMapping("/api/v1/tier2")
public class Tier2Controller {

    private final UserService userService;
    private final RequestService requestService;
    ObjectMapper mapper = new ObjectMapper();

    //@Autowired
    private final HandlebarsTemplateLoader handlebarsTemplateLoader;

    private final TransactionService transactionService;

    public Tier2Controller(UserService userService, TransactionService transactionService, RequestService requestService, HandlebarsTemplateLoader handlebarsTemplateLoader) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.requestService = requestService;
        this.handlebarsTemplateLoader = handlebarsTemplateLoader;
    }

    @GetMapping("/user/add")
    public String getLoginTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("tier2AddNewUser");
        return template.apply("");
    }

    @PostMapping("/user/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void signupSubmit(UserDTO newUserRequest, String password, String userType, HttpServletResponse response) throws IOException {
        userService.registerUser(newUserRequest, password, userType);
        log.info("POST request: tier2 new user request");
        response.sendRedirect("/allUsers");
    }

    @GetMapping("/details")
    @ResponseBody
    public String currentUserDetails() throws UnauthorizedAccessExcpetion, IOException {

            User user = userService.getCurrentUser();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            mapper.setDateFormat(df);
            if (user == null) {
                log.info("GET request: Unauthorized request for tier2 employee user detail");
                throw new UnauthorizedAccessExcpetion("401", "Unauthorized Access !");
            }
            JsonNode result = mapper.valueToTree(user);
            Template template = handlebarsTemplateLoader.getTemplate("profileTier2");
            log.info("GET request: Tier2 Employee user detail");
            return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/allUsers")
    public String getUsers() throws IOException {
        ArrayList<User> allUsers = (ArrayList<User>) userService.getAllUsers();
        HashMap<String, ArrayList<User>> resultMap= new HashMap<>();
        resultMap.put("result", allUsers);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("tier2UserAccess");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/delete/{id}")
    public void deleteUser(@PathVariable Long id, HttpServletResponse response) throws Exceptions, IOException {
        User current = userService.getUserByIdAndActive(id);
        if (current == null) {
            throw new Exceptions("404", " ");
        }
        if (!(current.getUserType().equals(UserType.USER_ROLE))) {
            log.warn("GET request: tier2 employee unauthorised request access");
            throw new Exceptions("401", "Unauthorized request !!");
        }

        userService.deleteUser(id);
        log.info("POST request: Delete User request");
        response.sendRedirect("/api/v1/tier2/allUsers");
    }

    @GetMapping("/viewUser/{id}")
    public String getUserDetail(@PathVariable Long id) throws Exceptions, IOException {
        User user = userService.getUserByIdAndActive(id);

        if (user == null) {
            throw new Exceptions("404", " ");
        }
        if (!(user.getUserType().equals(UserType.USER_ROLE))) {
            log.warn("GET request: Unauthorized request for external user");
            throw new Exceptions("409", " ");
        }

        JsonNode result = mapper.valueToTree(user);
        Template template = handlebarsTemplateLoader.getTemplate("tier2ViewUser");
            return template.apply(handlebarsTemplateLoader.getContext(result));
        }

    @GetMapping("/transactions")
    public String getAllUserRequest() throws IOException {
        ArrayList<Request> allRequests = requestService.getAllTier2Requests();
        HashMap<String, ArrayList<Request>> resultMap = new HashMap<>();
        resultMap.put("result", allRequests);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("tier2Transactions");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/editUser/{id}")
    public String modifyUserDetail(UserDTO userDTO
    ) throws Exceptions, IOException {

        Optional<User> user = userService.editUser(userDTO);

        if (user == null) {
            throw new Exceptions("404", " ");
        }
        JsonNode result = mapper.valueToTree(user.get());
        Template template = handlebarsTemplateLoader.getTemplate("tier2ViewUser");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @PutMapping("/tier2Requests/approve/{id}")
    public void approveEdit(@PathVariable Long id) {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            switch (req.getRequestType()) {
                case RequestType.APPROVE_CRITICAL_TRANSACTION:
                    requestService.modifyRequest(request, user, RequestType.APPROVE_CRITICAL_TRANSACTION, StatusType.APPROVED);
                    break;
            }
        });
    }

    @PutMapping("/tier2Requests/decline/{id}")
    public void declineEdit(@PathVariable Long id) {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            switch (req.getRequestType()) {
                case RequestType.APPROVE_CRITICAL_TRANSACTION:
                    requestService.modifyRequest(request, user, RequestType.APPROVE_CRITICAL_TRANSACTION, StatusType.DECLINED);
                    break;
            }
        });
    }

    /*
    @GetMapping("/viewRequests")
    @ResponseBody
    public List<Request> viewRequests() {
        return requestService.getAllRequests();
    }
     */

    @PutMapping("/approveCriticalTransaction")
    public void approveCriticalTransaction(@RequestParam Long requestId) {
        transactionService.approveCriticalTransaction(requestId);
    }

}