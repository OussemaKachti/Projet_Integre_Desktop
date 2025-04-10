package test;
import java.util.List;

import entities.User;
import enums.RoleEnum;
import services.AuthService;
import services.UserService;

public class Main {
// Modify your Main.java to include these tests
public static void main(String[] args) {
    // Test User service
    UserService userService = new UserService();
    AuthService authService = new AuthService();
    
    try {
        // Test 1: Create a new user
        System.out.println("TEST 1: Creating new user");
        User user = new User("John", "Doe", "john.doe@test.com", "P@ssword123", RoleEnum.NON_MEMBRE);
        userService.ajouter(user);
        System.out.println("Created user with ID: " + user.getId());
        
        // Test 2: Find user by email
        System.out.println("\nTEST 2: Finding user by email");
        User foundUser = userService.findByEmail("john.doe@test.com");
        System.out.println("Found user: " + (foundUser != null ? foundUser.toString() : "Not found"));
        
        // Test 3: Authentication
        System.out.println("\nTEST 3: Testing authentication");
        User authUser = authService.authenticate("john.doe@test.com", "P@ssword123");
        System.out.println("Authentication result: " + (authUser != null ? "Success" : "Failed"));
        
        // Test 4: List all users
        System.out.println("\nTEST 4: Listing all users");
        List<User> allUsers = userService.recuperer();
        allUsers.forEach(System.out::println);
        
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        userService.close();
    }
}
}