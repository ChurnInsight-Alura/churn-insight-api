package com.alura.churnnsight.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.alura.churnnsight.dto.PredictRequestDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/predict")
public class PredictionController {

@GetMapping
public String prediction(){
        return  "Deposite microservicio";
}

@PostMapping
public ResponseEntity<?> predict(
        @Valid @RequestBody PredictRequestDTO request
) {
    // HU-04: solo validaciones y manejo de errores
    return ResponseEntity.ok().build();
    }
}

