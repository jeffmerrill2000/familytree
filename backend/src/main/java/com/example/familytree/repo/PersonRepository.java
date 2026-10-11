package com.example.familytree.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.familytree.domain.FamilyView;
import com.example.familytree.domain.Person;

@Repository
public class PersonRepository {

	private static final String CREATE_PERSON = """
			CREATE (p:Person {id: $id, givenName: $givenName, surname: $surname, sex: $sex,
			  birthYear: $birthYear, birthDateText: $birthDateText, birthPlace: $birthPlace,
			  deathYear: $deathYear, deathDateText: $deathDateText, living: $living})
			RETURN p {.*} AS person
			""";

	private static final String LINK_PARENT = """
			MATCH (parent:Person {id: $parentId}), (child:Person {id: $childId})
			WHERE parent.id <> child.id
			  AND NOT EXISTS { MATCH (child)-[:PARENT_OF*1..40]->(parent) }
			  AND (EXISTS { MATCH (parent)-[:PARENT_OF]->(child) }
			       OR COUNT { (:Person)-[:PARENT_OF]->(child) } < 2)
			MERGE (parent)-[r:PARENT_OF]->(child)
			  ON CREATE SET r.kind = 'biological', r.createdAt = datetime()
			RETURN parent.id AS parentId, child.id AS childId
			""";

	private static final String LINK_SPOUSE = """
			MATCH (a:Person {id: $aId}), (b:Person {id: $bId})
			WHERE a.id <> b.id
			MERGE (a)-[r:SPOUSE_OF]-(b)
			  ON CREATE SET r.marriageYear = $marriageYear
			RETURN a.id AS aId, b.id AS bId
			""";

	private static final String FAMILY = """
			MATCH (p:Person {id: $id})
			CALL { WITH p MATCH (p)<-[:PARENT_OF]-(x:Person) RETURN collect(x {.*}) AS parents }
			CALL { WITH p MATCH (p)-[:PARENT_OF]->(x:Person) RETURN collect(x {.*}) AS children }
			CALL { WITH p MATCH (p)-[:SPOUSE_OF]-(x:Person) RETURN collect(x {.*}) AS spouses }
			CALL { WITH p MATCH (p)<-[:PARENT_OF]-(:Person)-[:PARENT_OF]->(x:Person)
			       WHERE x <> p RETURN collect(DISTINCT x {.*}) AS siblings }
			RETURN p {.*} AS person, parents, children, spouses, siblings
			""";

	private static final String SEARCH = """
			MATCH (p:Person)
			WHERE toLower(p.givenName + ' ' + p.surname) CONTAINS toLower($q)
			RETURN p {.*} AS person LIMIT 25
			""";

	private final Neo4jClient neo4jClient;

	public PersonRepository(Neo4jClient neo4jClient) {
		this.neo4jClient = neo4jClient;
	}

	@Transactional
	public Person create(Person person) {
		return neo4jClient.query(CREATE_PERSON)
				.bind(person.id()).to("id")
				.bind(person.givenName()).to("givenName")
				.bind(person.surname()).to("surname")
				.bind(person.sex()).to("sex")
				.bind(person.birthYear()).to("birthYear")
				.bind(person.birthDateText()).to("birthDateText")
				.bind(person.birthPlace()).to("birthPlace")
				.bind(person.deathYear()).to("deathYear")
				.bind(person.deathDateText()).to("deathDateText")
				.bind(person.living()).to("living")
				.fetch()
				.one()
				.map(row -> toPerson(asMap(row.get("person"))))
				.orElseThrow();
	}

	@Transactional
	public boolean addParent(String parentId, String childId) {
		return !neo4jClient.query(LINK_PARENT)
				.bind(parentId).to("parentId")
				.bind(childId).to("childId")
				.fetch()
				.all()
				.isEmpty();
	}

	@Transactional
	public boolean addSpouse(String aId, String bId, Integer marriageYear) {
		return !neo4jClient.query(LINK_SPOUSE)
				.bind(aId).to("aId")
				.bind(bId).to("bId")
				.bind(marriageYear).to("marriageYear")
				.fetch()
				.all()
				.isEmpty();
	}

	public Optional<FamilyView> findFamily(String id) {
		return neo4jClient.query(FAMILY)
				.bind(id).to("id")
				.fetch()
				.one()
				.map(row -> new FamilyView(
						toPerson(asMap(row.get("person"))),
						toPeople(row.get("parents")),
						toPeople(row.get("children")),
						toPeople(row.get("spouses")),
						toPeople(row.get("siblings"))));
	}

	public List<Person> searchByName(String q) {
		return neo4jClient.query(SEARCH)
				.bind(q).to("q")
				.fetch()
				.all()
				.stream()
				.map(row -> toPerson(asMap(row.get("person"))))
				.toList();
	}

	private List<Person> toPeople(Object value) {
		if (!(value instanceof List<?> list)) {
			return List.of();
		}
		List<Person> people = new ArrayList<>();
		for (Object item : list) {
			people.add(toPerson(asMap(item)));
		}
		return List.copyOf(people);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object value) {
		if (value instanceof Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		throw new IllegalStateException("Expected a map from Neo4j");
	}

	private Person toPerson(Map<String, Object> values) {
		return new Person(
				asString(values.get("id")),
				asString(values.get("givenName")),
				asString(values.get("surname")),
				asString(values.get("sex")),
				asInteger(values.get("birthYear")),
				asString(values.get("birthDateText")),
				asString(values.get("birthPlace")),
				asInteger(values.get("deathYear")),
				asString(values.get("deathDateText")),
				Boolean.TRUE.equals(values.get("living")));
	}

	private String asString(Object value) {
		return value == null ? null : value.toString();
	}

	private Integer asInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		return Integer.valueOf(value.toString());
	}
}
