# Christmas Talk 2025

An interactive presentation app built with Squint (ClojureScript) and Bun. Features real-time audience participation via Ably.

## Project Structure

```
├── src/
│   ├── talk/              # ClojureScript source files
│   │   ├── app.cljs       # App entry point and routing
│   │   ├── presenter.cljs # Presenter controls and slide definitions
│   │   ├── display.cljs   # Display/projector view with slide rendering
│   │   ├── audience.cljs  # Audience participation interface
│   │   ├── ably.cljs      # Ably real-time communication
│   │   ├── ui.cljs        # Shared UI components
│   │   └── entry.css      # Tailwind CSS entry point
│   └── index.html         # HTML entry point
├── build/                 # Squint compilation output (gitignored)
├── target/public/         # Final bundled output (gitignored)
├── .github/workflows/     # CI/CD configuration
│   └── build.yml          # Build and deploy workflow
├── bb.edn                 # Babashka task definitions
├── squint.edn             # Squint compiler configuration
├── build.js               # Bun build script
└── package.json           # Node dependencies
```

## Requirements

- [Bun](https://bun.sh) - JavaScript runtime and bundler
- [Babashka](https://babashka.org) - Task runner for ClojureScript tooling

## Setup

Install dependencies:

```bash
bun install
```

## Development

Start the development server with hot-reload:

```bash
bb dev
```

This runs in parallel:
- Squint compilation (watches `src/talk/`)
- Bun bundling (watches `build/`)
- Local server at https://localhost:3000

## Build

Build for production:

```bash
bb build
```

Output goes to `target/public/` containing:
- `index.html` - Entry point
- `app.js` - Bundled JavaScript
- `app.css` - Compiled Tailwind CSS
- Static assets (images)

Clean build artifacts:

```bash
bb clean
```

## Deployment

This project uses a two-stage deployment:

### 1. Build Stage (this repo)

On push to `main`, GitHub Actions:
1. Installs Bun and Babashka
2. Runs `bb build`
3. Pushes contents of `target/public/` to the `dist` branch

The `dist` branch contains only the built static assets, no source code.

### 2. Deploy Stage (blog repo)

The [blog repo](https://github.com/Akeboshiwind/akeboshiwind.github.io) includes this repo as a git submodule pointing to the `dist` branch:

```
talks/christmas-talk -> christmas-talk-2025@dist
```

When the blog builds, it copies the submodule contents to `static/talk/christmas/`, making the presentation available at:

**https://blog.bythe.rocks/talk/christmas/**

### Updating the Deployed Version

After pushing changes to `main`:

1. Wait for the build workflow to complete (updates `dist` branch)
2. In the blog repo, update the submodule:
   ```bash
   git submodule update --remote talks/christmas-talk
   git commit -am "chore: update christmas talk"
   git push
   ```
