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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires des regles de gestion (RG-01 a RG-06) portees par
 * ReservationService. Repositories mockes : la logique metier vit
 * exclusivement dans le service, on l'isole donc de toute base de donnees.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Books livreIndisponible;
    private Books livreDisponible;
    private Users adherent;

    @BeforeEach
    void setUp() {
        livreIndisponible = new Books();
        livreIndisponible.setBookId(1);
        livreIndisponible.setBookName("1984");
        livreIndisponible.setNoOfCopies(0);

        livreDisponible = new Books();
        livreDisponible.setBookId(2);
        livreDisponible.setBookName("Les Misérables");
        livreDisponible.setNoOfCopies(3);

        adherent = new Users();
        adherent.setUserId(10);
        adherent.setName("Cyntia");
    }

    // ------------------------------------------------------------------
    // RG-01 : on ne peut reserver qu'un livre indisponible.
    // ------------------------------------------------------------------

    @Test
    void creerReservation_livreDisponible_rejeteeParRG01() {
        when(booksRepository.findById(2)).thenReturn(Optional.of(livreDisponible));
        when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));

        ReservationRequestDTO requestDTO = new ReservationRequestDTO(2, 10);

        assertThatThrownBy(() -> reservationService.creerReservation(requestDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-01");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerReservation_livreIndisponible_autoriseeParRG01() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByLivreAndAdherentAndStatutIn(any(), any(), anyCollection())).thenReturn(false);
        when(reservationRepository.countByAdherentAndStatutIn(any(), anyCollection())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponseDTO response = reservationService.creerReservation(new ReservationRequestDTO(1, 10));

        assertThat(response.statut()).isEqualTo(StatutReservation.EN_ATTENTE);
        verify(reservationRepository).save(any(Reservation.class));
    }

    // ------------------------------------------------------------------
    // RG-02 : un adherent ne peut avoir qu'une seule reservation active
    // pour un meme livre.
    // ------------------------------------------------------------------

    @Test
    void creerReservation_reservationActiveExistantePourMemeLivre_rejeteeParRG02() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByLivreAndAdherentAndStatutIn(any(), any(), anyCollection())).thenReturn(true);

        ReservationRequestDTO requestDTO = new ReservationRequestDTO(1, 10);

        assertThatThrownBy(() -> reservationService.creerReservation(requestDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-02");

        verify(reservationRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // RG-03 : un adherent ne peut pas depasser 3 reservations actives
    // simultanees.
    // ------------------------------------------------------------------

    @Test
    void creerReservation_adherentA3ReservationsActives_rejeteeParRG03() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByLivreAndAdherentAndStatutIn(any(), any(), anyCollection())).thenReturn(false);
        when(reservationRepository.countByAdherentAndStatutIn(any(), anyCollection())).thenReturn(3L);

        ReservationRequestDTO requestDTO = new ReservationRequestDTO(1, 10);

        assertThatThrownBy(() -> reservationService.creerReservation(requestDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-03");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerReservation_adherentA2ReservationsActives_autoriseeParRG03() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByLivreAndAdherentAndStatutIn(any(), any(), anyCollection())).thenReturn(false);
        when(reservationRepository.countByAdherentAndStatutIn(any(), anyCollection())).thenReturn(2L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponseDTO response = reservationService.creerReservation(new ReservationRequestDTO(1, 10));

        assertThat(response).isNotNull();
        verify(reservationRepository).save(any(Reservation.class));
    }

    // ------------------------------------------------------------------
    // RG-04 : dateExpiration = dateReservation + 7 jours, calculees cote
    // serveur (jamais fournies par le client : absentes du DTO de requete).
    // ------------------------------------------------------------------

    @Test
    void creerReservation_dateExpirationFixeeA7JoursDeDateReservation() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(10)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByLivreAndAdherentAndStatutIn(any(), any(), anyCollection())).thenReturn(false);
        when(reservationRepository.countByAdherentAndStatutIn(any(), anyCollection())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime avant = LocalDateTime.now();
        ReservationResponseDTO response = reservationService.creerReservation(new ReservationRequestDTO(1, 10));
        LocalDateTime apres = LocalDateTime.now();

        assertThat(response.dateReservation()).isBetween(avant, apres);
        assertThat(response.dateExpiration()).isEqualTo(response.dateReservation().plusDays(7));
    }

    // ------------------------------------------------------------------
    // RG-05 : seule une reservation EN_ATTENTE ou DISPONIBLE peut etre
    // annulee.
    // RG-06 : une reservation ANNULEE, EXPIREE ou HONOREE ne peut plus
    // changer d'etat.
    // ------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = StatutReservation.class, names = {"EN_ATTENTE", "DISPONIBLE"})
    void annulerReservation_statutActif_annuleeAvecSuccesParRG05(StatutReservation statutInitial) {
        Reservation reservation = reservationExistante(statutInitial);
        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponseDTO response = reservationService.annulerReservation(1);

        assertThat(response.statut()).isEqualTo(StatutReservation.ANNULEE);
    }

    @ParameterizedTest
    @EnumSource(value = StatutReservation.class, names = {"ANNULEE", "EXPIREE", "HONOREE"})
    void annulerReservation_statutTerminal_rejeteeParRG06(StatutReservation statutInitial) {
        Reservation reservation = reservationExistante(statutInitial);
        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.annulerReservation(1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-05/RG-06");

        verify(reservationRepository, never()).save(any());
        assertThat(reservation.getStatut()).isEqualTo(statutInitial);
    }

    // ------------------------------------------------------------------
    // Tache planifiee (bonus) : expiration automatique des reservations
    // actives dont la dateExpiration est depassee.
    // ------------------------------------------------------------------

    @Test
    void expirerReservationsActives_reservationsEnRetard_passentAuStatutExpiree() {
        Reservation enRetard1 = reservationExistante(StatutReservation.EN_ATTENTE);
        Reservation enRetard2 = reservationExistante(StatutReservation.DISPONIBLE);
        when(reservationRepository.findByStatutInAndDateExpirationBefore(anyCollection(), any()))
                .thenReturn(List.of(enRetard1, enRetard2));

        int nombreExpirees = reservationService.expirerReservationsActives();

        assertThat(nombreExpirees).isEqualTo(2);
        assertThat(enRetard1.getStatut()).isEqualTo(StatutReservation.EXPIREE);
        assertThat(enRetard2.getStatut()).isEqualTo(StatutReservation.EXPIREE);
        verify(reservationRepository).saveAll(List.of(enRetard1, enRetard2));
    }

    @Test
    void expirerReservationsActives_aucuneReservationEnRetard_neModifieRien() {
        when(reservationRepository.findByStatutInAndDateExpirationBefore(anyCollection(), any()))
                .thenReturn(List.of());

        int nombreExpirees = reservationService.expirerReservationsActives();

        assertThat(nombreExpirees).isZero();
        verify(reservationRepository).saveAll(List.of());
    }

    private Reservation reservationExistante(StatutReservation statut) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(1);
        reservation.setLivre(livreIndisponible);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(LocalDateTime.now().minusDays(1));
        reservation.setDateExpiration(LocalDateTime.now().plusDays(6));
        reservation.setStatut(statut);
        return reservation;
    }
}