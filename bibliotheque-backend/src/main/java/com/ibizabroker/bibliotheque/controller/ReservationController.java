package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations", description = "Gestion des réservations de livres")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une réservation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée"),
            @ApiResponse(responseCode = "400", description = "Requête invalide (livreId/adherentId manquant)"),
            @ApiResponse(responseCode = "404", description = "Livre ou adhérent introuvable"),
            @ApiResponse(responseCode = "409", description = "Règle de gestion violée (RG-01, RG-02 ou RG-03)")
    })
    public ReservationResponseDTO creerReservation(@Valid @RequestBody ReservationRequestDTO requestDTO) {
        return reservationService.creerReservation(requestDTO);
    }

    @GetMapping
    @Operation(summary = "Lister les réservations", description = "Filtres optionnels par statut et/ou par adhérent")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des réservations")
    })
    public List<ReservationResponseDTO> listerReservations(
            @RequestParam(required = false) StatutReservation statut,
            @RequestParam(required = false) Integer adherentId) {
        return reservationService.listerReservations(statut, adherentId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une réservation par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<ReservationResponseDTO> obtenirReservation(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.obtenirReservation(id));
    }

    @PatchMapping("/{id}/annuler")
    @Operation(summary = "Annuler une réservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation annulée"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "409", description = "Réservation non annulable (RG-05, RG-06)")
    })
    public ResponseEntity<ReservationResponseDTO> annulerReservation(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.annulerReservation(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une réservation")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation supprimée"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public void supprimerReservation(@PathVariable Integer id) {
        reservationService.supprimerReservation(id);
    }
}