'use client'

import React from 'react'
import dynamic from 'next/dynamic'
import useWeather from '../hooks/useWeather'

const CesiumRadar = dynamic(() => import('../components/CesiumRadar'), { ssr: false })

export default function Page() {
  const { currentCoordinates, weatherWisdom, fetchWisdom, setCurrentCoordinates } = useWeather()

  const onSearchSelect = (lat: number, lon: number) => {
    setCurrentCoordinates({ latitude: lat, longitude: lon })
    fetchWisdom(lat, lon)
  }

  return (
    <main className="p-6 h-screen">
      <div className="grid grid-cols-12 gap-6 h-full">
        <section className="col-span-4 flex flex-col gap-4">
          <div className="card-glass p-4 rounded-lg">
            <SearchBar onSelect={onSearchSelect} />
          </div>

          <div className="card-glass p-6 rounded-lg flex-1 flex flex-col items-center justify-center">
            <HeroPanel coords={currentCoordinates} />
          </div>

          <div className="card-glass p-4 rounded-lg">
            <WisdomGrid wisdom={weatherWisdom} />
          </div>
        </section>

        <section className="col-span-8 card-glass rounded-lg p-2 flex flex-col">
          <div className="flex-1 rounded-md overflow-hidden">
            <CesiumRadar latitude={currentCoordinates?.latitude} longitude={currentCoordinates?.longitude} />
          </div>
          <div className="mt-3">
            <TimelineControls />
          </div>
        </section>
      </div>
    </main>
  )
}

function SearchBar({ onSelect }: { onSelect: (lat: number, lon: number) => void }) {
  const [q, setQ] = React.useState('')
  const [results, setResults] = React.useState<any[]>([])

  async function doSearch() {
    try {
      const r = await fetch(`/api/location/search?q=${encodeURIComponent(q)}`)
      const data = await r.json()
      setResults(data)
    } catch (e) {
      console.error(e)
    }
  }

  return (
    <div>
      <div className="flex gap-2">
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search city" className="flex-1 p-2 rounded-md bg-transparent border border-white/6" />
        <button onClick={doSearch} className="px-4 py-2 bg-gradient-to-r from-cyan-400 to-blue-500 rounded-md">Search</button>
      </div>
      <div className="mt-2 max-h-40 overflow-auto">
        {results.map((r: any, idx: number) => (
          <div key={idx} onClick={() => onSelect(r.latitude, r.longitude)} className="p-2 hover:bg-white/2 rounded-md cursor-pointer">{r.name} {r.country ? `· ${r.country}` : ''}</div>
        ))}
      </div>
    </div>
  )
}

function HeroPanel({ coords }: { coords?: { latitude: number; longitude: number } | null }) {
  const [temp, setTemp] = React.useState<number | null>(null)

  React.useEffect(() => {
    if (!coords) return
    fetch(`/api/weather/wisdom?lat=${coords.latitude}&lon=${coords.longitude}`).then((r) => r.json()).then((d) => setTemp(d.meta?.temp ?? null))
  }, [coords])

  return (
    <div className="w-full h-48 flex items-center justify-center flex-col">
      <div className="text-6xl font-semibold">{temp !== null ? `${Math.round(temp)}°C` : '—'}</div>
      <div className="text-sm text-slate-300">Humidity: — · Real-time SVG animation</div>
    </div>
  )
}

function WisdomGrid({ wisdom }: { wisdom: any }) {
  const cards = [
    { key: 'health', title: 'Health Card', text: wisdom?.health_card || 'No data', glow: 'neon-red' },
    { key: 'travel', title: 'Travel Card', text: wisdom?.travel_card || 'No data', glow: 'neon-yellow' },
    { key: 'clothing', title: 'Clothing Card', text: wisdom?.clothing_card || 'No data', glow: 'neon-cyan' },
  ]

  return (
    <div className="grid grid-cols-3 gap-3">
      {cards.map((c) => (
        <div key={c.key} className={`card-glass p-4 rounded-lg ${c.glow}`}>
          <h3 className="font-semibold mb-2">{c.title}</h3>
          <p className="text-sm text-slate-200">{c.text}</p>
        </div>
      ))}
    </div>
  )
}

function TimelineControls() {
  return (
    <div className="flex items-center justify-between gap-3">
      <div className="flex items-center gap-2">
        <button className="p-2 rounded bg-white/5">Play</button>
        <button className="p-2 rounded bg-white/5">Pause</button>
      </div>
      <div className="flex-1">
        <input type="range" min={0} max={120} className="w-full" />
      </div>
      <div>
        <button className="p-2 rounded bg-white/5">Expand</button>
      </div>
    </div>
  )
}