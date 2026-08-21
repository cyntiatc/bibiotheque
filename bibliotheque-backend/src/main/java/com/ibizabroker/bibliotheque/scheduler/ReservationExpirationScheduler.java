package com.ibizabroker.bibliotheque.scheduler;

import com.ibizabroker.bibliotheque.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tache planifiee (bonus) : declenche periodiquement le passage automatique
 * en EXPIREE des reservations actives (EN_ATTENTE/DISPONIBLE) dont la
 * dateExpiration est depassee.
 * <p>
 * Cette classe ne porte aucune regle de gestion : elle se contente de
 * declencher l'appel selon le rythme configure. Toute la logique metier
 * (quelles reservations expirent, comment) reste dans
 * {@link ReservationService#expirerReservationsActives()}, testable sans
 * dependre de l'infrastructure de scheduling.
 */
@Component
public class ReservationExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    @Autowired
    private ReservationService reservationService;

    // Rythme configurable via application.properties (defaut : 5 minutes).
    // Premiere execution immediate au demarrage (comportement par defaut de
    // @Scheduled(fixedRate) sans initialDelay) : rattrape les reservations
    // deja expirees pendant que l'application etait arretee.
    @Scheduled(fixedRateString = "${reservation.expiration.check-rate-ms:300000}")
    public void expirerReservationsEnRetard() {
        int nombreExpirees = reservationService.expirerReservationsActives();
        if (nombreExpirees > 0) {
            log.info("{} réservation(s) passée(s) au statut EXPIREE.", nombreExpirees);
        }
    }
}