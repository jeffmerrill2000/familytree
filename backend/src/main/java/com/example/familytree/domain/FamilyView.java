package com.example.familytree.domain;

import java.util.List;

public record FamilyView(
		Person person,
		List<Person> parents,
		List<Person> children,
		List<Person> spouses,
		List<Person> siblings) {
}
