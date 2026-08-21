package com.ibizabroker.bibliotheque.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active l'infrastructure Spring des taches planifiees (@Scheduled).
 * Utilisee par ReservationExpirationScheduler pour faire passer
 * automatiquement en EXPIREE les reservations actives dont la date
 * d'expiration est depassee.
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}