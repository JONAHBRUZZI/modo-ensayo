package com.modoensayo.shared.config;

import com.modoensayo.classes.domain.DisciplineCatalog;
import com.modoensayo.classes.repository.DisciplineCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DisciplineCatalogSeeder implements CommandLineRunner {

    private final DisciplineCatalogRepository repo;

    @Override
    public void run(String... args) {
        repo.deleteAll();
        int i = 0;
        // Danza
        repo.save(discipline("Folclore", "Danza", i++));
        repo.save(discipline("Contemporaneo", "Danza", i++));
        repo.save(discipline("Ballet", "Danza", i++));
        repo.save(discipline("Urbano", "Danza", i++));
        // Musica
        repo.save(discipline("Folclore", "Musica", i++));
        repo.save(discipline("Instrumental", "Musica", i++));
        repo.save(discipline("Moderno", "Musica", i++));
        repo.save(discipline("Guitarra", "Musica", i++));
        repo.save(discipline("Bateria", "Musica", i++));
        repo.save(discipline("Bajo", "Musica", i++));
        repo.save(discipline("Canto", "Musica", i++));
        repo.save(discipline("Piano", "Musica", i++));
        repo.save(discipline("Violin", "Musica", i++));
        repo.save(discipline("Saxofon", "Musica", i++));
        // Teatro
        repo.save(discipline("Clasico", "Teatro", i++));
        repo.save(discipline("Contemporaneo", "Teatro", i++));
        repo.save(discipline("Musical", "Teatro", i++));
        repo.save(discipline("Improvisacion", "Teatro", i++));
        repo.save(discipline("Dramaturgia", "Teatro", i++));

        log.info("DisciplineCatalogSeeder: {} disciplinas predefinidas creadas", i);
    }

    private DisciplineCatalog discipline(String name, String category, int order) {
        return DisciplineCatalog.builder()
                .name(name).category(category).sortOrder(order).active(true).build();
    }
}
