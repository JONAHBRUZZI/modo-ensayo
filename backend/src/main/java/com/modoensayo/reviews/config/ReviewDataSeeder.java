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
        var classes = classRepository.findAll();

        if (users.isEmpty() || venues.isEmpty() || classes.isEmpty()) {
            log.info("Sin datos suficientes para generar reviews de prueba.");
            return;
        }

        // Generar 55 reviews (algunos se duplicaran en clase pero con distinto reviewer)
        List<Review> reviews = new ArrayList<>();
        var existingClasses = classes.stream()
                .filter(c -> c.getStartTime() != null && c.getStartTime().isBefore(Instant.now()))
                .toList();

        if (existingClasses.isEmpty()) {
            log.info("Sin clases pasadas para generar reviews de prueba.");
            return;
        }

        for (int i = 0; i < 55; i++) {
            var cls = existingClasses.get(random.nextInt(existingClasses.size()));
            User reviewer = users.get(random.nextInt(users.size()));

            // Evitar auto-reviews (profesor revisando su propia clase)
            if (cls.getTeacherId() != null && cls.getTeacherId().equals(reviewer.getId())) {
                continue;
            }

            // Verificar si ya existe review de este usuario para esta clase
            boolean existe = reviewRepository.findByClassId(cls.getId()).stream()
                    .anyMatch(r -> r.getReviewerId().equals(reviewer.getId()));
            if (existe) continue;

            int score = random.nextInt(3) + 3; // 3-5 estrellas
            String comentario = COMENTARIOS_POSITIVOS[random.nextInt(COMENTARIOS_POSITIVOS.length)];

            // 60% reviews para profesor (TEACHER), 40% para sede (VENUE)
            String targetType = random.nextDouble() < 0.6 ? "TEACHER" : "VENUE";
            UUID targetId;
            if ("TEACHER".equals(targetType) && cls.getTeacherId() != null) {
                targetId = cls.getTeacherId();
            } else {
                targetId = venues.get(random.nextInt(venues.size())).getId();
            }

            Instant createdAt = cls.getStartTime().plus(random.nextInt(14) + 1, ChronoUnit.DAYS);
            if (createdAt.isAfter(Instant.now())) createdAt = Instant.now().minus(random.nextInt(7) + 1, ChronoUnit.DAYS);

            Review review = Review.builder()
                    .classId(cls.getId())
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
