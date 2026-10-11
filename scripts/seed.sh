#!/usr/bin/env bash
set -euo pipefail

API="${API:-http://localhost:8080}"

create_person() {
	local given="$1" surname="$2" sex="$3" year="$4"
	curl -fsS -X POST "$API/api/persons" \
		-H 'Content-Type: application/json' \
		-d "{\"givenName\":\"$given\",\"surname\":\"$surname\",\"sex\":\"$sex\",\"birthYear\":$year,\"living\":true}"
}

link_parent() {
	local child_id="$1" parent_id="$2"
	curl -fsS -X PUT "$API/api/persons/${child_id}/parents/${parent_id}"
}

gpf1=$(create_person George Hale M 1940 | jq -r .id)
gpm1=$(create_person Ruth Hale F 1942 | jq -r .id)
gpf2=$(create_person James Cole M 1938 | jq -r .id)
gpm2=$(create_person Helen Cole F 1941 | jq -r .id)
parent_a=$(create_person Martin Hale M 1970 | jq -r .id)
parent_b=$(create_person Claire Cole F 1972 | jq -r .id)
other=$(create_person Nora Pike F 1975 | jq -r .id)
child1=$(create_person Evan Hale M 2000 | jq -r .id)
child2=$(create_person Maya Hale F 2002 | jq -r .id)
child3=$(create_person Leo Hale M 2004 | jq -r .id)
youngest=$(create_person Iris Hale F 2008 | jq -r .id)

link_parent "$parent_a" "$gpf1"
link_parent "$parent_a" "$gpm1"
link_parent "$parent_b" "$gpf2"
link_parent "$parent_b" "$gpm2"
link_parent "$child1" "$parent_a"
link_parent "$child1" "$parent_b"
link_parent "$child2" "$parent_a"
link_parent "$child2" "$parent_b"
link_parent "$child3" "$parent_a"
link_parent "$child3" "$parent_b"
link_parent "$youngest" "$parent_a"
link_parent "$youngest" "$other"

curl -fsS -X PUT "$API/api/persons/${parent_a}/spouses/${parent_b}?marriageYear=1998" >/dev/null

echo "$youngest"
