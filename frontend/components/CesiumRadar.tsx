'use client'
import React, { useEffect, useRef } from 'react'

type Props = { latitude?: number; longitude?: number }

export default function CesiumRadar({ latitude = 18.59, longitude = 73.74 }: Props) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)

  useEffect(() => {
    if (!canvasRef.current) return

    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Simple animated weather radar visualization
    const animate = () => {
      ctx.fillStyle = 'rgba(6, 24, 38, 0.9)'
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      // Draw radar rings
      ctx.strokeStyle = 'rgba(0, 200, 255, 0.3)'
      ctx.lineWidth = 1

      const centerX = canvas.width / 2
      const centerY = canvas.height / 2
      const maxRadius = Math.min(canvas.width, canvas.height) / 2 - 20

      for (let i = 1; i <= 4; i++) {
        const radius = (maxRadius / 4) * i
        ctx.beginPath()
        ctx.arc(centerX, centerY, radius, 0, Math.PI * 2)
        ctx.stroke()
      }

      // Draw crosshairs
      ctx.strokeStyle = 'rgba(0, 200, 255, 0.2)'
      ctx.beginPath()
      ctx.moveTo(centerX, centerY - maxRadius)
      ctx.lineTo(centerX, centerY + maxRadius)
      ctx.moveTo(centerX - maxRadius, centerY)
      ctx.lineTo(centerX + maxRadius, centerY)
      ctx.stroke()

      // Draw animated precipitation layer
      ctx.fillStyle = 'rgba(100, 150, 255, 0.3)'
      const time = (Date.now() / 1000) % 10
      const angle = (time * Math.PI * 2) / 10

      for (let i = 0; i < 12; i++) {
        const a = (i / 12) * Math.PI * 2 + angle
        const intensity = Math.sin((time + i) * 0.5) * 0.5 + 0.5
        const radius = maxRadius * 0.6 * intensity

        ctx.fillStyle = `rgba(100, 150, 255, ${intensity * 0.4})`
        ctx.beginPath()
        ctx.arc(
          centerX + Math.cos(a) * radius,
          centerY + Math.sin(a) * radius,
          10,
          0,
          Math.PI * 2
        )
        ctx.fill()
      }

      // Draw marker at current location
      ctx.fillStyle = 'rgba(0, 200, 255, 1)'
      ctx.beginPath()
      ctx.arc(centerX, centerY, 6, 0, Math.PI * 2)
      ctx.fill()

      // Also draw a pulsing glow
      ctx.strokeStyle = 'rgba(0, 200, 255, 0.5)'
      ctx.lineWidth = 2
      const glowSize = 15 + Math.sin(time * Math.PI * 2) * 5
      ctx.beginPath()
      ctx.arc(centerX, centerY, glowSize, 0, Math.PI * 2)
      ctx.stroke()

      requestAnimationFrame(animate)
    }

    animate()
  }, [])

  return (
    <canvas
      ref={canvasRef}
      width={800}
      height={600}
      style={{
        width: '100%',
        height: '100%',
        borderRadius: 12,
        display: 'block',
      }}
    />
  )
}