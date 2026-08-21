package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final int DUREE_VALIDITE_JOURS = 7;
    private static final int MAX_RESERVATIONS_ACTIVES = 3;

    // Statuts "actifs" au sens de l'enonce : EN_ATTENTE ou DISPONIBLE.
    private static final EnumSet<StatutReservation> STATUTS_ACTIFS =
            EnumSet.of(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Transactional
    public ReservationResponseDTO creerReservation(ReservationRequestDTO requestDTO) {
        Books livre = booksRepository.findById(requestDTO.livreId())
                .orElseThrow(() -> new NotFoundException("Book with id " + requestDTO.livreId() + " does not exist."));

        Users adherent = usersRepository.findById(requestDTO.adherentId())
                .orElseThrow(() -> new NotFoundException("User with id " + requestDTO.adherentId() + " does not exist."));

        // RG-01 : on ne peut reserver qu'un livre indisponible.
        if (livre.getNoOfCopies() != null && livre.getNoOfCopies() > 0) {
            throw new ConflictException("RG-01 : le livre \"" + livre.getBookName() + "\" est disponible (" +
                    livre.getNoOfCopies() + " exemplaire(s) restant(s)), il ne peut pas être réservé.");
        }

        // RG-02 : un adherent ne peut avoir qu'une seule reservation active pour un meme livre.
        if (reservationRepository.existsByLivreAndAdherentAndStatutIn(livre, adherent, STATUTS_ACTIFS)) {
            throw new ConflictException("RG-02 : l'adhérent id=" + adherent.getUserId() +
                    " a déjà une réservation active pour le livre \"" + livre.getBookName() + "\".");
        }

        // RG-03 : un adherent ne peut pas depasser 3 reservations actives simultanees.
        long reservationsActives = reservationRepository.countByAdherentAndStatutIn(adherent, STATUTS_ACTIFS);
        if (reservationsActives >= MAX_RESERVATIONS_ACTIVES) {
            throw new ConflictException("RG-03 : l'adhérent id=" + adherent.getUserId() +
                    " a atteint la limite de " + MAX_RESERVATIONS_ACTIVES + " réservations actives simultanées.");
        }

        // RG-04 : dateReservation/dateExpiration generees cote serveur, jamais fournies par le client.
        LocalDateTime dateReservation = LocalDateTime.now();
        LocalDateTime dateExpiration = dateReservation.plusDays(DUREE_VALIDITE_JOURS);

        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(dateReservation);
        reservation.setDateExpiration(dateExpiration);
        reservation.setStatut(StatutReservation.EN_ATTENTE);

        return toResponseDTO(reservationRepository.save(reservation));
    }

    public List<ReservationResponseDTO> listerReservations(StatutReservation statut, Integer adherentId) {
        List<Reservation> reservations;
        if (statut != null && adherentId != null) {
            reservations = reservationRepository.findByStatutAndAdherent_UserId(statut, adherentId);
        } else if (statut != null) {
            reservations = reservationRepository.findByStatut(statut);
        } else if (adherentId != null) {
            reservations = reservationRepository.findByAdherent_UserId(adherentId);
        } else {
            reservations = reservationRepository.findAll();
        }
        return reservations.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public ReservationResponseDTO obtenirReservation(Integer id) {
        return toResponseDTO(getReservationOrThrow(id));
    }

    @Transactional
    public ReservationResponseDTO annulerReservation(Integer id) {
        Reservation reservation = getReservationOrThrow(id);

        // RG-05 : seule une reservation EN_ATTENTE ou DISPONIBLE peut etre annulee.
        // RG-06 : une reservation ANNULEE, EXPIREE ou HONOREE ne change plus d'etat.
        if (!STATUTS_ACTIFS.contains(reservation.getStatut())) {
            throw new ConflictException("RG-05/RG-06 : la réservation id=" + id + " est au statut " +
                    reservation.getStatut() + " et ne peut plus être annulée.");
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        return toResponseDTO(reservationRepository.save(reservation));
    }

    @Transactional
    public void supprimerReservation(Integer id) {
        reservationRepository.delete(getReservationOrThrow(id));
    }

    // Appelee par ReservationExpirationScheduler (tache planifiee, bonus). Reste
    // ici et non dans le scheduler : la logique metier reste exclusivement dans
    // le Service, le scheduler ne fait que declencher l'appel selon un rythme.
    @Transactional
    public int expirerReservationsActives() {
        List<Reservation> reservationsEnRetard = reservationRepository
                .findByStatutInAndDateExpirationBefore(STATUTS_ACTIFS, LocalDateTime.now());

        reservationsEnRetard.forEach(reservation -> reservation.setStatut(StatutReservation.EXPIREE));
        reservationRepository.saveAll(reservationsEnRetard);

        return reservationsEnRetard.size();
    }

    private Reservation getReservationOrThrow(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation with id " + id + " does not exist."));
    }

    private ReservationResponseDTO toResponseDTO(Reservation reservation) {
        return new ReservationResponseDTO(
                reservation.getReservationId(),
                reservation.getLivre().getBookId(),
                reservation.getLivre().getBookName(),
                reservation.getAdherent().getUserId(),
                reservation.getAdherent().getName(),
                reservation.getDateReservation(),
                reservation.getDateExpiration(),
                reservation.getStatut()
        );
    }
}