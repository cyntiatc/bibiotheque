package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    List<Reservation> findByStatut(StatutReservation statut);

    List<Reservation> findByAdherent_UserId(Integer adherentId);

    List<Reservation> findByStatutAndAdherent_UserId(StatutReservation statut, Integer adherentId);

    // RG-02 : un adherent ne peut avoir qu'une seule reservation active pour un meme livre.
    boolean existsByLivreAndAdherentAndStatutIn(Books livre, Users adherent, Collection<StatutReservation> statuts);

    // RG-03 : un adherent ne peut pas depasser 3 reservations actives simultanees.
    long countByAdherentAndStatutIn(Users adherent, Collection<StatutReservation> statuts);

    // Tache planifiee : reservations actives dont la date d'expiration est depassee.
    List<Reservation> findByStatutInAndDateExpirationBefore(Collection<StatutReservation> statuts, LocalDateTime instant);
}