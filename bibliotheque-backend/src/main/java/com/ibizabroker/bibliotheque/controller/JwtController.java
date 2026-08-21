package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
//@RequestMapping("/")
@Tag(name = "Authentification", description = "Génération du token JWT à partir des identifiants utilisateur")
public class JwtController {

    @Autowired
    private JwtService jwtService;

    @Operation(
            summary = "S'authentifier",
            description = "Vérifie les identifiants et retourne un token JWT accompagné des informations de l'utilisateur. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie, token JWT retourné"),
            @ApiResponse(responseCode = "400", description = "Corps de requête invalide"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides ou compte désactivé")
    })
    @PostMapping("/authenticate")
    public JwtResponse createJwtToken(@RequestBody JwtRequest jwtRequest) {
        return jwtService.createJwtToken(jwtRequest);
    }
}