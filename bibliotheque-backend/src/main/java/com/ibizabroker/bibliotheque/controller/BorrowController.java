package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Repository
@RestController
@RequestMapping("/borrow")
@Tag(name = "Gestion des emprunts", description = "Emprunt, retour et historique des emprunts de livres")
public class BorrowController {

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Operation(
            summary = "Emprunter un livre",
            description = "Décrémente le nombre d'exemplaires disponibles et enregistre l'emprunt avec une date d'échéance à 7 jours. " +
                    "Endpoint public (aucune authentification requise)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Emprunt enregistré avec succès"),
            @ApiResponse(responseCode = "404", description = "Aucun livre ou utilisateur ne correspond à l'identifiant fourni"),
            @ApiResponse(responseCode = "409", description = "Le livre n'a plus d'exemplaire disponible")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public String borrowBook(@RequestBody Borrow borrow) {
        Users user = usersRepository.findById(borrow.getUserId())
                .orElseThrow(() -> new NotFoundException("User with id " + borrow.getUserId() + " does not exist."));
        Books book = booksRepository.findById(borrow.getBookId())
                .orElseThrow(() -> new NotFoundException("Book with id " + borrow.getBookId() + " does not exist."));

        if (book.getNoOfCopies() < 1) {
            throw new ConflictException("Le livre \"" + book.getBookName() + "\" n'a plus d'exemplaire disponible.");
        }

        book.borrowBook();
        booksRepository.save(book);

        Date currentDate = new Date();
        Date overdueDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(overdueDate);
        c.add(Calendar.DATE, 7);
        overdueDate = c.getTime();
        borrow.setIssueDate(currentDate);
        borrow.setDueDate(overdueDate);
        borrowRepository.save(borrow);
        return user.getName() + " has borrowed one copy of \"" + book.getBookName() + "\"!";
    }

    @Operation(
            summary = "Lister tous les emprunts",
            description = "Retourne l'historique complet des emprunts, en cours et terminés."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des emprunts retournée avec succès")
    })
    @GetMapping
    public List<Borrow> getAllBorrow() {
        return borrowRepository.findAll();
    }

    @Operation(
            summary = "Retourner un livre emprunté",
            description = "Incrémente le nombre d'exemplaires disponibles et renseigne la date de retour de l'emprunt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retour enregistré avec succès"),
            @ApiResponse(responseCode = "404", description = "Aucun emprunt ou livre ne correspond à l'identifiant fourni")
    })
    @PutMapping
    public Borrow returnBook(@RequestBody Borrow borrow) {
        Borrow borrowBook = borrowRepository.findById(borrow.getBorrowId())
                .orElseThrow(() -> new NotFoundException("Borrow with id " + borrow.getBorrowId() + " does not exist."));
        Books book = booksRepository.findById(borrowBook.getBookId())
                .orElseThrow(() -> new NotFoundException("Book with id " + borrowBook.getBookId() + " does not exist."));

        book.returnBook();
        booksRepository.save(book);

        Date currentDate = new Date();
        borrowBook.setReturnDate(currentDate);
        return borrowRepository.save(borrowBook);
    }

    @Operation(
            summary = "Lister les emprunts d'un utilisateur",
            description = "Retourne l'historique des emprunts effectués par l'utilisateur dont l'identifiant est fourni."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des emprunts de l'utilisateur retournée avec succès (liste vide si aucun emprunt)")
    })
    @GetMapping("user/{id}")
    public List<Borrow> booksBorrowedByUser(@PathVariable Integer id) {
        return borrowRepository.findByUserId(id);
    }

    @Operation(
            summary = "Lister l'historique des emprunts d'un livre",
            description = "Retourne l'historique des emprunts effectués pour le livre dont l'identifiant est fourni."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des emprunts du livre retourné avec succès (liste vide si aucun emprunt)")
    })
    @GetMapping("book/{id}")
    public List<Borrow> bookBorrowHistory(@PathVariable Integer id) {
        return borrowRepository.findByBookId(id);
    }


//    @Autowired
//    private EntityManager entityManager;
//
//    @PostMapping
//    public Borrow borrowBook(@RequestBody Borrow borrow) {
//        borrowRepository.save(borrow);
//        Books book = booksRepository.findById(borrow.getBOOKID()).orElseThrow(() -> new NotFoundException("Book not found."));
//        if(book.getNoOfCopies()-1 < 0) {
//            throw new IllegalStateException("There are no available books.");
//        }
//        book.borrowBook();
//        booksRepository.save(book);
//
//        return borrow;
//    }
//
//    @GetMapping
//    public List<Borrow> getAllBorrow() {
//        return borrowRepository.findAll();
//    }
//
//    @PutMapping
//    public Borrow returnBook(@RequestBody Borrow borrow) {
//        borrowRepository.save(borrow);
//        Books book = booksRepository.findById(borrow.getBOOKID()).orElseThrow(() -> new NotFoundException("Book not found."));
//        book.returnBook();
//        booksRepository.save(book);
//
//        Date currentDate = new Date(new java.util.Date().getTime());
//        borrow.setReturnDate(currentDate);
//        return borrow;
//    }
//
//    @GetMapping("user/{id}")
//    public List<Books> booksBorrowedByUser(@PathVariable Integer id) {
//        Query q = entityManager.createNativeQuery("SELECT * FROM BOOKS AS B, BORROW AS L WHERE B.book_id = L.BOOKID AND L.USERID = " + id);
//        List<Books> borrowedBooks = q.getResultList();
//        return borrowedBooks;
//    }
//
//    @GetMapping("book/{id}")
//    public List<Users> bookBorrowHistory(@PathVariable Integer id) {
//        Query q = entityManager.createNativeQuery("SELECT * FROM USERS AS U, BORROW AS L WHERE U.user_id = L.USERID AND L.BOOKID = " + id);
//        List<Users> usersList = q.getResultList();
//        return usersList;
//    }

//    @PostMapping
//    public Borrow borrowBook(@RequestBody Borrow borrow) {
//        borrow(borrow.getBorrowId(), borrow.getUser().getUserId(), borrow.getBook().getBookId());
//        return borrow;
//    }
//
//    @GetMapping
//    public List<Borrow> getAllBorrow() {
//        return borrowRepository.findAll();
//    }
//
//    @PutMapping
//    public Borrow returnBook(@RequestBody Borrow borrow) {
//        Books book = booksRepository.findById(borrow.getBook().getBookId()).orElseThrow(() -> new NotFoundException("Book not found."));
//        book.returnBook();
//        booksRepository.save(book);
//
//        Date currentDate = new Date(new java.util.Date().getTime());
//        borrow.setReturnDate(currentDate);
//        return borrowRepository.save(borrow);
//    }
//
//    @GetMapping("user/{id}")
//    public List<Books> booksBorrowedByUser(@PathVariable Integer id) {
//        Users user = usersRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found."));
//        return user.getBooks();
//    }
//
//    @GetMapping("book/{id}")
//    public List<Users> bookBorrowHistory(@PathVariable Integer id) {
//        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found."));
//        return book.getUsers();
//    }
//
//    public void borrow(Integer borrowId, Integer userId, Integer bookId) {
//        Users user = usersRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
//        if(user.getBooks().stream().anyMatch(book -> Objects.equals(book.getBookId(), bookId))) {
//            throw new IllegalStateException("User already borrowed the book");
//        }
//
//        Books book = booksRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book not found."));
//        if(book.getNoOfCopies()-1 < 0) {
//            throw new IllegalStateException("There are no available books.");
//        }
//
//        book.getUsers().add(user);
//        book.setNoOfCopies(book.getNoOfCopies()-1);
//        booksRepository.save(book);
//
//        user.getBooks().add(book);
//        usersRepository.save(user);
//    }

}