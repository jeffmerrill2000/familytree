package com.example.familytree.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.familytree.domain.CreatePersonRequest;
import com.example.familytree.domain.FamilyView;
import com.example.familytree.domain.Person;
import com.example.familytree.repo.PersonRepository;

@Service
public class PersonService {

	static final String LINK_NOT_ALLOWED = "Link not allowed (self, cycle, or child already has two parents)";

	private final PersonRepository personRepository;

	public PersonService(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	public Person create(CreatePersonRequest request) {
		Person person = new Person(
				UUID.randomUUID().toString(),
				request.givenName(),
				request.surname(),
				request.sex(),
				request.birthYear(),
				request.birthDateText(),
				request.birthPlace(),
				request.deathYear(),
				request.deathDateText(),
				request.living());
		return personRepository.create(person);
	}

	public Person get(String id) {
		return family(id).person();
	}

	public FamilyView family(String id) {
		return personRepository.findFamily(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	public List<Person> search(String q) {
		return personRepository.searchByName(q == null ? "" : q);
	}

	public void addParent(String childId, String parentId) {
		requirePerson(parentId);
		requirePerson(childId);
		if (!personRepository.addParent(parentId, childId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, LINK_NOT_ALLOWED);
		}
	}

	public void addSpouse(String aId, String bId, Integer marriageYear) {
		requirePerson(aId);
		requirePerson(bId);
		if (!personRepository.addSpouse(aId, bId, marriageYear)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	private void requirePerson(String id) {
		if (personRepository.findFamily(id).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}
}
