package com.modoensayo.classes.controller;

import com.modoensayo.classes.dto.SearchRequest;
import com.modoensayo.classes.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<List<Object>> search(@ModelAttribute SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }
}
