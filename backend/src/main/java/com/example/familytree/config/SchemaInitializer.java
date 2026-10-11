package com.example.familytree.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
public class SchemaInitializer implements ApplicationRunner {

	private final Neo4jClient neo4jClient;

	public SchemaInitializer(Neo4jClient neo4jClient) {
		this.neo4jClient = neo4jClient;
	}

	@Override
	public void run(ApplicationArguments args) {
		neo4jClient.query("CREATE CONSTRAINT person_id IF NOT EXISTS FOR (p:Person) REQUIRE p.id IS UNIQUE").run();
		neo4jClient.query("CREATE INDEX person_surname IF NOT EXISTS FOR (p:Person) ON (p.surname)").run();
	}
}
