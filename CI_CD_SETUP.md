# HavaMan AI - CI/CD Setup Guide

This guide explains how to configure GitHub Actions workflows for automated Android builds, signing, and distribution.

## Table of Contents
1. [GitHub Secrets Configuration](#github-secrets-configuration)
2. [Creating a Release Keystore](#creating-a-release-keystore)
3. [Firebase App Distribution Setup](#firebase-app-distribution-setup)
4. [Building and Distributing](#building-and-distributing)
5. [Workflows Overview](#workflows-overview)

---

## GitHub Secrets Configuration

Go to your GitHub repository → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**.

### Optional: Release Signing (For signed APK/AAB)

If you want to build signed releases, add these secrets:

| Secret Name | Description | Example |
|---|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded keystore file | (from `base64 -w 0 release.keystore`) |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password | mypassword123 |
| `RELEASE_KEY_ALIAS` | Key alias name | my-release-key |
| `RELEASE_KEY_PASSWORD` | Key password | keypassword123 |

### Firebase App Distribution (For easy distribution to testers)

| Secret Name | Description | Obtain From |
|---|---|---|
| `FIREBASE_APP_ID` | Firebase App ID | Firebase Console → Project Settings → Android App ID |
| `FIREBASE_APP_DISTRIBUTION_TOKEN` | Firebase CI Token | Firebase CLI: `firebase login:ci` |

---

## Creating a Release Keystore

### Step 1: Generate the Keystore File

Run this command on your local machine:

```bash
keytool -genkey -v -keystore release.keystore \
  -alias my-release-key \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -dname "CN=Your Name,OU=Your Team,O=Your Company,L=City,ST=State,C=US"
```

You'll be prompted for passwords. Remember them:
- **Keystore password** → `RELEASE_KEYSTORE_PASSWORD` secret
- **Key password** → `RELEASE_KEY_PASSWORD` secret

### Step 2: Encode to Base64

```bash
base64 -w 0 release.keystore > keystore-base64.txt
```

Copy the output and add as `RELEASE_KEYSTORE_BASE64` secret in GitHub.

### Step 3: Store Keystore Securely

**Keep the original `release.keystore` file safe and backed up!** Store it in a secure location (not in git).

---

## Firebase App Distribution Setup

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app: Package name = `com.astrasoft.havaman`

### Step 2: Generate Firebase CI Token

```bash
npm install -g firebase-tools
firebase login:ci
```

This opens a browser for authentication. After success, you'll get a token. Copy it and add as `FIREBASE_APP_DISTRIBUTION_TOKEN` secret.

### Step 3: Create Tester Group

In Firebase Console:
1. Go to **App Distribution**
2. Create a tester group (e.g., "testers")
3. Add email addresses of testers

### Step 4: Verify APK Signing

The workflow automatically signs APKs before uploading to Firebase.

---

## Building and Distributing

### Option 1: Debug Build (Automatic)

Debug builds trigger automatically on push to `main` or `develop`:

```bash
git push origin main
```

Artifact: `app-debug-apk` (available in GitHub Actions for 7 days)

### Option 2: Release Build (Manual)

1. Go to **GitHub Actions**
2. Select **Build and Release Android APK** workflow
3. Click **Run workflow**
4. Enter optional **Release notes** (e.g., "Bug fixes and UI improvements")
5. Click **Run workflow**

The workflow will:
- Build a signed release APK
- Build a signed release AAB (for Play Store)
- Upload to Firebase App Distribution (if configured)
- Store artifacts for 30 days

### Option 3: Download APK Manually

1. Go to **GitHub Actions**
2. Find the successful workflow run
3. Download artifact: `app-debug-apk` or `app-release-apk`
4. Extract ZIP and install:

```bash
adb install -r app-debug.apk
```

---

## Workflows Overview

### Debug Build Workflow (`android-build.yml`)

**Trigger:** Manual dispatch or auto on push to main/develop  
**Output:** `app-debug.apk`  
**Features:**
- Gradle caching for 2-3x faster builds
- Android SDK setup
- 7-day artifact retention

**Typical build time:** 3-4 minutes (first), 1-2 minutes (cached)

### Release Workflow (`android-release.yml`)

**Trigger:** Manual dispatch only  
**Output:** `app-release.apk`, `app-release.aab`  
**Features:**
- Signed APK and AAB builds
- Firebase App Distribution integration (optional)
- Release notes generation
- 30-day artifact retention

**Typical build time:** 4-5 minutes

---

## Troubleshooting

### Build Fails with "Keystore not found"

If no secrets are configured, the workflow falls back to building a debug APK with a temporary keystore. This is OK for testing.

### Firebase Upload Fails

Check that:
1. `FIREBASE_APP_DISTRIBUTION_TOKEN` is valid (tokens expire after ~1 hour of inactivity)
2. `FIREBASE_APP_ID` matches your Android app ID
3. Tester group "testers" exists in Firebase Console

To regenerate token:

```bash
firebase login:ci
```

### APK Installation Fails on Device

Ensure:
1. Device has "Unknown sources" enabled
2. Previous version of app is uninstalled
3. Device meets minimum API level 24 (Android 7.0)

Install with:

```bash
adb install -r app-debug.apk
```

---

## Next Steps

1. **Create GitHub Secrets** (even if just empty for now)
2. **Set up Firebase** (optional but recommended for distribution)
3. **Test a workflow run** from GitHub Actions tab
4. **Download and test APK** on your device
5. **Iterate on features** with confidence in CI/CD

---

## Security Best Practices

✅ **Do:**
- Store keystore file offline, separate from Git
- Rotate keystores periodically
- Use strong passwords (32+ characters recommended)
- Review Firebase distribution logs regularly
- Limit tester access to testers only

❌ **Don't:**
- Commit keystore file to Git
- Share keystore passwords via email/chat
- Use same password for keystore and key
- Store secrets in code or `.env` files

---

## Resources

- [Firebase App Distribution Docs](https://firebase.google.com/docs/app-distribution)
- [Android Signing Documentation](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
