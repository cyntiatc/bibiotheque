package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin")
@Tag(name = "Gestion des livres", description = "Consultation et administration du catalogue de livres")
public class BooksController {

    @Autowired
    private BooksRepository booksRepository;

    @Operation(
            summary = "Lister tous les livres",
            description = "Retourne l'intégralité du catalogue de livres. Endpoint public, accessible sans authentification."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des livres retournée avec succès")
    })
    @GetMapping("/books")
    public List<Books> getAllBooks(){
        return booksRepository.findAll();
    }

    @Operation(
            summary = "Consulter un livre par son identifiant",
            description = "Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livre trouvé"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle Admin requis"),
            @ApiResponse(responseCode = "404", description = "Aucun livre ne correspond à l'identifiant fourni")
    })
    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/books/{id}")
    public ResponseEntity<Books> getBookById(@PathVariable Integer id) {
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));
        return ResponseEntity.ok(book);
    }

    @Operation(
            summary = "Créer un livre",
            description = "Ajoute un nouveau livre au catalogue. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Livre créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Corps de requête invalide"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle Admin requis")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/books")
    public Books createBook(@RequestBody Books book) {
        return booksRepository.save(book);
    }

    @Operation(
            summary = "Mettre à jour un livre",
            description = "Modifie le nom, l'auteur, le genre et le nombre d'exemplaires d'un livre existant. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livre mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Corps de requête invalide"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle Admin requis"),
            @ApiResponse(responseCode = "404", description = "Aucun livre ne correspond à l'identifiant fourni")
    })
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/books/{id}")
    public ResponseEntity<Books> updateBook(@PathVariable Integer id, @RequestBody Books bookDetails) {
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));

        book.setBookName(bookDetails.getBookName());
        book.setBookAuthor(bookDetails.getBookAuthor());
        book.setBookGenre(bookDetails.getBookGenre());
        book.setNoOfCopies(bookDetails.getNoOfCopies());

        Books updatedBook = booksRepository.save(book);
        return ResponseEntity.ok(updatedBook);
    }

    @Operation(
            summary = "Supprimer un livre",
            description = "Supprime définitivement un livre du catalogue. Réservé aux administrateurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livre supprimé avec succès"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle Admin requis"),
            @ApiResponse(responseCode = "404", description = "Aucun livre ne correspond à l'identifiant fourni")
    })
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/books/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteBook(@PathVariable Integer id) {
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));

        booksRepository.delete(book);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }
}