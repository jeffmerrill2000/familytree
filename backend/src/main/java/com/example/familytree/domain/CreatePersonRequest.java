package com.example.familytree.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreatePersonRequest(
		@NotBlank String givenName,
		@NotBlank String surname,
		@Pattern(regexp = "M|F|U") String sex,
		Integer birthYear,
		String birthDateText,
		String birthPlace,
		Integer deathYear,
		String deathDateText,
		boolean living) {
}
