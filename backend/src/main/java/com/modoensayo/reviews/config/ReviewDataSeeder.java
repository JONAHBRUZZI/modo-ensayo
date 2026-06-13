package com.modoensayo.reviews.config;

import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.reviews.domain.Review;
import com.modoensayo.reviews.repository.ReviewRepository;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.repository.UserRepository;
import com.modoensayo.venues.domain.Venue;
import com.modoensayo.venues.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewDataSeeder implements CommandLineRunner {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final ClassRepository classRepository;

    private final Random random = new Random();

    private static final String[] COMENTARIOS_POSITIVOS = {
        "Excelente profesor, muy dedicado y paciente.",
        "Las instalaciones son increibles, todo muy limpio.",
        "Aprendi muchisimo en esta clase, totalmente recomendado.",
        "El ambiente es muy profesional y acogedor.",
        "Muy buena experiencia, volvere sin duda.",
        "El profesor explica muy bien, se nota su experiencia.",
        "La sala esta muy bien equipada, excelente sonido.",
        "Puntual y muy organizado, 100% recomendable.",
        "Supero mis expectativas, gran calidad de ensenanza.",
        "Muy buen trato y profesionalismo.",
        "El espacio es comodo y bien iluminado.",
        "Clase muy dinamica y entretenida.",
        "Buena relacion precio-calidad.",
        "El profesor tiene mucha paciencia con principiantes.",
        "Todo el equipamiento en perfecto estado.",
        "Gran metodologia de ensenanza.",
        "Sala amplia y con buena ventilacion.",
        "Excelente atencion, se preocupan por los detalles.",
        "Muy recomendable para todos los niveles.",
        "La mejor clase que he tomado en mucho tiempo.",
        "Profesor muy motivador y entusiasta.",
        "Insonorizacion perfecta, no hay ruido externo.",
        "El lugar es facil de llegar, bien ubicado.",
        "Material de apoyo muy completo.",
        "Clase muy bien estructurada y con objetivos claros."
    };

    @Override
    public void run(String... args) {
        long existing = reviewRepository.count();
        if (existing >= 50) {
            log.info("Ya existen {} reviews, se omite la generacion de datos de prueba.", existing);
            return;
        }

        List<User> users = userRepository.findAll();
        List<Venue> venues = venueRepository.findAll();

        if (users.isEmpty() || venues.isEmpty()) {
            log.info("Sin datos suficientes para generar reviews de prueba.");
            return;
        }

        List<Review> reviews = new ArrayList<>();
        var existingClasses = classRepository.findAll().stream()
                .filter(c -> c.getStartTime() != null && c.getStartTime().isBefore(Instant.now()))
                .toList();

        for (int i = 0; i < 55; i++) {
            User reviewer = users.get(random.nextInt(users.size()));
            String targetType = random.nextDouble() < 0.6 ? "TEACHER" : "VENUE";
            UUID targetId;

            if ("TEACHER".equals(targetType)) {
                // Usamos un profesor existente o un usuario como target TEACHER
                var teachers = users.stream()
                        .filter(u -> u.getUserRoles().stream().anyMatch(ur -> "TEACHER".equals(ur.getRole().getName())))
                        .toList();
                if (teachers.isEmpty()) continue;
                targetId = teachers.get(random.nextInt(teachers.size())).getId();
            } else {
                targetId = venues.get(random.nextInt(venues.size())).getId();
            }

            int score = random.nextInt(3) + 3;
            String comentario = COMENTARIOS_POSITIVOS[random.nextInt(COMENTARIOS_POSITIVOS.length)];

            UUID classRef = existingClasses.isEmpty() ? null
                    : existingClasses.get(random.nextInt(existingClasses.size())).getId();

            Instant createdAt = Instant.now().minus(random.nextInt(30) + 1, ChronoUnit.DAYS);

            Review review = Review.builder()
                    .classId(classRef)
                    .reviewerId(reviewer.getId())
                    .targetType(targetType)
                    .targetId(targetId)
                    .score(score)
                    .comment(comentario)
                    .createdAt(createdAt)
                    .build();
            reviews.add(review);
        }

        reviewRepository.saveAll(reviews);
        log.info("Se generaron {} reviews de prueba exitosamente.", reviews.size());
    }
}
