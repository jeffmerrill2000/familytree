package com.example.familytree.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.familytree.domain.FamilyView;
import com.example.familytree.domain.Person;

@Testcontainers
@SpringBootTest
class PersonRepositoryTest {

	@Container
	static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26-community"))
			.withAdminPassword("test-password");

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", NEO4J::getBoltUrl);
		registry.add("spring.neo4j.authentication.username", () -> "neo4j");
		registry.add("spring.neo4j.authentication.password", NEO4J::getAdminPassword);
	}

	@Autowired
	private PersonRepository personRepository;

	@Test
	void childHasTwoParents() {
		Person mother = create("Ada", "Lovelace");
		Person father = create("Charles", "Babbage");
		Person child = create("Ann", "Babbage");

		assertTrue(personRepository.addParent(mother.id(), child.id()));
		assertTrue(personRepository.addParent(father.id(), child.id()));

		FamilyView family = personRepository.findFamily(child.id()).orElseThrow();
		assertEquals(2, family.parents().size());
	}

	@Test
	void childrenOfTheSameMotherAreSiblings() {
		Person mother = create("Mary", "Shelley");
		Person first = create("William", "Shelley");
		Person second = create("Clara", "Shelley");
		personRepository.addParent(mother.id(), first.id());
		personRepository.addParent(mother.id(), second.id());

		FamilyView family = personRepository.findFamily(first.id()).orElseThrow();
		assertEquals(1, family.siblings().size());
		assertEquals(second.id(), family.siblings().get(0).id());
	}

	@Test
	void thirdParentIsRejected() {
		Person child = create("Only", "Child");
		assertTrue(personRepository.addParent(create("One", "Parent").id(), child.id()));
		assertTrue(personRepository.addParent(create("Two", "Parent").id(), child.id()));
		assertFalse(personRepository.addParent(create("Three", "Parent").id(), child.id()));
	}

	@Test
	void selfParentIsRejected() {
		Person person = create("Self", "Parent");
		assertFalse(personRepository.addParent(person.id(), person.id()));
	}

	@Test
	void ancestryCycleIsRejected() {
		Person a = create("Gen", "A");
		Person b = create("Gen", "B");
		Person c = create("Gen", "C");
		assertTrue(personRepository.addParent(a.id(), b.id()));
		assertTrue(personRepository.addParent(b.id(), c.id()));
		assertFalse(personRepository.addParent(c.id(), a.id()));
	}

	@Test
	void linkingTheSameParentTwiceDoesNotDuplicate() {
		Person parent = create("Same", "Parent");
		Person child = create("Same", "Child");
		assertTrue(personRepository.addParent(parent.id(), child.id()));
		assertTrue(personRepository.addParent(parent.id(), child.id()));
		FamilyView family = personRepository.findFamily(parent.id()).orElseThrow();
		assertEquals(1, family.children().size());
	}

	private Person create(String givenName, String surname) {
		return personRepository.create(new Person(
				java.util.UUID.randomUUID().toString(),
				givenName,
				surname,
				"U",
				null,
				null,
				null,
				null,
				null,
				true));
	}
}
