package com.example.familytree.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.familytree.domain.CreatePersonRequest;
import com.example.familytree.domain.FamilyView;
import com.example.familytree.domain.Person;
import com.example.familytree.service.PersonService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class PersonController {

	private final PersonService personService;

	public PersonController(PersonService personService) {
		this.personService = personService;
	}

	@PostMapping("/api/persons")
	@ResponseStatus(HttpStatus.CREATED)
	public Person create(@Valid @RequestBody CreatePersonRequest request) {
		return personService.create(request);
	}

	@GetMapping("/api/persons/{id}")
	public Person get(@PathVariable String id) {
		return personService.get(id);
	}

	@GetMapping("/api/persons/{id}/family")
	public FamilyView family(@PathVariable String id) {
		return personService.family(id);
	}

	@GetMapping("/api/persons")
	public List<Person> search(@RequestParam(name = "q", required = false) String q) {
		return personService.search(q);
	}

	@PutMapping("/api/persons/{childId}/parents/{parentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void addParent(@PathVariable String childId, @PathVariable String parentId) {
		personService.addParent(childId, parentId);
	}

	@PutMapping("/api/persons/{aId}/spouses/{bId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void addSpouse(
			@PathVariable String aId,
			@PathVariable String bId,
			@RequestParam(name = "marriageYear", required = false) Integer marriageYear) {
		personService.addSpouse(aId, bId, marriageYear);
	}
}
