package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin")
@Tag(name = "Gestion des utilisateurs", description = "Création et administration des comptes utilisateurs")
public class AdminController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(
            summary = "Créer un utilisateur",
            description = "Crée un nouvel utilisateur et hache son mot de passe (BCrypt) avant sauvegarde. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Corps de requête invalide"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle Admin requis")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/users")
    @PreAuthorize("hasRole('Admin')")
    public Users addUserByAdmin(@RequestBody Users user) {
//        Role role = new Role();
////        role.setRoleName(UserConstant.DEFAULT_ROLE);
//        role.setRoleName(role.getRoleName());
//        Set<Role> setRole = new HashSet<>();
//        setRole.add(role);
//        user.setRole(setRole);
        String password = user.getPassword();
        String encryptPassword = passwordEncoder.encode(password);
        user.setPassword(encryptPassword);
        usersRepository.save(user);
        return user;
    }

    @Operation(
            summary = "Lister tous les utilisateurs",
            description = "Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des utilisateurs retournée avec succès"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle Admin requis")
    })
    @GetMapping("/users")
    @PreAuthorize("hasRole('Admin')")
    public List<Users> getAllUsers() {
        return usersRepository.findAll();
    }

    @Operation(
            summary = "Consulter un utilisateur par son identifiant",
            description = "Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle Admin requis"),
            @ApiResponse(responseCode = "404", description = "Aucun utilisateur ne correspond à l'identifiant fourni")
    })
    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/users/{id}")
    public ResponseEntity<Users> getUserById(@PathVariable Integer id) {
        Users user = usersRepository.findById(id).orElseThrow(() -> new NotFoundException("User with id "+ id +" does not exist."));
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Mettre à jour un utilisateur",
            description = "Modifie le nom, le nom d'utilisateur et les rôles d'un utilisateur existant. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Corps de requête invalide"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle Admin requis"),
            @ApiResponse(responseCode = "404", description = "Aucun utilisateur ne correspond à l'identifiant fourni")
    })
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/users/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable Integer id, @RequestBody Users userDetails) {
        Users user = usersRepository.findById(id).orElseThrow(() -> new NotFoundException("User with id "+ id +" does not exist."));

        user.setName(userDetails.getName());
        user.setRole(userDetails.getRole());
        user.setUsername(userDetails.getUsername());

        Users updatedUser = usersRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }
}