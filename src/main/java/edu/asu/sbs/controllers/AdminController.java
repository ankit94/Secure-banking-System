package edu.asu.sbs.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import edu.asu.sbs.config.RequestType;
import edu.asu.sbs.config.UserType;
import edu.asu.sbs.errors.Exceptions;
import edu.asu.sbs.errors.UnauthorizedAccessExcpetion;
import edu.asu.sbs.loader.HandlebarsTemplateLoader;
import edu.asu.sbs.models.Request;
import edu.asu.sbs.models.User;
import edu.asu.sbs.services.RequestService;
import edu.asu.sbs.services.UserService;
import edu.asu.sbs.services.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONException;
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
public class AdminController{

    private final UserService userService;
    private final RequestService requestService;
    private final HandlebarsTemplateLoader handlebarsTemplateLoader;

    ObjectMapper mapper = new ObjectMapper();

    public AdminController(UserService userService, RequestService requestService, HandlebarsTemplateLoader handlebarsTemplateLoader) {
        this.userService = userService;
        this.requestService = requestService;
        this.handlebarsTemplateLoader = handlebarsTemplateLoader;
    }

    @GetMapping("/employee/details")
    @ResponseBody
    public String currentUserDetails() throws UnauthorizedAccessExcpetion, JSONException, IOException {

        User user = userService.getCurrentUser();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        mapper.setDateFormat(df);
        if (user == null) {
            log.info("GET request: Unauthorized request for admin user detail");
            throw new UnauthorizedAccessExcpetion("401", "Unauthorized Access !");
        }

        JsonNode result = mapper.valueToTree(user);
        Template template = handlebarsTemplateLoader.getTemplate("profileAdmin");
        log.info("GET request: Admin user detail");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/employee/add")
    public String getLoginTemplate() throws IOException {
        Template template = handlebarsTemplateLoader.getTemplate("adminAddNewEmployee");
        return template.apply("");
    }

    @PostMapping("/employee/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void signupSubmit(UserDTO newUserRequest, String password, String userType, HttpServletResponse response) throws Exceptions, IOException {
        userService.registerUser(newUserRequest, password, userType);
        log.info("POST request: Admin new user request");
        response.sendRedirect("/allEmployees");
    }

    @GetMapping("/allEmployees")
    public String getUsers() throws Exceptions, JSONException, IOException {
        ArrayList<User> allEmployees = (ArrayList<User>) userService.getAllEmployees();
        HashMap<String, ArrayList<User>> resultMap= new HashMap<>();
        resultMap.put("result", allEmployees);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("adminEmployeeAccess");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/delete/{id}")
    public void deleteUser(@PathVariable Long id, HttpServletResponse response) throws Exceptions, IOException {
        Optional<User> current = userService.getUserByIdAndActive(id);
        if (current == null) {
            throw new Exceptions("404", " ");
        }
        if (!(current.get().getUserType().equals("EMPLOYEE_ROLE1") || current.get().getUserType().equals("EMPLOYEE_ROLE2"))) {
            log.warn("GET request: Admin unauthorised request access");
            throw new Exceptions("401", "Unauthorized request !!");
        }

        userService.deleteUser(id);
        log.info("POST request: Employee New modification request");
        response.sendRedirect("../allEmployees");
    }

    @GetMapping("/viewEmployee/{id}")
    public String getUserDetail(@PathVariable Long id) throws Exceptions, JSONException, IOException {
        Optional<User> user = userService.getUserByIdAndActive(id);

        if (user == null) {
            throw new Exceptions("404", " ");
        }
        if (!(user.get().getUserType().equals("EMPLOYEE_ROLE1") || user.get().getUserType().equals("EMPLOYEE_ROLE2"))) {
            log.warn("GET request: Unauthorized request for external user");
            throw new Exceptions("409", " ");
        }

        JsonNode result = mapper.valueToTree(user.get());
        Template template = handlebarsTemplateLoader.getTemplate("adminViewEmployee");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @GetMapping("/requests")
    public String getAllUserRequest() throws JSONException, IOException {
        ArrayList<Request> allRequests = (ArrayList<Request>) requestService.getAllRequests();
        HashMap<String, ArrayList<Request>> resultMap= new HashMap<>();
        resultMap.put("result", allRequests);
        JsonNode result = mapper.valueToTree(resultMap);
        Template template = handlebarsTemplateLoader.getTemplate("adminHome");
        return template.apply(handlebarsTemplateLoader.getContext(result));
    }

    @PutMapping("/requests/approve/{id}")
    public void approveEdit(@PathVariable Long id) throws Exceptions {

        Optional<Request> request = requestService.getRequest(id);
        request.ifPresent(req -> {
            switch (req.getRequestType()) {
                case RequestType.TIER1_TO_TIER2:
                    userService.updateUserType(req.getRequestBy(), UserType.EMPLOYEE_ROLE2);
                    break;
                case RequestType.TIER2_TO_TIER1:
                    userService.updateUserType(req.getRequestBy(), UserType.EMPLOYEE_ROLE1);
                    break;
                case RequestType.UPDATE_PROFILE:
                    break;
            }
        });
    }

    /*
     @GetMapping("/requests/view/{id}")
    public JSONObject getUserRequest(@PathVariable() UUID id) throws Exceptions, JSONException {
        UpdateRequest updateRequest = UpdateRequestService.getUpdateRequest(id);
        JSONObject jsonObject = new JSONObject();
        if (updateRequest == null) {
            throw new Exceptions("404", "Invalid Request !");
        }
        if (!updateRequest.getUserType().equals("internal")) {
            log.warn("GET request: Admin unauthrorised request access");
            throw new Exceptions("401", "Unauthorised Request !");
        }
        jsonObject.put("modificationRequests", updateRequest);
        log.info("GET request: User modification request by ID");

        return jsonObject;
    }
    @GetMapping("/request/delete/{id}")
    public JSONObject getDeleteRequest(@PathVariable() UUID id) throws Exceptions, JSONException {
        UpdateRequest updateRequest = UpdateRequestService.getUpdateRequest(id);

        if (updateRequest == null) {
            throw new Exceptions("404","Invalid Request !");
        }
        if (!updateRequest.getUserType().equals("internal")) {
            log.warn("GET request: Admin unauthrorised request access");
            throw new Exceptions("401","Unauthorised Request !");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("modificationRequests", updateRequest);
        log.info("GET request: User modification request by ID");

        return jsonObject;
    }

    @PostMapping("/request/delete/{requestId}")
    public void deleteRequest(@PathVariable UUID requestId) throws Exceptions {
        UpdateRequest request = UpdateRequestService.getUpdateRequest(requestId);

        // checks validity of request
        if (request == null) {
            throw new Exceptions("404", "Invalid Request !");
        }

        if (!UpdateRequestService.verifyUpdateRequestUserType(requestId, "internal")) {
            log.warn("GET request: Admin unauthrorised request access");
            throw new Exceptions("401", "Unauthorised Request !");
        }
        UpdateRequestService.deleteUpdateRequest(request);
        log.info("POST request: Admin approves modification request");
    }
     */

    @GetMapping("/logDownload")
    public void doDownload(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

        /**
         * Size of a byte buffer to read/write file
         */
        final int BUFFER_SIZE = 4096;

        /**
         * Path of the file to be downloaded, relative to application's directory
         */
        String filePath = "logs/application.log";


        // get absolute path of the application
        ServletContext context = request.getServletContext();

        ClassLoader classLoader = getClass().getClassLoader();

        // construct the complete absolute path of the file
        File downloadFile = new File(classLoader.getResource(filePath).getFile());
        FileInputStream inputStream = new FileInputStream(downloadFile);

        // get MIME type of the file
        String mimeType = context.getMimeType("text/plain");
        if (mimeType == null) {
            // set to binary type if MIME mapping not found
            mimeType = "application/octet-stream";
        }
        System.out.println("MIME type: " + mimeType);

        // set content attributes for the response
        response.setContentType(mimeType);
        response.setContentLength((int) downloadFile.length());

        // set headers for the response
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"",
                downloadFile.getName());
        response.setHeader(headerKey, headerValue);

        // get output stream of the response
        OutputStream outStream = response.getOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = -1;

        // write bytes read from the input stream into the output stream
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outStream.close();

    }
}