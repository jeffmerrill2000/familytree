package com.example.familytree.domain;

public record Person(
		String id,
		String givenName,
		String surname,
		String sex,
		Integer birthYear,
		String birthDateText,
		String birthPlace,
		Integer deathYear,
		String deathDateText,
		boolean living) {
}
