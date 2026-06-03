// Minimal Capacitor config used for build-time metadata. Kept as plain object to avoid
// requiring exact types from '@capacitor/cli' during Next.js type checks.

const config = {
  appId: 'com.astrasoft.havaman',
  appName: 'Havaman.Ai',
  webDir: 'out',
  plugins: {
    SplashScreen: {
      launchAutoHide: true,
    },
  },
}

export default config
