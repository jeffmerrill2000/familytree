import { useState, type FormEvent } from 'react'
import './App.css'

const API = import.meta.env.VITE_API_URL as string

type Person = {
  id: string
  givenName: string
  surname: string
  sex: string
  birthYear: number | null
  birthDateText: string | null
  birthPlace: string | null
  deathYear: number | null
  deathDateText: string | null
  living: boolean
}

type FamilyView = {
  person: Person
  parents: Person[]
  children: Person[]
  spouses: Person[]
  siblings: Person[]
}

function years(person: Person) {
  const birth = person.birthYear ?? '?'
  const death = person.living ? '' : `–${person.deathYear ?? '?'}`
  return `${birth}${death}`
}

function Card({ person, onOpen }: { person: Person; onOpen: (id: string) => void }) {
  return (
    <button type="button" className="card" onClick={() => onOpen(person.id)}>
      <span className="name">{person.givenName} {person.surname}</span>
      <span className="years">{years(person)}</span>
    </button>
  )
}

function Row({ title, people, onOpen }: { title: string; people: Person[]; onOpen: (id: string) => void }) {
  return (
    <section className="row" aria-label={title}>
      <h2>{title}</h2>
      {people.length === 0 ? <p className="empty">None recorded</p> : (
        <div className="cards">
          {people.map((person) => <Card key={person.id} person={person} onOpen={onOpen} />)}
        </div>
      )}
    </section>
  )
}

export default function App() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Person[]>([])
  const [family, setFamily] = useState<FamilyView | null>(null)
  const [status, setStatus] = useState<string>('')
  const [error, setError] = useState<string>('')
  const [givenName, setGivenName] = useState('')
  const [surname, setSurname] = useState('')
  const [sex, setSex] = useState('U')
  const [birthYear, setBirthYear] = useState('')

  async function loadFamily(id: string) {
    setError('')
    setStatus('Loading family…')
    try {
      const response = await fetch(`${API}/api/persons/${id}/family`)
      if (!response.ok) {
        throw new Error(`Family request failed (${response.status})`)
      }
      setFamily(await response.json() as FamilyView)
      setStatus('')
    } catch (cause) {
      setFamily(null)
      setStatus('')
      setError(cause instanceof Error ? cause.message : 'Could not load family')
    }
  }

  async function search(event: FormEvent) {
    event.preventDefault()
    setError('')
    setStatus('Searching…')
    try {
      const response = await fetch(`${API}/api/persons?q=${encodeURIComponent(query)}`)
      if (!response.ok) {
        throw new Error(`Search failed (${response.status})`)
      }
      const people = await response.json() as Person[]
      setResults(people)
      setStatus(people.length === 0 ? 'No matches' : '')
    } catch (cause) {
      setResults([])
      setStatus('')
      setError(cause instanceof Error ? cause.message : 'Search failed')
    }
  }

  async function addPerson(event: FormEvent) {
    event.preventDefault()
    setError('')
    setStatus('Saving person…')
    try {
      const response = await fetch(`${API}/api/persons`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          givenName,
          surname,
          sex,
          birthYear: birthYear === '' ? null : Number(birthYear),
          living: true,
        }),
      })
      if (!response.ok) {
        const body = await response.json().catch(() => ({})) as { message?: string }
        throw new Error(body.message ?? `Create failed (${response.status})`)
      }
      const person = await response.json() as Person
      setGivenName('')
      setSurname('')
      setBirthYear('')
      setStatus('')
      await loadFamily(person.id)
    } catch (cause) {
      setStatus('')
      setError(cause instanceof Error ? cause.message : 'Could not add person')
    }
  }

  return (
    <main className="page">
      <header>
        <h1>Family tree</h1>
        <p>Biological parents, children, spouses, and siblings derived from shared parents.</p>
      </header>

      <form className="panel" onSubmit={search}>
        <label htmlFor="q">Search</label>
        <div className="search-line">
          <input id="q" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Given name or surname" />
          <button type="submit">Search</button>
        </div>
        {results.length > 0 && (
          <ul className="results">
            {results.map((person) => (
              <li key={person.id}>
                <button type="button" onClick={() => loadFamily(person.id)}>
                  {person.givenName} {person.surname} ({years(person)})
                </button>
              </li>
            ))}
          </ul>
        )}
      </form>

      <form className="panel" onSubmit={addPerson}>
        <h2>Add person</h2>
        <label htmlFor="given">Given name</label>
        <input id="given" required value={givenName} onChange={(event) => setGivenName(event.target.value)} />
        <label htmlFor="surname">Surname</label>
        <input id="surname" required value={surname} onChange={(event) => setSurname(event.target.value)} />
        <label htmlFor="sex">Sex</label>
        <select id="sex" value={sex} onChange={(event) => setSex(event.target.value)}>
          <option value="U">Unknown</option>
          <option value="F">Female</option>
          <option value="M">Male</option>
        </select>
        <label htmlFor="year">Birth year</label>
        <input id="year" inputMode="numeric" value={birthYear} onChange={(event) => setBirthYear(event.target.value)} />
        <button type="submit">Create</button>
      </form>

      {status && <p className="status" role="status">{status}</p>}
      {error && <p className="error" role="alert">{error}</p>}

      {!family && !status && !error && <p className="empty">Search for a person or add one to see the tree.</p>}

      {family && (
        <div className="tree">
          <Row title="Parents" people={family.parents} onOpen={loadFamily} />
          <section className="focus" aria-label="Selected person">
            <Card person={family.person} onOpen={loadFamily} />
            <Row title="Spouses" people={family.spouses} onOpen={loadFamily} />
          </section>
          <Row title="Siblings" people={family.siblings} onOpen={loadFamily} />
          <Row title="Children" people={family.children} onOpen={loadFamily} />
        </div>
      )}
    </main>
  )
}
