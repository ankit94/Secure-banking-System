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
import edu.asu.sbs.services.AccountService;
import edu.asu.sbs.services.RequestService;
import edu.asu.sbs.services.UserService;
import edu.asu.sbs.services.dto.ProfileRequestDTO;
import edu.asu.sbs.services.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@PreAuthorize("hasAuthority('" + UserType.ADMIN_ROLE + "')")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserService userService;
    private final RequestService requestService;
    private final HandlebarsTemplateLoader handlebarsTemplateLoader;
    private final AccountService accountService;
    private final Environment env;

    ObjectMapper mapper = new ObjectMapper();

    public AdminController(UserService userService, RequestService requestService, HandlebarsTemplateLoader handlebarsTemplateLoader, AccountService accountService, Environment env) {
        this.userService = userService;
        this.requestService = requestService;
        this.handlebarsTemplateLoader = handlebarsTemplateLoader;
        this.accountService = accountService;
        this.env = env;
    }

    // Details of the admin.
    @GetMapping("/details")
    @ResponseBody
    public String currentUserDetails() throws UnauthorizedAccessExcpetion, IOException {

        User user = userService.getCurrentUser();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        mapper.setDateFormat(df);
        if (user == null) {
            log.info("GET request: Unauthorized request for admin user detail");
            throw new UnauthorizedAccessExcpetion("401", "Unauthorized Access ! Don't sneak behave good -__- ¯\\_(ツ)_/¯");
        }

        JsonNode result = mapper.valueToTree(user);
        Template template = handlebarsTemplateLoader.getTemplate("profileAdmin");
        log.info("GET request: Admin user detail");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/employee/add")
    public String getLoginTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("adminAddNewEmployee");
        return template.apply("adminAddNewEmployee");
    }

    @PostMapping("/employee/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void signupSubmit(UserDTO newUserRequest, String password, String userType, HttpServletResponse response) throws IOException {
        userService.registerUser(newUserRequest, password, userType);
        log.info("POST request: Admin new user request");
        response.sendRedirect("/allEmployees");
    }

    @GetMapping("/allEmployees")
    public String getUsers() throws IOException {
        ArrayList<User> allEmployees = (ArrayList<User>) userService.getAllEmployees();
        HashMap<String, ArrayList<User>> resultMap = new HashMap<>();
        resultMap.put("result", allEmployees);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("adminEmployeeAccess");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/delete/{id}")
    public void deleteUser(@PathVariable Long id, HttpServletResponse response) throws Exceptions, IOException {
        User current = userService.getUserByIdAndActive(id);
        if (current == null) {
            throw new Exceptions("404", "No scene, User not found ¯\\_(ツ)_/¯");
        }

        userService.deleteUser(id);
        log.info("POST request: Employee deletion request");
        response.sendRedirect("../allEmployees");
    }

    @GetMapping("/viewEmployee/{id}")
    public String getUserDetail(@PathVariable Long id) throws Exceptions, IOException {
        User user = userService.getUserByIdAndActive(id);

        if (user == null) {
            throw new Exceptions("404", " ");
        }

        JsonNode result = mapper.valueToTree(user);
        Template template = handlebarsTemplateLoader.getTemplate("adminViewEmployee");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/requests")
    public String getAllUserRequest() throws IOException {
        ArrayList<ProfileRequestDTO> allRequests = (ArrayList<ProfileRequestDTO>) requestService.getAllAdminRequests();
        HashMap<String, ArrayList<ProfileRequestDTO>> resultMap = new HashMap<>();
        resultMap.put("result", allRequests);
        System.out.println(resultMap);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("adminHome");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @PostMapping("/requests/approve")
    public void approveEdit(Long id, HttpServletResponse response) throws IllegalStateException, IOException {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            switch (req.getRequestType()) {
                case RequestType.TIER1_TO_TIER2:
                    userService.updateUserType(req.getRequestBy(), UserType.EMPLOYEE_ROLE2);
                    requestService.updateChangeRoleRequest(req, StatusType.APPROVED, user);
                    break;
                case RequestType.TIER2_TO_TIER1:
                    userService.updateUserType(req.getRequestBy(), UserType.EMPLOYEE_ROLE1);
                    requestService.updateChangeRoleRequest(req, StatusType.APPROVED, user);
                    break;
                case RequestType.UPDATE_EMP_PROFILE:
                    requestService.updateUserProfile(req, userService.getCurrentUser(), RequestType.UPDATE_EMP_PROFILE, StatusType.APPROVED);
                    break;
                default:
                    throw new IllegalStateException("Unexpected RequestType: " + req.getRequestType());
            }
        });
        response.sendRedirect("/api/v1/admin/requests");
    }

    @PostMapping("/requests/decline")
    public void declineEdit(Long id, HttpServletResponse response) throws IllegalStateException, IOException {

        Optional<Request> request = requestService.getRequest(id);
        User user = userService.getCurrentUser();
        request.ifPresent(req -> {
            switch (req.getRequestType()) {
                case RequestType.TIER1_TO_TIER2:
                case RequestType.TIER2_TO_TIER1:
                    requestService.updateChangeRoleRequest(req, StatusType.DECLINED, user);
                    break;
                case RequestType.UPDATE_EMP_PROFILE:
                    requestService.updateUserProfile(req, userService.getCurrentUser(), RequestType.UPDATE_EMP_PROFILE, StatusType.DECLINED);
                    break;
                default:
                    throw new IllegalStateException("Unexpected RequestType: " + req.getRequestType());
            }
        });
        response.sendRedirect("/api/v1/admin/requests");
    }

    @GetMapping("/logDownload")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void doDownload(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

        final int BUFFER_SIZE = 4096;
        String filePath = env.getProperty("logging.file.name");
        ServletContext context = request.getServletContext();

        // construct the complete absolute path of the file
        File downloadFile = new File(filePath);
        try (FileInputStream inputStream = new FileInputStream(downloadFile);
             OutputStream outStream = response.getOutputStream()) {
            // get MIME type of the file
            String mimeType = context.getMimeType("text/plain");
            if (mimeType == null) {
                // set to binary type if MIME mapping not found
                mimeType = "application/octet-stream";
            }
            log.debug("MIME type: " + mimeType);

            // set content attributes for the response
            response.setContentType(mimeType);
            response.setContentLength((int) downloadFile.length());

            // set headers for the response
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"",
                    downloadFile.getName());
            response.setHeader(headerKey, headerValue);


            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /* Admin can edit his own details */
    @PostMapping("/details/edit")
    public void updateAdminProfile(UserDTO userDTO, HttpServletResponse response) throws IOException {
        User user = userService.getCurrentUser();
        if (user.getUserType().equals(UserType.ADMIN_ROLE)) {
            userDTO.setId(user.getId());
            System.out.println(userDTO);
            userService.editUser(userDTO);
        }
        response.sendRedirect("/api/v1/admin/requests");
    }

    @PostMapping("/employee/closeAccount")
    public void closeEmployeeAccount(Long id, HttpServletResponse response) throws IllegalStateException, IOException {
        if (id != null) {
            accountService.closeUserAccount(id);
        } else {
            throw new IllegalStateException("Incorrect Id: " + id);
        }
        response.sendRedirect("/api/v1/admin/requests");
    }

}